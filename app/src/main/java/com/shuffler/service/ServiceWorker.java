package com.shuffler.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.shuffler.activity.MainActivity;
import com.shuffler.R;
import com.shuffler.handler.PlayerStateUpdateHandler;
import com.shuffler.handler.QueueRequestHandler;
import com.shuffler.handler.RequestHandler;
import com.shuffler.handler.VolleyErrorHandler;
import com.shuffler.spotify.listener.PlayerStateCallback;
import com.shuffler.spotify.listener.PlayerStateListener;
import com.shuffler.spotify.listener.QueueErrorCallback;
import com.shuffler.utility.BooleanLock;
import com.shuffler.utility.LookupList;
import com.shuffler.volley.listener.ErrorListener;
import com.shuffler.volley.listener.PlaylistsResponseListener;
import com.shuffler.volley.listener.TrackResponseListener;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.Empty;
import com.spotify.protocol.types.PlayerState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ServiceWorker extends Thread implements RequestHandler, PlayerStateUpdateHandler, QueueRequestHandler, VolleyErrorHandler {

    private EnqueueingService service;
    private RequestQueue queue;
    private String authToken;
    private SpotifyAppRemote mSpotifyAppRemote;
    private PlayerApi player;
    private PlayerState playerState = null;
    private List<String> playlists;
    private static LookupList<String> tracks;
    private Subscription<PlayerState> subscription;
    private Integer notificationID = null;
    private static Integer DEFAULT_SPOTIFY_QUEUE_LENGTH;
    private final static List<String> spotifyQueue = new ArrayList<>();
    private final BooleanLock startingPlayback = new BooleanLock();
    private final BooleanLock resumingServiceWork = new BooleanLock();
    private final Set<String> pendingPlaylistRequests = new HashSet<>();
    private final Set<Pair<String, String>> pendingTrackRequests = new HashSet<>();

    public ServiceWorker(EnqueueingService service, String authToken) {
        queue = Volley.newRequestQueue(service.getApplicationContext());
        playlists = new ArrayList<>();
        tracks = new LookupList<>();
        this.authToken = authToken;
        this.service = service;
        DEFAULT_SPOTIFY_QUEUE_LENGTH = Integer.parseInt(service.getResources().getString(R.string.default_spotify_queue_length));
        setName("ServiceWorker");
    }

    @Override
    public void run() {
        mSpotifyAppRemote = MainActivity.getAppRemote();
        player = mSpotifyAppRemote.getPlayerApi();
        player.getPlayerState().setResultCallback(new PlayerStateCallback(this));
        subscription = player.subscribeToPlayerState().setEventCallback(new PlayerStateListener(this));
        forwardPlaylistsRequest(service.getResources().getString(R.string.playlists_request_url));
    }

    @Override
    public void interrupt() {
        subscription.cancel();
        queue.stop();
        queue.cancelAll(this);
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
        super.interrupt();
    }

    public void managePlaylistResponse(JSONObject response){
        try {
            JSONArray JSONPlaylistArray = response.getJSONArray("items");
            String next = response.getString("next");
            if(!next.equals("null"))
                forwardPlaylistsRequest(next);
            String[] playlistArray = new String[JSONPlaylistArray.length()];

            for(int i = 0; i < playlistArray.length; i++){
                playlistArray[i] = JSONPlaylistArray.getJSONObject(i).getString("id");
            }

            playlists.addAll(Arrays.asList(playlistArray));
            shuffle(playlists);
            synchronized(pendingPlaylistRequests) {
                pendingPlaylistRequests.remove(response.getString("offset"));
            }

            if(!playlists.isEmpty()) {
                // If the first set of playlist has been received, ask directly for the tracks inside it, to be able to
                // start the playback of the first song while waiting for the track retrieval to wait.
                String playlistID = playlists.remove(0);
                StringBuilder sb = new StringBuilder(service.getResources().getString(R.string.playlist_tracks_request_url))
                        .append(playlistID)
                        .append("/tracks");
                String url = sb.toString();
                if(response.getInt("offset") == 0)
                    forwardTracksRequest(url);
            }
            else
                Toast.makeText(service, R.string.no_playlists_found, Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(service, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void manageTrackResponse(JSONObject response){
        try{
            JSONArray JSONTrackArray = response.getJSONArray("items");
            String next = response.getString("next");
            if(!next.equals("null"))
                forwardTracksRequest(next);
            String[] trackArray = new String[JSONTrackArray.length()];

            for(int i = 0; i < trackArray.length; i++){
                JSONObject trackObject = JSONTrackArray.getJSONObject(i).getJSONObject("track");
                trackArray[i] = trackObject.getString("uri");
                // Put in the map pair <author, title> for each track
                EnqueueingService.tracksInfo.put(trackArray[i], new Pair<String, String>(trackObject.getJSONArray("artists").getJSONObject(0).getString("name"), trackObject.getString("name")));
            }

            tracks.addAll(Arrays.asList(trackArray));
            synchronized (playerState) {
                synchronized (startingPlayback) {
                    if ((playerState.track == null || playerState.isPaused) && !startingPlayback.getValue()) {
                        startingPlayback.setValue(true);
                        player.play(trackArray[0]);
                        tracks.remove(trackArray[0]);
                    }
                }
            }

            String playlistID = response.getString("href");
            playlistID = playlistID.split("playlists/")[1];
            playlistID = playlistID.substring(0, playlistID.indexOf("/tracks"));
            String offset = response.getString("offset");
            Pair<String, String> requestIdentifier = new Pair<>(playlistID, offset);
            synchronized (pendingTrackRequests) {
                pendingTrackRequests.remove(requestIdentifier);
            }

            //TODO: when the song is playing is finished, add saved tracks in queue and run playback, then continue managing requests

            // if there are still playlistIDs left in the list
            if(!playlists.isEmpty()) {
                StringBuilder sb = new StringBuilder(service.getResources().getString(R.string.playlist_tracks_request_url))
                        .append(playlists.remove(0))
                        .append("/tracks");
                forwardTracksRequest(sb.toString());
            }
            else {
                //if all requests have been sent, check whether there is any pending request
                synchronized(pendingPlaylistRequests) {
                    synchronized(pendingTrackRequests) {
                        if (pendingPlaylistRequests.isEmpty() && pendingTrackRequests.isEmpty()) {
                            // In case some tracks have already been enqueued (an error occurred), avoid enqueueing it again
                            tracks.removeAll(spotifyQueue);
                            shuffle(tracks);

                            StringBuilder sb = new StringBuilder("Done! Enjoy your ")
                                    .append(tracks.size() + spotifyQueue.size() + 1)
                                    .append(" tracks");
                            synchronized (spotifyQueue) {
                                enqueue();
                            }
                            Toast.makeText(service, sb.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Toast.makeText(service, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void resumeServiceWork(int notificationID){
        if(this.notificationID == notificationID){
            this.notificationID = null;
            resumeServiceWork();
        }
    }

    private void resumeServiceWork(){
        // check if there are pending requests to be forwarded, first
        StringBuilder sb;
        synchronized(pendingPlaylistRequests) {
            if (!pendingPlaylistRequests.isEmpty()) {
                String baseUrl = service.getResources().getString(R.string.playlists_request_url) + "&offset=";
                for (String offset : pendingPlaylistRequests) {
                    sb = new StringBuilder(baseUrl)
                            .append(offset);
                    forwardPlaylistsRequest(sb.toString());
                }
            }
        }

        // NOTE: the first call to forwardTracksRequest will start to call itself recursively to request all the tracks, therefore, even if there are playlistIDs left in the list, these
        // will be managed automatically, without starting calling forwardTracksRequest on the first element of the list
        synchronized (pendingTrackRequests) {
            if (!pendingTrackRequests.isEmpty()) {
                String baseUrl = service.getResources().getString(R.string.playlist_tracks_request_url);
                for (Pair<String, String> identifier : pendingTrackRequests) {
                    String playlistID = identifier.first;
                    String offset = identifier.second;
                    sb = new StringBuilder(baseUrl)
                            .append(playlistID)
                            .append("/tracks?offset=")
                            .append(offset);
                    String url = sb.toString();
                    forwardTracksRequest(url);
                }
            }
            // if there are no pending requests, we must check if there are playlistIDs left in the list. If so, it is sufficient to call forwardTracksRequest on the first element of the list
            else {
                if (!playlists.isEmpty()) {
                    sb = new StringBuilder(service.getResources().getString(R.string.playlist_tracks_request_url))
                            .append(playlists.remove(0))
                            .append("/tracks");
                    forwardTracksRequest(sb.toString());
                }
            }
        }
        synchronized (resumingServiceWork){
            resumingServiceWork.toggle();
        }
    }

    public void manageWebRequestError(VolleyError error){

        queue.cancelAll(this);
        if(error instanceof NoConnectionError)
            handle((NoConnectionError) error);
        else {
            int statusCode = error.networkResponse.statusCode;
            if (statusCode == 429) {
                // Verify if by canceling all requests in the Volley queue, it is possible to avoid to use the BooleanLock resumingServiceWork
                synchronized (resumingServiceWork) {
                    if (!resumingServiceWork.getValue()) {
                        resumingServiceWork.toggle();
                        Integer retryAfter = Integer.parseInt(error.networkResponse.headers.get("Retry-After"));

                        Executors.newSingleThreadScheduledExecutor().schedule(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        resumeServiceWork();
                                    }
                                },
                                retryAfter,
                                TimeUnit.SECONDS
                        );
                    }
                }
            } else {
                StringBuilder sb = new StringBuilder(service.getResources().getString(R.string.web_api_error_response))
                        .append("\n")
                        .append("Error: ")
                        .append(statusCode)
                        .append("\n")
                        .append(new String(error.networkResponse.data));
                Toast.makeText(service, sb.toString(), Toast.LENGTH_SHORT).show();
            }
        }
        if(!tracks.isEmpty()) {
            shuffle(tracks);
            synchronized (spotifyQueue) {
                enqueue();
            }
        }
    }

    @Override
    public void handle(NoConnectionError e) {
        if(notificationID == null) {
            Notification notification = new NotificationCompat.Builder(service, NotificationChannel.DEFAULT_CHANNEL_ID)
                    .setContentTitle("No internet connection available")
                    .setContentText("Shuffler is waiting for an Internet connection")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .build();
            NotificationManagerCompat manager = NotificationManagerCompat.from(service);
            notificationID = ThreadLocalRandom.current().nextInt();
            manager.notify(notificationID, notification);
            service.createReceiver(notificationID);
        }
    }

    // TODO: evaluate possibility to use the same ResponseListener for every request, instead of always creating a new one. Probably this approach needs to keep ResponseListener's methods synchronized
    private void forwardPlaylistsRequest(String url){
        Response.Listener<JSONObject> responseListener = new PlaylistsResponseListener(this);
        Response.ErrorListener errorListener = new ErrorListener(this);


        JsonObjectRequest playlistRequest = new JsonObjectRequest(Request.Method.GET, url, null, responseListener, errorListener){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String auth = "Bearer ";
                auth += authToken;
                headers.put("Authorization", auth);
                return headers;
            }
        };
        playlistRequest.setTag(this);
        int offsetIndex = url.indexOf("offset=");
        synchronized (pendingPlaylistRequests) {
            if (offsetIndex == -1)
                pendingPlaylistRequests.add("0");
            else {
                String offsetSubString = url.substring(offsetIndex).split("=")[1];
                String offset = offsetSubString.contains("&") ? offsetSubString.substring(0, offsetSubString.indexOf("&")) : offsetSubString;
                pendingPlaylistRequests.add(offset);
            }
        }
        queue.add(playlistRequest);
    }

    private void forwardTracksRequest(String url){

        Response.Listener<JSONObject> responseListener = new TrackResponseListener(this);
        Response.ErrorListener errorListener = new ErrorListener(this);


        JsonObjectRequest trackRequest = new JsonObjectRequest(Request.Method.GET, url, null, responseListener, errorListener){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String auth = "Bearer ";
                auth += authToken;
                headers.put("Authorization", auth);
                return headers;
            }
        };
        trackRequest.setTag(this);

        int offsetIndex = url.indexOf("offset=");
        String offset;
        if(offsetIndex == -1)
            offset = "0";
        else{
            String offsetSubString = url.substring(offsetIndex);
            offsetSubString = offsetSubString.split("=")[1];
            offset = offsetSubString.contains("&")  ?  offsetSubString.substring(0, offsetSubString.indexOf("&")) : offsetSubString;
        }
        // Add a couple <playlistID, offset> to the list of pending tracks
        String playlistID = url.split("playlists/")[1];
        Pair<String, String> requestIdentifier = new Pair<>(playlistID.substring(0, playlistID.indexOf("/tracks")), offset);
        synchronized (pendingTrackRequests) {
            pendingTrackRequests.add(requestIdentifier);
        }
        queue.add(trackRequest);
    }

    public void enqueue(){
        synchronized (spotifyQueue) {
            while (!tracks.isEmpty() && spotifyQueue.size() < DEFAULT_SPOTIFY_QUEUE_LENGTH) {
                String track = tracks.pop();
                //spotifyQueue.add(track);
                int currentLength = spotifyQueue.size();


                try {
                    // Unfortunately, the only thing it can be done is assume that, if after 100 milliseconds an error is not received, the track has been successfully enqueued
                    // Tried to set a ResultCallback on the player.queue CallResult, but it seems it never receives a result, therefore the callback is never invoked

                    CallResult<Empty> callResult = null;
                    while(spotifyQueue.size() == currentLength) {
                        if(callResult != null)
                            callResult.cancel();
                        callResult = (CallResult<Empty>) player.queue(track).setErrorCallback(new QueueErrorCallback(this, track));
                        spotifyQueue.add(track);
                        spotifyQueue.wait(100);
                    }
                } catch (InterruptedException e) {
                    Log.e(this.getClass().getName(), "Call to wait() in method ServiceWorker.enqueue() threw an InterruptedException");
                }

            }
        }
        synchronized (pendingPlaylistRequests){
            synchronized (pendingTrackRequests){
                if(tracks.isEmpty() && pendingTrackRequests.isEmpty() && pendingPlaylistRequests.isEmpty()){
                    service.stopForeground(true);
                    service.stopSelf();
                }
            }
        }
    }

    @Override
    public void failedQueueRequest(String track) {
        synchronized(spotifyQueue){
            spotifyQueue.remove(track);
            spotifyQueue.notifyAll();
        }
    }


    public void updatePlayerState(PlayerState playerState){
        synchronized (playerState) {
            synchronized (startingPlayback) {
                if(this.playerState != null && !this.playerState.track.uri.equals(playerState.track.uri)){
                    // if the song launched in playback is not the first track retrieved
                    if(!startingPlayback.getValue()) {
                        synchronized (spotifyQueue) {
                            updateSpotifyQueue(playerState.track.uri);
                            if (spotifyQueue.size() <= 2)
                                enqueue();
                        }
                    }
                }
                this.playerState = playerState;

                startingPlayback.setValue(false);
            }
        }
    }

    private void updateSpotifyQueue(String launchedTrack){
        String trackUri;

        synchronized (spotifyQueue) {
            if(spotifyQueue.contains(launchedTrack)) {
                do {
                    trackUri = spotifyQueue.remove(0);
                } while (!trackUri.equals(launchedTrack) && !spotifyQueue.isEmpty());
            }
        }
    }

    private void shuffle(List<?> list){
        for (int i = 0; i < 3; i++){
            Collections.shuffle(list, ThreadLocalRandom.current());
            Collections.reverse(list);
        }
        Collections.reverse(list);
    }

    public static List<String> getSpotifyQueue(){
        return spotifyQueue;
    }

    public static List<String> getTrackList(){
        return tracks;
    }
}
