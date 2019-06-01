# Shuffler
*Utility Android app thought to shuffle tracks among all user's playlists*

## Introduction
This app has been thoguht initially for personal use only. I am used to create playlists named with artist's name, therefore my playlist library on Spotify counts many playlists, many of which, however, contain a single song, or few of them.
I thought it could be useful to have a functionality allowing to take songs from all the playlists in the library and shuffle them, to have a continously, automatically updated queue of tracks.
So, I started developing Shuffler. It makes use of [Spotify Web API](https://developer.spotify.com/documentation/web-api/) and [Spotify App Remote API](https://developer.spotify.com/documentation/android/) in order to be able to retrieve information about playlists and tracks of the currently logged user and creates a queue out of them.

## Pre-requisites
- Shuffler requires that Spotify application is installed on the device and that the user has logged in it with its account
- Shuffler requires the user to have a Spotify Premium subscription, otherwise many Spotify functionalities and API are disabled, and it would be impossible to control the queue and the playback.
- Obviously, Shuffler also needs an Internet connection to be able to authenticate the user and to retrieve up-to-date information about playlists and tracks.

## Installation
Go to the [releases](https://github.com/kristopher-pellizzi/Shuffler/releases) page of the project and download the last version of the apk archive.
On the Android device, go to Settings and enable apps from Unknown Sources. This step is different according to the different models of devices and versions of Android. Usually, the path is Settings > Advanced > Security.
Then, transfer the apk archive on the Android devices, if it is not already there, and open it.
A system prompt will ask a confirmation to install the archive from an unknown source. Accept.
Probably, there will be another prompt by Google services, warning the user that the developer of the app is unknown to Google Play developers database and asking again for user's confirmation. Again, accept it.
After that, Shuffler will be installed on the device, and ready to be used.
Please, read the [Usage](#Usage) and [Limitations][current-version-limitations] sections in the document to be fully aware of what the application can do and which are the main issues to be solved in the current release version.

If you find any unreported bug, or problem, or even if you have suggestions for possible functionalities or you are interested in becoming a contributor of the project, please contact me at krijojo@gmail.com, using Shuffler as part of the mail subject.

## Usage
Shuffler is very ease to use. It is sufficient to open the application. Automatically, Shuffler will attempt to authenticate the user. In case it is not possible to do it in an automatic fashion, Shuffler should prompt the user to login to its Spotify account. Once the user is logged, Shuffler starts retrieving playlists and tracks from the logged account.

Playlists and tracks are retrieved through the Spotify Web API, via simple HTTP requests and responses. It is possible for a user to have many playlists and/or tracks saved in the library and the actual number highly affects the time needed to Shuffler to complete the retrieval (i.e. fewer playlists with few tracks require much less time to be retrieved than many playlists, with many tracks). To avoid aving the user possibly waiting order of 10^1 ***seconds*** to have all the information retrieved, Shuffler starts an already retrieved track everytime it observes the player is paused. This should allow Shuffler to complete the retrieval of information, without bothering the user with long waiting times.

After this step has been completed, the app starts queueing tracks in the Spotify track queue. Also this step considers the variable number of tracks that the user could have saved in the library. If the app enqueued all the tracks together, the queue may be *"infinitely"* long, and difficult to be managed, both by Spotify (which could have difficulties rendering all of it), and by the user, which couldn't clear it easily, for instance.

To avoid these kind of problems, Shuffler won't enqueue all songs in the same moment, but only a specific number (default number is 20). When the queue is going to be entirely consumed (i.e. by default, when there are 2 or less tracks left in the queue), the app schedules other tracks, and repeats this operation until all tracks have been played once or the task is interrupted by the user.

Since there is not an easy way to clear Spotify's queue (up to now there is no API offering methods to clear it), the only way the user have to interrupt the enqueueing of tracks is interrupting Shuffler and jump to the last song in the queue. This way, Shuffler won't schedule any new track, and the queue will be considered consumed by the Spotify app.

# Application versions information 
*Current version: [v0.3.1-alpha][current-version]*

## Shuffler v0.1-alpha limitations
- As explicitly announced in the description of the [release](https://github.com/kristopher-pellizzi/Shuffler/releases), this is a ***naive*** implementation. This means that the code is not optimized, nor well organized yet.
- This is also my first Android app ever developed. Therefore probably the code won't follow many *best practices*, nor is well designed as a mobile application. I'm also exploiting this project to learn and improve myself. 
- Many functionalities and checks are missing. For instance: HTTP requests are sent and managed through the [Volley library](https://github.com/google/volley). The retrieving step is considered concluded whenever the handler for the response of tracks from a playlist sees the list of user's playlists empty. However, in general, this is wrong. Since requests are sent through asynchronous tasks, the tasks consuming the list of playlists may access and consume the whole list quite fast. But if the playlists contain many tracks, responses for tracks requests may arrive with a higher latency and the first response arriving may already see the list of playlists empty. In order to avoid this, a check on pending requests should be done. This will be implemented in future versions.
Another missing check is on the error due to the number of requests to Spotify servers. As explained in the [Spotify Web API documentation](https://developer.spotify.com/documentation/web-api/), at section *Rate Limiting*, if too much requests are forwarded to Spotify servers, a response containing a status code 429 is sent back to the client. Shuffler won't check if this is happening, up to now. Therefore, if this happens, it will simply notify about the error, and stop working. Please, if this is the case be patient, a new version handling this situation will be out as soon as possible.
- Since much of the content of users library is always the same, Shuffler has been designed to make use of memory and/or disk caches to keep track of user's playlists and tracks. However, this version completely lacks cache, and always retrieves all the information from user's account. This also prevents Shuffler from working without an active Internet connection.
- The application is composed by a single activity, handling all the logic. This is not a good implementation, as the activity must handle both presentation of the UI and application's business logic. This also limits the freedom of the user: in order to work properly, Shuffler must keep its main activity open while the tracks are played in the player. If the Activity is closed, either by the user or by the system, also the enqueuing logic will be interrupted. As said above, I'll try to fix this ASAP.

## Shuffler v0.2-alpha changelog
- Main activity simply makes initial checks and login
- A foreground service, in a new thread, implementing the enqueueing algorithm is launched after login
- Error responses with status code 429 (Spotify's servers error due to the too high number of requests done) are managed
- Before launching the foreground service, the application checks whether the user is logged with a premium Spotify account
- Pending requests are tracked in order to be sure that all responses have been received and to re-perform requests triggering error status code 429

## Shuffler v0.2-alpha limitations
- Both memory and disk caches still miss, therefore, it's still necessary to have an active Internet connection in order to let the application work
- Due to the lack of a proper method in the Spotify Web API or Spotify App Remote API, Shuffler can't have direct access to user's Spotify queue. There is a simple implementation of a queue embedded in the application, which tracks the songs played by Shuffler itself. However, if there is already a queue in the user's Spotify account, this might not work properly, at the beginning, causing the skipping of some tracks, or the repeat of some others. For the same reason, it might happen that the user's actual queue is longer than the default size. As soon as there will be methods available to get access to the queue, this will be fixed properly.

### **Warning**
Shuffler v0.2-alpha uses a *foreground service* to perform the enqueueing task in background. This goes on until the application has scheduled all the tracks retrieved once. Up to now there is no way to interrupt the service from the application. The only way the user can interrupt it is going in the device Settings (usual path is Settings > Developer Options > Active Services), look for active services and interrupt it. In future versions, the application will give the user the possibility to interrupt the service from within.
***NOTE***: if there is no Developer Options enabled in the device, just go into Settings > About Device and tap 7 times on the Build Number. This will enable Developer Options and add them in the Settings menu.

## Shuffler v0.3-alpha changelog
- UI automatically updates after launching the Enqueueing Service, allowing the user to easily dismiss the service. Also, since a possible bug has been detected, a second **temporary** button is provided to the user to force the enqueue of tracks.

## Shuffler v0.3-alpha limitations
- Same as v0.2-alpha. Check them out in the [Shuffler v0.2-alpha limitations](#shuffler-v02-alpha-limitations) section.
- The UI won't update again after service is dismissed. In order to relaunch the service the user must close the application from the recent apps tab and relaunch Shuffler.

## **Warning** for Huawei devices owners only
Shuffler makes use of a *foreground service* in order to allow the application enqueue new songs until the end without the application being actually open. In huawei devices, however, there is an implementation of *Protected Apps*. In practice, each app that is not set as 'Protected', will be closed when the screen is locked or the process is cleared from the recent apps tab. To allow Shuffler working properly, please enable it as a Protected App in the device settings. Usually, the path is Settings > Advanced > Battery > Protected Apps. However, this may vary according to the device model and Android version, as well. In future implementations there will be a disclaimer notifying the user about the necessity of declaring the application as a Protected App.

[current-version]: #shuffler-v03-alpha-changelog
[current-version-limitations]: #shuffler-v03-alpha-limitations
