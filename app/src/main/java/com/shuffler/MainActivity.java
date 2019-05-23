package com.shuffler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.shuffler.broadcast.receiver.NetworkStateChangeListener;
import com.shuffler.handler.RequestHandler;
import com.shuffler.spotify.listener.ConnectionListener;
import com.shuffler.spotify.listener.PlayerStateCallback;
import com.shuffler.spotify.listener.PlayerStateListener;
import com.shuffler.utility.BooleanLock;
import com.shuffler.utility.LookupList;
import com.shuffler.volley.listener.ErrorListener;
import com.shuffler.volley.listener.PlaylistsResponseListener;
import com.shuffler.volley.listener.TrackResponseListener;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.types.PlayerState;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

// TODO: strong refactory to re-optimize and re-organize code
// TODO: add splash screen
// TODO: do main logic in a background task instead that on the main activity
// TODO: also avoid that the background task is closed when the main activity is closed by either the user or the system
// TODO: add memory and disk caches to avoid loading playlists and tracks from the web api each time the App is opened
public class MainActivity extends AppCompatActivity implements RequestHandler {
    private String CLIENT_ID;
    private String REDIRECT_CALLBACK;
    private int DEFAULT_SPOTIFY_QUEUE_LENGTH;
    private SpotifyAppRemote mSpotifyAppRemote;
    private ConnectionListener connectionListener;
    private ConnectionParams params;
    private TextView mainMessage;
    private RequestQueue queue;
    private Integer requestCode;
    private String authToken;
    private PlayerApi player;
    private PlayerState playerState = null;
    private List<String> playlists;
    private List<String> tracks;
    private final List<String> spotifyQueue = new ArrayList<>();
    private final BooleanLock startingPlayback = new BooleanLock();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CLIENT_ID = getResources().getString(R.string.client_id);
        REDIRECT_CALLBACK = getResources().getString(R.string.redirect_callback);
        DEFAULT_SPOTIFY_QUEUE_LENGTH = Integer.parseInt(getResources().getString(R.string.default_spotify_queue_length));
        mainMessage = (TextView) findViewById(R.id.main_message);
        queue = Volley.newRequestQueue(this.getApplicationContext());
        connectionListener = new ConnectionListener(this);
        params = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_CALLBACK)
                .showAuthView(true)
                .build();
        playlists = new ArrayList<>();
        tracks = new LookupList<>();

        BroadcastReceiver br = new NetworkStateChangeListener();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(br, filter);
    }

    public void connect(){
        mainMessage.setText(R.string.main_activity_bootstrap);
        if(checkConnection()) {
            SpotifyAppRemote.connect(this, params, connectionListener);
        }
        else{
            mainMessage.setText(R.string.no_connection_available);
        }
    }

    private boolean checkConnection(){
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return (info != null && info.isConnected());
    }

    public void setSpotifyAppRemote(SpotifyAppRemote mSpotifyAppRemote){
        this.mSpotifyAppRemote = mSpotifyAppRemote;
        this.player = mSpotifyAppRemote.getPlayerApi();
        player.getPlayerState().setResultCallback(new PlayerStateCallback(this));
        player.subscribeToPlayerState().setEventCallback(new PlayerStateListener(this));
    }

    public void connected(){
        mainMessage.setText(R.string.connected);
        getSpotifyAuthToken();
    }

    private void getSpotifyAuthToken(){
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_CALLBACK)
                .setScopes(new String[]{"playlist-read-private", "playlist-read-collaborative"});
        AuthenticationRequest request = builder.build();
        requestCode = Integer.parseInt(getResources().getString(R.string.auth_request_code));
        AuthenticationClient.openLoginActivity(this, requestCode, request);
    }

    public void connectionFailed(Throwable throwable){
        StringBuilder sb = new StringBuilder(getResources().getString(R.string.connection_failed));
        sb.append('\n');
        sb.append("Error message: ");
        sb.append(throwable.getMessage());

        mainMessage.setText(sb.toString());
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
            Collections.shuffle(playlists, ThreadLocalRandom.current());

            if(!playlists.isEmpty()) {
                // If the first set of playlist has been received, ask directly for the tracks inside it, to be able to
                // start the playback of the first song while waiting for the track retrieval to wait.
                if(response.getInt("offset") == 0)
                    forwardTracksRequest(playlists.remove(0));
            }
            else
                mainMessage.setText(R.string.no_playlists_found);
        } catch (JSONException e) {
            mainMessage.setText(e.getMessage());
        }
    }

    public void manageTrackResponse(JSONObject response){
        try{
            JSONArray JSONTrackArray = response.getJSONArray("items");
            String[] trackArray = new String[JSONTrackArray.length()];

            for(int i = 0; i < trackArray.length; i++){
                trackArray[i] = JSONTrackArray.getJSONObject(i).getJSONObject("track").getString("uri");
            }

            tracks.addAll(Arrays.asList(trackArray));
            synchronized (playerState) {
                synchronized (startingPlayback) {
                    if (playerState.isPaused && !startingPlayback.getValue()) {
                        startingPlayback.setValue(true);
                        player.play(trackArray[0]);
                        tracks.remove(trackArray[0]);

                    }
                }
            }

            //TODO: check whether there are pending requests before concluding
            //TODO: when the song is playing is finished, add saved tracks in queue and run playback, then continue managing requests
            if(!playlists.isEmpty())
                forwardTracksRequest(playlists.remove(0));
            else {
                Collections.shuffle(tracks, ThreadLocalRandom.current());

                StringBuilder sb = new StringBuilder("Done! Enjoy your ")
                        .append(tracks.size() + 1)
                        .append(" tracks");
                synchronized (spotifyQueue) {
                    enqueue();
                }
                mainMessage.setText(sb.toString());
            }
        } catch (JSONException e) {
            mainMessage.setText(e.getMessage());
        }
    }

    public void manageWebRequestError(VolleyError error){
        // TODO: manage the case where the error is due to the too many requests done to the Spotify Web API.
        // If that's the case, the user should be able to start listening to the already requested playlists and tracks and
        // a thread should be started that, after the amount of time the client must wait to be able to make requests again, contained in the response,
        // should go on consuming the playlist list and filling the tracks list by simply forwarding the request for the first playlist in the list (then requests are sent by "recursively"
        // forwarding new requests.
        StringBuilder sb = new StringBuilder(getResources().getString(R.string.web_api_error_response))
                .append("\n")
                .append("Error: ")
                .append(error.networkResponse.statusCode)
                .append("\n")
                .append(new String(error.networkResponse.data));
        mainMessage.setText(sb.toString());
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

        queue.add(playlistRequest);
    }

    private void forwardTracksRequest(String playlistID){
        StringBuilder sb = new StringBuilder(getResources().getString(R.string.playlist_tracks_request_url))
                .append(playlistID)
                .append("/tracks");
        String url = sb.toString();
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

        queue.add(trackRequest);
    }

    private void enqueue(){
        synchronized (spotifyQueue) {
            while (!tracks.isEmpty() && spotifyQueue.size() < DEFAULT_SPOTIFY_QUEUE_LENGTH) {
                String track = tracks.remove(0);
                spotifyQueue.add(track);
                player.queue(track);
            }
        }
    }

    public void updatePlayerState(PlayerState playerState){
        synchronized (playerState) {
            synchronized (startingPlayback) {
                if(this.playerState != null && !this.playerState.track.uri.equals(playerState.track.uri)){
                    if(!startingPlayback.getValue()) {
                        updateSpotifyQueue(playerState.track.uri);
                        synchronized (spotifyQueue) {
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

        do{
            trackUri = spotifyQueue.remove(0);
        } while (!trackUri.equals(launchedTrack) && !spotifyQueue.isEmpty());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == this.requestCode) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            StringBuilder sb;

            switch (response.getType()) {
                case TOKEN:
                    authToken = response.getAccessToken();
                    mainMessage.setText(R.string.wait_for_track_loading);
                    forwardPlaylistsRequest(getResources().getString(R.string.playlists_request_url));
                    break;

                case ERROR:
                    sb = new StringBuilder(getResources().getString(R.string.auth_error_response))
                            .append('\n')
                            .append("Error: \n")
                            .append(response.getError());
                    mainMessage.setText(sb.toString());
                    break;

                default:
                    sb = new StringBuilder(getResources().getString(R.string.default_response_type_switch))
                            .append('\n')
                            .append("HTTP response code: \n")
                            .append(response.getCode());
                    mainMessage.setText(sb.toString());

            }
        }

    }

}
