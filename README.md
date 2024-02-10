# Spotify Bot
Spotify Bot for everyone to use. The main functionality is to have users add songs to the queue (via redeem or command). Additional functionality can be found below or be requested.<br>
Though the Bot is free to use, consider giving credit on your twitch via command or panel.<br>
Created by [alexshadowolex](https://www.twitch.tv/alexshadowolex)

Table of Contents:
1. [Current Versions](#current-versions)
2. [How to set up](#how-to-set-up)
   1. [Requirements](#requirements)
   2. [Setup Steps](#setup-steps)
      1. [Download and save](#download-and-save)
      2. [Set Up Project Structure](#set-up-project-structure)
      3. [Update Properties](#update-properties)
      4. [Set Up Spotify Token](#set-up-spotify-token)
3. [How to Update](#how-to-update)
4. [Current functionalities](#current-functionalities)
5. [Contents of data-files explained](#contents-of-data-files-explained)
6. [Additional Information](#additional-information)

## Current Versions
* Spotify-Bot.jar: v1.2.4 [Download from Release v1.2.4](https://github.com/alexshadowolex/Spotify-Bot/releases/download/v1.2.4/Spotify.Bot-windows-x64-1.2.4.jar)
* SetupToken.jar: v3 [Download from Release v1.2.1](https://github.com/alexshadowolex/Spotify-Bot/releases/download/v1.2.1/SetupToken_v3.jar)
* SetupProject.jar: v1 [Download from Release v1.2.1](https://github.com/alexshadowolex/Spotify-Bot/releases/download/v1.2.1/SetupProject_v1.jar)
* UpdateProperties.jar: v1.2.4 [Download from Release v1.2.4](https://github.com/alexshadowolex/Spotify-Bot/releases/download/v1.2.4/UpdateProperties_1-2-4.jar)

## How to set up

### Requirements
You need to have java 17 or higher installed on your pc. To check if you have, open a cmd (type "cmd" bottom left into search bar) and type "java --version". If it responds with a version, you will see the version number. If not, you can download java 19 installer from here: [Oracle Java 19](https://download.oracle.com/java/19/archive/jdk-19.0.2_windows-x64_bin.msi).
After installing, check again in a new cmd if it is installed.
<br>
Also the app only works on windows.
<br><br>

### Setup Steps
#### Download and save
Download all files and put them on the same level, like this:
````
-> someFolder
    -> SetupProject.jar
    -> SetupToken.jar
    -> UpdateProperties.jar
    -> Spotify-Bot.jar
````
During the setup process, you will need to execute those jars in a specific order. Execute them from that location.
<br><br>
#### Set Up Project Structure
First we need to set up the project structure. Execute the newest version of the file SetupProject.jar.<br>
You will see this window:
<br>
![setupProject.png](images/setupProject.png)
<br>
To get the Twitch Token, login into the twitch account and go in another tab to [Token Generator](https://twitchtokengenerator.com/) and click on "bot chat token", then copy the "access token" into the field.
<br><br>
Both the Spotify ID and Spotify Secret need to get copied from the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard). Login with your spotify credentials and do following steps to get them:<br>
1.) Click on "Create App"<br>
2.) Give your app a name<br>
3.) Write into the field "Redirect URI" the address "https://www.example.com". If you chose a different URI, the setup will not work.<br>
4.) Select "Web API". You can also select more, if you want to.<br>
5.) Agree to the TOS and click on "Save"<br>
6.) Go to "Settings". There you see the client ID. After clicking on "View Client Secret", you also see the secret. Copy both of them into the given field.<br>
<br>
Now Click in SetupProject on "Setup Project". After a success message, you can close this window.

Now the data-folder should exist with following structure:
````
-> data
    -> properties
        -> spotifyConfig.properties
        -> twitchBotconfig.properties
    -> tokens
        -> twitchtoken.txt
        -> spotifyClientSecret.txt
````
<br><br>
#### Update Properties
Now we need to fill the files with the needed properties. For that, execute the UpdateProperties.jar of the newest project version.
After doing that, a cmd will open and log the added properties:<br>
![updateProperties.png](images/updateProperties.png)
<br>
The properties-files in folder data\properties now have the needed entries. They only have default values at the moment.<br>
You will get a detailed explanation of the properties later. For now, we only need to change 1 property:<br>
Go into the file data\properties\twitchBotConfig.properties. There you have the entry "channel_name" -> remove everything after the "=" and add your twitch-channel name.
<br><br>
#### Set Up Spotify Token
Now we need to set up the spotify token. Execute the latest version of SetupToken.jar. Two things will happen: A Browser Tab opens and following window will be visible:<br>
![setupToken.png](images/setupToken.png)
<br>
Go into the browser tab. It has the website of the redirect URI open, example.com. In the URL, you will see following text:<br>
![exampleCom.png](images/exampleCom.png)
<br>
Copy everything after "?code=" and paste it into the text field. Click now on "Generate Spotify Token". Close the window after the success message.
<br>
Now you will find "spotifyToken.json" in data\tokens\. 
<br><br><br>

With this, the setup is done. You can execute the Spotify-Bot.jar with the current version on the same level as all the other files. The data folder needs to be next to it.<br>
On start up, the bot will display a message in chat to confirm start up. <br>

## How to Update
If an update is released, you can find it under [Releases](https://github.com/alexshadowolex/spotify-bot/releases/latest). In this release, the latest files are found.
<br>
If the release includes a file "UpdateProperties_<version>.jar" it means that new properties have been added during the release. Download this file and execute it first so all needed properties are there. The cmd will inform you, which properties in which files are new.
<br>
If the release includes new versions of the other script files (SetupProject, SetupToken), they are the latest versions. Older version might not work anymore. Download, if needed.
<br>
The file, that will always be included, is the new Bot version. Download it and swap out the old version with the new one.

## Current functionalities
* Song Request -> Can be toggled off (is_song_request_enabled_by_default, switch in UI). Either with redeem or command. Takes a spotify-link or a text and then adds the result to the queue. You can set the maximum length of song duration in spotifyConfig.properties (maximum_length_minutes_song_request).
* Song Name Getter -> Can be toggled off (is_spotify_song_name_getter_enabled_by_default, switch in UI). Gets the current song name, artists and album image and writes it into different files:
  * data\displayFiles\currentSong.txt -> Song Name + "by" + Artists + 10 spaces (for moving text display)
  * data\displayFiles\currentSongName.txt -> Song Name
  * data\displayFiles\currentSongArtists.txt -> Song Artists
  * data\displayFiles\currentAlbumImage.jpg -> Current song album's image
  <br><br>
  The different files are thought so every user can use the parts they want to use.
  <br>
  There is a functionality to empty those files on pause (is_empty_song_display_files_on_pause_enabled_by_default, switch in UI).
  <br>
  This will only happen, if the function is active and the bot has been started.
* Update Checker -> Can be toggled off (show_new_version_available_window_on_start_up). Checks for new versions on GitHub and if there is one, a window will appear.
* Song Info Command -> Can be toggled off (is_song_info_command_enabled_by_default, switch in UI). Command to display the current song in twitch chat together with a spotify link.
* Add Song Command -> Can be toggled off (is_add_song_command_enabled_by_default, switch in UI). Command to add the current song to a certain playlist. This playlist is identified by an ID set in spotifyConfig.properties with the parameter playlist_id_for_add_song_command. <br>
  To obtain this ID, simply extract it from the share-link: <br>
  E.g. the ID in this share link: https://open.spotify.com/playlist/idinsidethislink?si=abc1234569ada is "idinsidethislink", the random characters between "playlist/" and "?si". Just crop it out and paste it into the properties-file.<br>
  This functionality can be restricted to be used only by certain user groups:
  * BROADCASTER: Only the broadcaster
  * MODERATOR: Owner and Moderators
  * EVERYONE: Everyone (careful with that one, your playlist might get filled with too many songs)<br>
  
  This security setting can be done in the UI and in the properties files. Note that the value is case-sensitive.

## Contents of data-files explained
The files consist of following content:
````
data\tokens\twitchtoken.txt: only the twitch token, nothing else
````
````
data\tokens\spotifyClientSecret.txt: only the spotify client secret from the app, nothing else
````
````
data\properties\twitchBotconfig.properties:
    ->channel=<Twitch channel Name>
    ->command_prefix=<prefix for command usage>
    ->default_command_cool_down=<cool down in seconds for commands>
    ->default_user_cool_down=<cool down in seconds for users per command>
    ->song_request_redeem_id=<the ID of the song request redeem>
    ->song_request_emotes=<Twitch emotes that will be used after confirmation of an added song, seperated by ",">
    ->is_song_request_command_enabled_by_default=<true or false. If this is false, the redeem will be enabled on app start instead>
    ->blacklisted_users=<list of Twitch names/IDs of users that can't use the redeem/command, seperated by ",">
    ->blacklist_emote=<Twitch emote that will be displayed after the message towards a blacklisted user>
    ->is_spotify_song_name_getter_enabled_by_default=<true or false. If this is true, the name getter functionality is enabled on start up>
    ->show_new_version_available_window_on_start_up=<true or false. If this is true, the bot checks on start up for a new version on GitHub and if there is one, it will open a window>
    ->is_song_request_enabled_by_default=<true or false. If this is true, the song request functionality is enabled on start up>
    ->is_song_info_command_enabled_by_default=<true or false. If this is true, the song info command is enabled on start up>
    ->is_empty_song_display_files_on_pause_enabled_by_default=<true or false. If this is true, the song display files emptying on pause functionality is enabled on start up>
    ->is_add_song_command_enabled_by_default=<true or false. If this is true, the add song command is enabled on start up>
````
````
data\properties\spotifyConfig.properties:
    ->spotify_client_id=<client ID from spotify app>
    ->playlist_id_for_add_song_command=<playlist ID for add song command. Check the command description for how to obtain the ID>
    ->add_song_command_security_level_on_start_up=<security level that decides, who can use the add song command. Possible values: BROADCASTER, MODERATOR, EVERYONE>
    ->maximum_length_minutes_song_request<Maximum song length for songrequests. This value is interpreted as Minutes (you can also use decimal numbers). If you don't want a maximum lenght limit, just leave this property empty.>
````
Fill out all the properties in twitchBotConfig.properties and spotifyConfig.properties with data that fits. You can leave empty "blacklisted_users" and the redeem ID (if you are not going to use it). If you want to leave them empty, you still have to include them in the file. An empty property looks like this "blacklisted_users=".
<br>
If you don't have the redeem ID, you have 2 possible solutions:<br>
1.) Use the redeem's name instead (on the property song_request_redeem_id). Start the bot and use the redeem once. Then go into the latest log-file and check for a warning that says you used the name and should use the ID instead. Copy and Paste it and done.<br>
2.) Go to https://www.instafluff.tv/TwitchCustomRewardID/?channel=<channel_name> (swap out <channel_name> with your channel). Then use the redeem on your channel, the site will show you the redeem ID.


## Additional Information
* Use any compiled binary (.jar-files) at your own risk. They could be doing anything without you knowing it. Either you trust me and my builds or not. I will take no responsibility for anything.
* You can change the properties' values at any time, but they will only be applied after the next start up.
* Make sure to check on releases, if there are newer version of the project-setup- or setup-token-jar, if needed.
* If an error occurred (a window with an error message appeared), read the error message. The most common mistake is a property missing or having invalid data. A property missing can be identified like this: <br>
  ![exampleMissingProperty.png](images/exampleMissingProperty.png)
  <br>
  Here you can see the name of the faulty property and the file it is located in.
* If any issues occurred, message me on Discord (alexshadowolex) or on [Twitch](https://twitch.tv/alexshadowolex). Please also provide the log files and tell me, what went wrong.
* Also feel free to message me for any improvement ideas.
