# Shuffler
*Utility Android app thought to shuffle tracks among all user's playlists*

## Introduction
This app has been thoguht initially for personal use only. I am used to create playlists named with artist's name, therefore my playlist library on Spotify counts many playlists, many of which, however, contain a single song, or few of them.
I thought it could be useful to have a functionality allowing to take songs from all the playlist in the library and shuffle them, to have a continously, automatically updated queue of tracks.
So, I started developing Shuffler. It makes use of Spotify Web API and Spotify App Remote API in order to be able to retrieve information about playlists and tracks of the currently logged user and creates a queue out of them.

## Pre-requisites
- Shuffler requires the user to have a Spotify Premium subscription, otherwise many Spotify functionalities and API are disabled, and it would be impossible to control the queue and the playback.
- Obviously, Shuffler also needs an Internet connection to be able to authenticate the user and to retrieve up-to-date information about playlists and tracks.

## Usage
Shuffler is very ease to use. It is sufficient to open the application. Automatically, Shuffler will attempt to authenticate the user automatically. In case it is not possible to do it in an automatic fashion, Shuffler should prompt the user to login to its Spotify account. Once the user logged in, Shuffler starts retrieving playlists and tracks from the logged account.

Playlists and tracks are retrieved through the Spotify Web API, via simple HTTP requests and responses. It is possible for a user to have many tracks saved in the library and the actual number highly affects the time needed to Shuffler (i.e. fewer playlists with few tracks require much less time to be retrieved than many playlists, with many tracks). To avoid aving the user waiting order of 10^1 ***seconds*** to have all the information retrieved, Shuffler starts an already retrieved track everytime it observes the player is paused. This should allow Shuffler to complete the retrieval of information.

After this step has been completed, the app starts queueing tracks in the Spotify track queue. Also this step considers the variable number of tracks that the user could have saved in the library. If the app enqueued all the tracks together, the queue may be *"infinitely"* long, and difficult to be managed, both by Spotify (which could have difficulties rendering all of it), and by the user, which couldn't clear it easily, for instance.

To avoid these kind of problems, Shuffler won't enqueue all songs in the same moment, but only a specific number (default number is 20). When the queue is going to be entirely consumed (i.e. by default, when there are 2 or less tracks left in the queue), the app schedules other tracks, and repeats this operation until all tracks have been played once.

Since there is not an easy way to clear Spotify's queue (up to now there is no API offering methods to clear it), the only way the user have to interrupt the enqueueing of tracks is interrupting Shuffler and jump to the last song in the queue. This way, Shuffler won't schedule any new track, and the queue will be considered consumed.

## Shuffler v0.1-alpha limitations
- As explicitly announced in the description of the [release](https://github.com/kristopher-pellizzi/Shuffler/releases), this is a ***naive*** implementation. This means that the code is not optimized, nor well organized yet.
- This is also my first Android app ever developed. Therefore probably the code won't follow many *best practices*, nor is well designed as a mobile application. I'm also exploiting this project to learn and improve myself. 
- Many functionalities and checks are missing. For instance: HTTP requests are sent and managed through the Volley library. The retrieving step is considered concluded whenever the handler for the response of tracks from a playlist sees the list of user's playlists empty. However, in general, this is wrong. Since requests are sent through asynchronous tasks, the tasks consuming the list of playlists may access and consume the whole list quite fast. But if the playlists contain many tracks, responses for tracks requests may arrive with a higher latency and the first response arriving may already see the list of playlists empty. In order to avoid this, a check on pending requests should be done. This will be implemented in future versions.
Another missing check is on the error due to the number of requests to Spotify servers. As explained in the [Spotify Web API documentation](https://developer.spotify.com/documentation/web-api/), at section *Rate Limiting*, if too much requests are forwarded to Spotify servers, a response containing a status code 429 is sent back to the client. Shuffler won't check if this is happening, up to now. Therefore, if this happens, it will simply notify about the error, and stop working. Please, if this is the case be patient, a new version handling this situation will be out as soon as possible.
- Since much of the content of users library is always the same, Shuffler has been designed to make use of memory and/or disk caches to keep track of user's playlists and tracks. However, this version completely lacks cache, and always retrieves all the information from user's account. This also prevents Shuffler from working without an active Internet connection.
- The application is composed by a single activity, handling all the logic. This is not a good implementation, as the activity must handle both presentation of the UI and application's business logic. This also limits the freedom of the user: in order to work properly, Shuffler must keep its main activity open while the tracks are played in the player. If the Activity is closed, either by the user or by the system, also the enqueuing logic will be interrupted. As said above, I'll try to fix this ASAP.
