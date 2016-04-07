# Unisong-Android

Unisong is an application that syncs up the speakers on multiple phones to create a speaker system.


Unisong is loosely organized into three main components: Activity, Audio, and Network.

## Activity

This segment contains the various activities (screens) of Unisong. 

## Audio

This segment contains the following:

#### Song

The Song class has two subclasses, LocalSong and UnisongSong. LocalSong is used for a song contained on your device, while UnisongSong is used for a song that is being played from another device. 

#### AudioTrackManager

AudioTrackManager is a singleton class used to synchronize the playback of the song data.

#### AudioStatePublisher

AudioStatePublisher is a Publisher class that notifies all registered AudioObservers of changes in the audio state (play, pause, seek, etc.)

#### FileDecoder

This class simply takes in a file path and decodes it to PCM data. It is used for both the playing of a local song and the conversion to AAC (File -> PCM -> AAC)

#### SongDecoder
