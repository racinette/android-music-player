package service;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.prett.myapplication.R;

import java.util.ArrayList;

import activity.MainActivity;
import alertdialog.AddPlaylistAlertDialog;
import audioeffect.EffectBundle;
import broadcast.Extras;
import broadcast.Messages;
import helper.Methods;
import playlist.AbstractPlaylist;
import playlist.Album;
import playlist.AlbumShell;
import playlist.Artist;
import playlist.ArtistShell;
import playlist.Genre;
import playlist.GenreShell;
import playlist.MainPlaylist;
import playlist.Playlist;
import playlist.PlaylistShell;
import playlist.Track;

public class PlayerService extends Service{

    /*
        DATA OF THE WHOLE APP LIES HERE, IN THIS SERVICE
        It is ideal to keep global app data in such a service, since it is always in the background
        as long as the app lives
     */

    // DATA DECLARATION STARTS HERE
    private EffectBundle allSongsPreset;
    private ArrayList<EffectBundle> effectBundles;

    private final int MAIN_PLAYLIST_INDEX = 0;

    private AbstractPlaylist currentPlaylist;
    private AbstractPlaylist selectedPlaylist;
    private Track currentTrack;

    private boolean musicPlaying;

    private int seekBarMax;

    private MainPlaylist mainPlaylist;

    private ArrayList<AbstractPlaylist> playlists;
    private ArrayList<Album> albums;
    private ArrayList<Artist> artists;
    private ArrayList<Genre> genres;
    // DATA DECLARATION ENDS HERE

    private static final int NO_TRACK = -1;

    private int state = 0;
    private static final int IDLE = 0;
    private static final int INITIALIZED = 1;
    private static final int PREPARED = 2;
    private static final int STARTED = 3;
    private static final int PAUSED = 4;
    private static final int STOPPED = 5;
    private static final int RESET = 6;

    private static boolean running = false;
    private boolean isLooping = false;

    public static boolean isRunning() {
        return running;
    }

    private MediaPlayer mediaPlayer;

    private AbstractPlaylist.OnSizeChangeListener sizeChangeListener = new AbstractPlaylist.OnSizeChangeListener() {
        @Override
        public void onChange(AbstractPlaylist playlist) {
            notifyPlaylistSizeChanged(playlist);
        }
    };

    private AbstractPlaylist.OnShuffleStateChangeListener stateChangeListener = new AbstractPlaylist.OnShuffleStateChangeListener() {
        @Override
        public void onChange(AbstractPlaylist playlist, boolean shuffled) {
            if (shuffled){
                sendShuffleSettings(playlist);
            }
        }
    };

    private final MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {

            // update seek bar's values
            seekBarMax = mediaPlayer.getDuration();

            sendAlbumCover(currentTrack);
            sendTrackData(currentTrack);

            state = PREPARED;

            // if music was previously playing, start playing the new track
            if (musicPlaying){
                play();
            }
            mediaPlayer.setOnPreparedListener(null);
        }
    };

    private final BroadcastReceiver menuDataRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            boolean requested = intent.getBooleanExtra(Extras.REQUESTED_EXTRA, false);

            if (requested){
                final PlaylistShell [] playlistShells = new PlaylistShell[playlists.size()];
                for (int i = 0; i < playlists.size(); i++){
                    playlistShells[i] = new PlaylistShell(playlists.get(i));
                }

                final GenreShell [] genreShells = new GenreShell[genres.size()];
                for (int i = 0; i < genres.size(); i++){
                    genreShells[i] = new GenreShell(genres.get(i));
                }

                final AlbumShell [] albumShells = new AlbumShell[albums.size()];
                for (int i = 0; i < albums.size(); i++){
                    albumShells[i] = new AlbumShell(albums.get(i));
                }

                final ArtistShell [] artistShells = new ArtistShell[artists.size()];
                for (int i = 0; i < artists.size(); i++){
                    artistShells[i] = new ArtistShell(artists.get(i));
                }

                Intent data = new Intent(Messages.MENU_DATA_MESSAGE);

                data.putExtra(Extras.PLAYLISTS_EXTRA, playlistShells);
                data.putExtra(Extras.GENRES_EXTRA, genreShells);
                data.putExtra(Extras.ALBUMS_EXTRA, albumShells);
                data.putExtra(Extras.ARTISTS_EXTRA, artistShells);

                LocalBroadcastManager.getInstance(PlayerService.this).sendBroadcast(data);
            }
        }
    };

    private final BroadcastReceiver playlistTitlesRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final int FIRST_USER_PLAYLIST_INDEX = 1;

            boolean mainIsSelected = selectedPlaylist == mainPlaylist;

            String [] titles = new String[playlists.size() - (mainIsSelected ? 1 : 2)];

            int index = 0;
            // starts from 1 because 0 is the main playlist
            for (int i = FIRST_USER_PLAYLIST_INDEX; i < playlists.size(); i++) {
                AbstractPlaylist playlist = playlists.get(i);
                if (playlist != selectedPlaylist){
                    titles[index] = playlist.getTitle();
                    index++;
                }
            }

            Intent message = new Intent(Messages.PLAYLIST_FRAGMENT_TITLES_MESSAGE);
            message.putExtra(Extras.PLAYLIST_TITLES_ARRAY_EXTRA, titles);
            LocalBroadcastManager.getInstance(PlayerService.this).sendBroadcast(message);
        }
    };

    private final BroadcastReceiver changeSelectedPlaylistReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final int PLAYLIST = 0;
            final int ALBUM = 1;
            final int ARTIST = 2;
            final int GENRE = 3;

            int type = intent.getIntExtra(Extras.TYPE_EXTRA, -1);
            int index = intent.getIntExtra(Extras.INDEX_EXTRA, -1);

            if (type > -1 && index > -1){
                switch (type) {
                    case PLAYLIST:
                        setSelectedPlaylist(playlists.get(index));
                        break;
                    case ALBUM:
                        setSelectedPlaylist(albums.get(index));
                        break;
                    case ARTIST:
                        setSelectedPlaylist(artists.get(index));
                        break;
                    case GENRE:
                        setSelectedPlaylist(genres.get(index));
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver loopBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isLooping = intent.getBooleanExtra(Extras.LOOPING_EXTRA, false);
            if (mediaPlayer != null){
                mediaPlayer.setLooping(isLooping);
            }
        }
    };

    private final BroadcastReceiver buttonActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int msg = intent.getIntExtra(Extras.BUTTON_CLICKED_EXTRA, -1);
            switch (msg){
                // 0 -- pause
                // 1 -- play
                // 2 -- next track
                // 3 -- previous track
                case Extras.PAUSE_CLICKED:
                    if (mediaPlayer != null){
                        //Log.e("PLAYERSERVICE", "paused with button");
                        pause();
                    }
                    break;
                case Extras.PLAY_CLICKED:
                    if (mediaPlayer != null){
                        play();
                    } else {
                        if (currentPlaylist.size() > 0){
                            setCurrentTrack(0);
                        }
                    }
                    break;
                case Extras.NEXT_CLICKED:
                    nextTrack();
                    break;
                case Extras.PREV_CLICKED:
                    previousTrack();
                    break;
            }
        }
    };

    private final BroadcastReceiver addPlaylistReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String title = intent.getStringExtra(Extras.PLAYLIST_TITLE_EXTRA);
            if (title != null){
                playlists.add(new Playlist(title));
            }
        }
    };

    private final BroadcastReceiver seekBarUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int pos = intent.getIntExtra(Extras.USER_CHANGE_POS_EXTRA, 0);
            if (mediaPlayer != null){
                mediaPlayer.seekTo(pos);
            }
        }
    };

    /*
    private final BroadcastReceiver pausePlaybackMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mediaPlayer != null){
                Log.e("PLAYERSERVICE", "onReceive: pausePlaybackMessage");
                pause();
                notifyPlayerStateChangedMessage();
            }
        }
    };
    */

    private void notifyPlayerStateChangedMessage(){
        Intent intent = new Intent(Messages.PLAYER_STATE_CHANGED_MESSAGE);
        intent.putExtra(Extras.MUSIC_IS_PLAYING_EXTRA, musicPlaying);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private final BroadcastReceiver trackFromPlaylistReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int ind = intent.getIntExtra(Extras.TRACK_INDEX_EXTRA, -1);
            boolean changePlaylist = intent.getBooleanExtra(Extras.PLAYLIST_CHANGE_FLAG_EXTRA, false);
            boolean startPlayingMusic = intent.getBooleanExtra(Extras.START_PLAYING_MUSIC_EXTRA, false);

            if (changePlaylist){
                Log.d("", "playlist changed");
                setCurrentPlaylist(selectedPlaylist);
            }

            setMusicPlaying(startPlayingMusic || musicPlaying);
            setCurrentTrack(ind);
        }
    };

    private final BroadcastReceiver albumCoverRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            sendAlbumCover(currentTrack);
        }
    };

    private final BroadcastReceiver currentPlaylistShuffleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentPlaylist.shuffle();
        }
    };

    private final BroadcastReceiver removeTracksReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int [] selectedIndices = intent.getIntArrayExtra(Extras.INDICES_ARRAY_EXTRA);

            selectedPlaylist.removeTracks(selectedIndices);
            // if selected playlist is the same as current playlist, then there is a chance
            // that the currently playing track was removed
            if (selectedPlaylist == currentPlaylist){
                if (!selectedPlaylist.isEmpty()){
                    if (selectedPlaylist.getLastAccessedTrackIndex() == AbstractPlaylist.UNDEFINED){
                        // if current track was removed, set the first track in the playlist to play
                        setCurrentTrack(0);
                    } // else the track was not removed and it continues to play
                } else {
                    // there are no elements in the playlist left -- stop playback
                    setCurrentTrack(NO_TRACK);
                }
            }
        }
    };

    private final BroadcastReceiver deleteTracksReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int [] which = intent.getIntArrayExtra(Extras.INDICES_ARRAY_EXTRA);

            selectedPlaylist.deleteTracks(which, PlayerService.this);

            if (selectedPlaylist == currentPlaylist){
                if (!selectedPlaylist.isEmpty()){
                    if (selectedPlaylist.getLastAccessedTrackIndex() == AbstractPlaylist.UNDEFINED){
                        // if current track was removed, set the first track in the playlist to play
                        setCurrentTrack(0);
                    } // else the track was not removed and it continues to play
                } else {
                    // there are no elements in the playlist left -- stop playback
                    setCurrentTrack(NO_TRACK);
                }
            }
        }
    };

    private final BroadcastReceiver addTracksReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int UNCONDITIONAL_SHIFT = 1;
            final int LAST_ADDED_PLAYLIST = -1;
            final int UNDEFINED = -2;

            int index = intent.getIntExtra(Extras.PLAYLIST_INDEX_EXTRA, -2);
            if (index > UNDEFINED){
                int [] tracksToAdd = intent.getIntArrayExtra(Extras.INDICES_ARRAY_EXTRA);

                if (index == LAST_ADDED_PLAYLIST){
                    index = playlists.size() - 1;

                    // playlist was added, BUT menu fragment doesn't know about it
                    // notify it

                    Intent message = new Intent(Messages.MENU_FRAGMENT_NEW_PLAYLIST_MESSAGE);
                    message.putExtra(Extras.ADDED_PLAYLIST_EXTRA, new PlaylistShell(playlists.get(index)));
                    LocalBroadcastManager.getInstance(PlayerService.this).sendBroadcast(message);
                } else {
                    // there is always an unconditional shift in the index
                    // Main Playlist is always the first one on the list
                    // so the index must be incremented

                    // threshold is a value after which the index must be incremented again
                    // since there is no way a user could have added tracks from selected playlist
                    // to the selected playlist

                    final int threshold = playlists.indexOf(selectedPlaylist);

                    // if selected playlist is not main playlist
                    if (threshold != 0){
                        index = index >= threshold ? (index + 1) : index;
                    }
                    // complement for the unconditional shift
                    index += UNCONDITIONAL_SHIFT;
                }

                AbstractPlaylist target = playlists.get(index);

                target.addTracks(selectedPlaylist, tracksToAdd);
            }
        }
    };

    private final BroadcastReceiver deletePlaylistReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int index = intent.getIntExtra(Extras.INDEX_EXTRA, -1);
            if (index > -1){
                final AbstractPlaylist playlist = playlists.get(index);

                // if current playlist is the deleted one,
                // change the current playlist to MainPlaylist
                // find the currently playing track in the MainPlaylist
                // set it playing as if nothing happened
                if (currentPlaylist == playlist){
                    setCurrentPlaylist(mainPlaylist);
                    currentPlaylist.setLastAccessedTrack(currentTrack);
                }
                // if the deleted playlist is currently selected, then select the current playlist instead
                if (selectedPlaylist == playlist){
                    setSelectedPlaylist(currentPlaylist);
                }

                playlist.clear();

                playlists.remove(index);
            }
        }
    };

    private final BroadcastReceiver playlistShuffleStateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isShuffled = intent.getBooleanExtra(Extras.IS_SHUFFLED_EXTRA, false);

            if (isShuffled){
                selectedPlaylist.shuffle();
            } else {
                selectedPlaylist.unshuffle();
            }
        }
    };

    private final BroadcastReceiver selectCurrentPlaylistReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean changePlaylist = intent.getBooleanExtra(Extras.PLAYLIST_CHANGE_FLAG_EXTRA, false);
            if (changePlaylist){
                setSelectedPlaylist(currentPlaylist);
            }
        }
    };

    private final BroadcastReceiver trackDataRequestReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.e("PlayerService", "onReceive: track data request received");
            sendTrackData(currentTrack);
        }
    };

    private final BroadcastReceiver playlistDemandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isDemanded = intent.getBooleanExtra(Extras.PLAYLIST_DEMAND_FLAG_EXTRA, false);
            if (isDemanded){
                sendPlaylistData(currentPlaylist, selectedPlaylist);
            }
        }
    };

    private void sendPlaylistData(AbstractPlaylist currentPlaylist, AbstractPlaylist selectedPlaylist){
        Intent playlistData = new Intent(Messages.PLAYLIST_DATA_MESSAGE);

        playlistData.putExtra(Extras.SELECTED_PLAYLIST_EXTRA, selectedPlaylist.toParcelable(currentPlaylist == selectedPlaylist));
        playlistData.putExtra(Extras.CURRENT_PLAYLIST_TITLE_EXTRA, currentPlaylist.getTitle());

        LocalBroadcastManager.getInstance(PlayerService.this).sendBroadcast(playlistData);
    }

    private final BroadcastReceiver audioSessionDemandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean demanded = intent.getBooleanExtra(Extras.AUDIO_SESSION_DEMAND_EXTRA, false);
            if (demanded){
                sendAudioSessionData();
            }
        }
    };

    private final MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            currentTrack.listened();
            mediaPlayer.setOnCompletionListener(null);

            // if it is the last track in the playlist, pause the next track's playback
            boolean isLast = currentPlaylist.getLastAccessedTrackIndex() == currentPlaylist.size() - 1;

            setMusicPlaying(!isLast);
            nextTrack();
        }
    };

    // this thread sends current mediaplayer's position to the seek bar every 200 millis
    private final Thread thread = new Thread(){
        boolean stopped = true;

        @Override
        public void run() {
            while (!stopped){
                if (mediaPlayer != null && state == STARTED){
                    Intent message = new Intent(Messages.SEEK_BAR_POS_MESSAGE);
                    message.putExtra(Extras.POSITION_EXTRA, mediaPlayer.getCurrentPosition());
                    LocalBroadcastManager.getInstance(PlayerService.this).sendBroadcast(message);
                    try {
                        sleep(200);
                    } catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                }
            }
        }

        @Override
        public synchronized void start() {
            stopped = false;
            super.start();
        }

        @Override
        public void interrupt() {
            stopped = true;
            super.interrupt();
        }
    };




    // METHODS START HERE


    private void sendTrackData(Track track){
        Intent trackData = new Intent(Messages.NEW_TRACK_MESSAGE);
        boolean empty = track == null;
        if (empty) {
            trackData.putExtra(Extras.EMPTY_EXTRA, true);
        } else {
            trackData.putExtra(Extras.EMPTY_EXTRA, false);
            track.toParcel(trackData, Extras.TRACK_EXTRA);
            trackData.putExtra(Extras.MUSIC_IS_PLAYING_EXTRA, musicPlaying);
            trackData.putExtra(Extras.SIZE_EXTRA, currentPlaylist.size());
            trackData.putExtra(Extras.CURRENT_INDEX_EXTRA, currentPlaylist.getLastAccessedTrackIndex());
            trackData.putExtra(Extras.MAX_POSITION_EXTRA, seekBarMax);
            trackData.putExtra(Extras.LOOPING_EXTRA, isLooping);
        }

        LocalBroadcastManager.getInstance(PlayerService.this).sendBroadcast(trackData);
    }

    private void setMusicPlaying(boolean playing){
        musicPlaying = playing;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        thread.start();

        running = true;

        allSongsPreset = EffectBundle.getStandardPreset();
        effectBundles = new ArrayList<>();
        musicPlaying = false;

        playlists = new ArrayList<>();
        albums = new ArrayList<>();
        artists = new ArrayList<>();
        genres = new ArrayList<>();

        mainPlaylist = new MainPlaylist();

        playlists.add(mainPlaylist);

        setCurrentPlaylist(mainPlaylist);
        setSelectedPlaylist(mainPlaylist);

        retrieveTracks();

        if (!mainPlaylist.isEmpty()) setCurrentTrack(0);

        registerReceivers();

        return START_STICKY;
    }

    private void registerReceivers(){
        LocalBroadcastManager.getInstance(this).registerReceiver(buttonActionReceiver, new IntentFilter(Messages.BUTTON_CLICKED_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(seekBarUserChangeReceiver, new IntentFilter(Messages.SEEK_BAR_USER_CHANGE_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(trackFromPlaylistReceiver, new IntentFilter(Messages.TRACK_FROM_PLAYLIST_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(loopBroadcastReceiver, new IntentFilter(Messages.SET_LOOPING_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(audioSessionDemandReceiver, new IntentFilter(Messages.AUDIO_SESSION_DEMAND_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(addPlaylistReceiver, new IntentFilter(Messages.ADD_PLAYLIST_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(playlistDemandReceiver, new IntentFilter(Messages.PLAYLIST_DEMAND_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(albumCoverRequestReceiver, new IntentFilter(Messages.ALBUM_COVER_REQUEST_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(trackDataRequestReceiver, new IntentFilter(Messages.TRACK_DATA_REQUEST_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(selectCurrentPlaylistReceiver, new IntentFilter(Messages.CHANGE_PLAYLIST_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(currentPlaylistShuffleReceiver, new IntentFilter(Messages.CURRENT_PLAYLIST_SHUFFLE_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(addTracksReceiver, new IntentFilter(Messages.ADD_TRACKS_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(playlistShuffleStateChangeReceiver, new IntentFilter(Messages.PLAYLIST_STATE_CHANGED_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(removeTracksReceiver, new IntentFilter(Messages.REMOVE_TRACKS_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(deleteTracksReceiver, new IntentFilter(Messages.DELETE_TRACKS_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(menuDataRequestReceiver, new IntentFilter(Messages.MENU_DATA_REQUEST_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(changeSelectedPlaylistReceiver, new IntentFilter(Messages.CHANGE_SELECTED_PLAYLIST_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(deletePlaylistReceiver, new IntentFilter(Messages.DELETE_PLAYLIST_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(renamePlaylistReceiver, new IntentFilter(Messages.RENAME_PLAYLIST_MESSAGE));
        LocalBroadcastManager.getInstance(this).registerReceiver(playlistTitlesRequestReceiver, new IntentFilter(Messages.REQUEST_PLAYLIST_TITLES_MESSAGE));
    }

    private void unregisterReceivers(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(buttonActionReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(seekBarUserChangeReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(trackFromPlaylistReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(loopBroadcastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(audioSessionDemandReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(addPlaylistReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playlistDemandReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(albumCoverRequestReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(trackDataRequestReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(selectCurrentPlaylistReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(currentPlaylistShuffleReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(addTracksReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playlistShuffleStateChangeReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(removeTracksReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(deleteTracksReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(menuDataRequestReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(changeSelectedPlaylistReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(deletePlaylistReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(renamePlaylistReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(playlistTitlesRequestReceiver);


    }

    private void play(){
        //Log.e("PLAYERSERVICE", "PLAY");
        mediaPlayer.start();
        setMusicPlaying(true);
        state = STARTED;
    }

    private void pause(){
        //Log.e("PLAYERSERVICE", "PAUSE");
        mediaPlayer.pause();
        setMusicPlaying(false);
        state = PAUSED;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void nextTrack(){
        int currentIndex = (currentPlaylist.getLastAccessedTrackIndex() + 1) % currentPlaylist.size();

        setCurrentTrack(currentIndex);
    }
    
    private void previousTrack(){
        int currentIndex = currentPlaylist.getLastAccessedTrackIndex();

        if (currentIndex - 1 < 0){
            currentIndex = currentPlaylist.size() - 1;
        } else {
            currentIndex--;
        }

        setCurrentTrack(currentIndex);
    }


    private void sendAudioSessionData(){
        if (mediaPlayer != null){
            int audioSession = mediaPlayer.getAudioSessionId();
            Intent intent = new Intent(Messages.AUDIO_SESSION_MESSAGE);
            intent.putExtra(Extras.AUDIO_SESSION_EXTRA, audioSession);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    private void setCurrentTrack(int index){
        Track track;

        // getting rid of the previous media player if it exists
        if (mediaPlayer != null){
            releaseMediaPlayer();
        }

        if (index < currentPlaylist.size() && index > -1){
            track = currentPlaylist.getTrack(index);

            // create a new media player instance
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, track.getUri());
                state = INITIALIZED;

                // on prepared listener invokes methods sendAlbumCover() and sendTrackData()
                mediaPlayer.setOnPreparedListener(onPreparedListener);

                mediaPlayer.prepareAsync();
                mediaPlayer.setOnCompletionListener(onCompletionListener);
                if (isLooping) {
                    mediaPlayer.setLooping(true);
                }
            } catch (Exception ex) {
                //track.remove();
                nextTrack();
            }
        } else {
            track = null;
            sendAlbumCover(null);
            sendTrackData(null);
        }

        currentTrack = track;
    }

    @Override
    public void onDestroy() {
        thread.interrupt();
        running = false;

        unregisterReceivers();

        if (mediaPlayer != null){
            releaseMediaPlayer();
        }
        super.onDestroy();
    }

    private void releaseMediaPlayer(){
        if (mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            mediaPlayer.stop();
        }
        mediaPlayer.reset();
        mediaPlayer.release();
        state = IDLE;
        mediaPlayer = null;
    }

    // TRACK RETRIEVAL

    private void retrieveTracks(){
        // specify which columns you want returned
        final String [] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.IS_MUSIC,
                MediaStore.Audio.Media._ID
        };

        final String [] genresProjection = {
                MediaStore.Audio.Genres._ID,
                MediaStore.Audio.Genres.NAME
        };

        // sort order is ascending by title
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        // create a cursor
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder);

        if (cursor == null){
            // if it's null there is some problem
            Toast.makeText(this, R.string.access_error, Toast.LENGTH_LONG).show();
        } else {
            // ensure that the playlist contains at least the amount of tracks in device's database
            int querySize = cursor.getCount();
            if (mainPlaylist.size() < querySize){
                mainPlaylist.ensureCapacity(querySize);
            }

            // else everything is OK
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
            int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int isMusicColumn = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC);
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);

            // number of found tracks
            int tracksFound = 0;

            // while there are tracks on device's database, do
            while (cursor.moveToNext()){
                int isMusic = cursor.getInt(isMusicColumn);
                if (isMusic != 0){
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    String album = cursor.getString(albumColumn);
                    String path = cursor.getString(pathColumn);
                    int year = cursor.getInt(yearColumn);
                    int id = cursor.getInt(idColumn);

                    Track track = new Track(path);

                    track.setTitle(title);
                    track.setArtist(artist);
                    track.setAlbum(album);
                    track.setYear(year);

                    int trackIndex = mainPlaylist.indexOf(track);

                    if (trackIndex < 0){
                        // get genres
                        Uri uri = MediaStore.Audio.Genres.getContentUriForAudioId("external", id);
                        Cursor genresCursor = getContentResolver().query(uri, genresProjection, null, null, null);
                        if (genresCursor != null){
                            int genresColumnIndex = genresCursor.getColumnIndex(MediaStore.Audio.Genres.NAME);
                            int index = 0;

                            String [] genreNames = new String[genresCursor.getCount()];
                            while (genresCursor.moveToNext()) {
                                String genre = genresCursor.getString(genresColumnIndex);
                                genreNames[index] = genre;
                                index++;
                            }
                            track.setGenres(genreNames);

                            genresCursor.close();
                        }

                        addTrackToPlayer(track);
                    }

                    // set track's flag to true
                    track.setFlag(true);
                    // found one! increment !!
                    tracksFound++;
                }
                // delete all tracks which have not been found in device's database
                if (tracksFound < mainPlaylist.size()){
                    for (int i = 0; i < mainPlaylist.size(); i++){
                        boolean found = mainPlaylist.getFlag(i);
                        if (!found){
                            mainPlaylist.removeTrack(i--);
                        } else {
                            // set flag to false for future checks
                            mainPlaylist.setFlag(i, false);
                        }
                    }
                }
            }

            cursor.close();
        }
    }

    private final BroadcastReceiver renamePlaylistReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int index = intent.getIntExtra(Extras.INDEX_EXTRA, -1);
            final String title = intent.getStringExtra(Extras.NAME_EXTRA);

            if (index > -1){
                AbstractPlaylist playlist = playlists.get(index);
                playlist.setTitle(title);
                sendPlaylistRenamedMessage(index, title);
            }
        }
    };

    private void sendPlaylistRenamedMessage(int playlistIndex, String title){
        AbstractPlaylist playlist = playlists.get(playlistIndex);

        Intent message = new Intent(Messages.PLAYLIST_RENAMED_MESSAGE);
        message.putExtra(Extras.INDEX_EXTRA, playlistIndex);
        message.putExtra(Extras.NAME_EXTRA, title);
        message.putExtra(Extras.SELECTED_RENAMED_EXTRA, playlist == selectedPlaylist);
        message.putExtra(Extras.CURRENT_RENAMED_EXTRA, playlist == currentPlaylist);
        LocalBroadcastManager.getInstance(this).sendBroadcast(message);
    }

    private void notifyPlaylistSizeChanged(AbstractPlaylist playlist){
        Intent message = new Intent(Messages.PLAYLIST_SIZE_CHANGE_MESSAGE);
        message.putExtra(Extras.SIZE_CHANGED_EXTRA, true);
        message.putExtra(Extras.SIZE_EXTRA, playlist.size());
        message.putExtra(Extras.CURRENT_INDEX_EXTRA, playlist.getLastAccessedTrackIndex());
        LocalBroadcastManager.getInstance(PlayerService.this).sendBroadcast(message);
    }

    private void sendShuffleSettings(AbstractPlaylist playlist){
        AbstractPlaylist.ShuffleSettings settings = playlist.getSettings();
        Intent shuffleSettings = new Intent(Messages.SHUFFLE_SETTINGS_MESSAGE);
        shuffleSettings.putExtra(Extras.SETTINGS_EXTRA, settings);
        LocalBroadcastManager.getInstance(this).sendBroadcast(shuffleSettings);
    }

    private void setSelectedPlaylist(AbstractPlaylist playlist){
        if (selectedPlaylist != null){
            selectedPlaylist.setStateChangeListener(null);
        }
        selectedPlaylist = playlist;
        playlist.setStateChangeListener(stateChangeListener);

        sendPlaylistData(currentPlaylist, selectedPlaylist);
    }

    private void setCurrentPlaylist(AbstractPlaylist playlist){
        if (currentPlaylist != null){
            currentPlaylist.setSizeChangeListener(null);
        }
        currentPlaylist = playlist;
        playlist.setSizeChangeListener(sizeChangeListener);
    }

    private void addTrackToPlayer(Track track){
        // adding track to the main playlist
        mainPlaylist.addTrack(track);

        // adding track to an album
        if (track.hasAlbum()){
            findAlbum(track.getAlbum(), track.getArtist()).addTrack(track);
        }
        // adding track to its artist
        if (track.hasArtist()){
            findArtist(track.getArtist()).addTrack(track);
        }
        // adding track to its genres
        if (track.hasGenres()){
            for (String genre : track.getGenres()){
                findGenre(genre).addTrack(track);
            }
        }
    }

    private void sendAlbumCover(Track track){
        Intent intent = new Intent(Messages.ALBUM_COVER_CHANGED_MESSAGE);

        boolean empty = track == null;

        intent.putExtra(Extras.ALBUM_COVER_FLAG_EXTRA, empty);
        if (!empty){
            intent.putExtra(Extras.TRACK_PATH_EXTRA, track.getPath());

        }
        LocalBroadcastManager.getInstance(PlayerService.this).sendBroadcast(intent);
    }

    public Album findAlbum(String title, String artist){
        for (Album album : albums){
            if (album.getTitle().equals(title) && album.getArtist().equals(artist)){
                return album;
            }
        }
        Album album = new Album(title, artist);
        albums.add(album);
        return album;
    }

    public Artist findArtist(String artistName){
        for (Artist artist : artists){
            if (artist.getTitle().equals(artistName)){
                return artist;
            }
        }
        Artist artistPlaylist = new Artist(artistName);
        artists.add(artistPlaylist);
        return artistPlaylist;
    }

    public Genre findGenre(String genreTitle){
        for (Genre genrePlaylist : genres){
            if (genrePlaylist.getTitle().equals(genreTitle)){
                return genrePlaylist;
            }
        }
        Genre genrePlaylist = new Genre(genreTitle);
        genres.add(genrePlaylist);
        return genrePlaylist;
    }
}
