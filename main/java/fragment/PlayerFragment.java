package fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.example.prett.myapplication.R;

import broadcast.Extras;
import broadcast.Messages;
import playlist.TrackParcel;

public class PlayerFragment extends Fragment{

    private static final String COLON = ":";
    private static final String DASH = " — ";

    private boolean playing;

    private int state;

    static final int COVER_STATE = 0;
    static final int EFFECT_STATE = 1;

    private FragmentManager fragmentManager;

    private final BroadcastReceiver stateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int s = intent.getIntExtra(Extras.STATE_EXTRA, -1);
            if (s > -1){
                setState(s);
            }
        }
    };

    private final BroadcastReceiver seekBarPosReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int pos = intent.getIntExtra(Extras.POSITION_EXTRA, 0);
            currentTimeText.setText(formatTime(pos));
            if (!userChange){
                seekBar.setProgress(pos);
            }
        }
    };

    private final BroadcastReceiver playerStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean musicIsPlaying = intent.getBooleanExtra(Extras.MUSIC_IS_PLAYING_EXTRA, false);
            setPlayButtonState(musicIsPlaying);
        }
    };

    private final BroadcastReceiver newTrackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            boolean isEmpty = intent.getBooleanExtra(Extras.EMPTY_EXTRA, true);

            Log.e("PlayerFragment", "onReceive: newTrack = " + isEmpty);

            if (!isEmpty){
                final TrackParcel track = intent.getParcelableExtra(Extras.TRACK_EXTRA);
                final boolean musicIsPlaying = intent.getBooleanExtra(Extras.MUSIC_IS_PLAYING_EXTRA, false);

                final String artist = track.getArtist() + DASH + track.getAlbum();
                artistText.setText(artist);

                titleText.setText(track.getTitle());

                final int currentIndex = intent.getIntExtra(Extras.CURRENT_INDEX_EXTRA, 0);
                final int size = intent.getIntExtra(Extras.SIZE_EXTRA, 0);

                setPlayButtonState(musicIsPlaying);

                setTrackCount(currentIndex, size);

                final int seekBarMax = intent.getIntExtra(Extras.MAX_POSITION_EXTRA, 0);
                final boolean looping = intent.getBooleanExtra(Extras.LOOPING_EXTRA, false);

                maxTimeText.setText(formatTime(seekBarMax));
                seekBar.setMax(seekBarMax);
                loopSwitch.setChecked(looping);
            } else {
                trackCountText.setText(R.string.zero_out_of_zero_text);
                playButton.setImageResource(android.R.drawable.ic_media_play);
                titleText.setText(R.string.generic_title);
                artistText.setText(R.string.generic_artist);
                seekBar.setMax(0);
            }

            currentTimeText.setText(R.string.zero_value);
            seekBar.setProgress(0);
        }
    };

    private final BroadcastReceiver playlistSizeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean sizeChanged = intent.getBooleanExtra(Extras.SIZE_CHANGED_EXTRA, false);
            if (sizeChanged){

                final int size = intent.getIntExtra(Extras.SIZE_EXTRA, 0);
                final int currentIndex = intent.getIntExtra(Extras.CURRENT_INDEX_EXTRA, 0);

                setTrackCount(currentIndex, size);
            }
        }
    };

    private final View.OnClickListener playOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            togglePlayButton();
        }
    };

    final View.OnClickListener prevTrackOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            previousTrack();
        }
    };

    private final CompoundButton.OnCheckedChangeListener switchOnChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            Intent intent = new Intent(Messages.SET_LOOPING_MESSAGE);
            intent.putExtra(Extras.LOOPING_EXTRA, b);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
        }
    };

    private final View.OnClickListener nextTrackOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            nextTrack();
        }
    };

    // UI ELEMENTS //
    private ImageButton playButton;
    private ImageButton nextTrackButton;
    private ImageButton previousTrackButton;

    SeekBar seekBar;
    final SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            userChange = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int currentPosition = seekBar.getProgress();

            Intent message = new Intent(Messages.SEEK_BAR_USER_CHANGE_MESSAGE);
            message.putExtra(Extras.USER_CHANGE_POS_EXTRA, currentPosition);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(message);

            userChange = false;
        }
    };

    TextView currentTimeText;
    TextView maxTimeText;
    TextView artistText;
    TextView trackCountText;
    TextView titleText;

    Switch loopSwitch;

    public void setState(int i){
        state = i;
        switch (i) {
            case EFFECT_STATE:
                fragmentManager.beginTransaction().replace(R.id.fragmentContainer, new EffectFragment()).commit();
                break;
            default:
                fragmentManager.beginTransaction().add(R.id.fragmentContainer, new AlbumCoverFragment()).commit();
                break;
        }
    }

    public int getState(){
        return state;
    }

    // BOOLEAN //
    boolean userChange;

    private static String formatTime(int millis){
        String delimiter = COLON;
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        if (seconds < 10){
            delimiter = delimiter + "0";
        }
        return Integer.toString(minutes) + delimiter + Integer.toString(seconds);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_main, container, false);

        // declaring all the UI elements
        playButton = rootView.findViewById(R.id.playButton);
        nextTrackButton = rootView.findViewById(R.id.nextTrackButton);
        previousTrackButton = rootView.findViewById(R.id.previousTrackButton);
        trackCountText = rootView.findViewById(R.id.trackCountText);
        currentTimeText = rootView.findViewById(R.id.currentTimeText);
        maxTimeText = rootView.findViewById(R.id.maxTimeText);
        loopSwitch = rootView.findViewById(R.id.loopSwitch);

        fragmentManager = getFragmentManager();
        setState(COVER_STATE);

        // INITIALIZING SEEK BAR'S FUNCTIONALITY //
        seekBar = rootView.findViewById(R.id.seekBar);
        userChange = false;

        // setting listeners to UI
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        nextTrackButton.setOnClickListener(nextTrackOnClickListener);
        playButton.setOnClickListener(playOnClickListener);
        previousTrackButton.setOnClickListener(prevTrackOnClickListener);
        loopSwitch.setOnCheckedChangeListener(switchOnChangeListener);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(stateChangedReceiver, new IntentFilter(Messages.STATE_CHANGED_MESSAGE));
        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(seekBarPosReceiver, new IntentFilter(Messages.SEEK_BAR_POS_MESSAGE));
        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(newTrackReceiver, new IntentFilter(Messages.NEW_TRACK_MESSAGE));
        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(playlistSizeChangeReceiver, new IntentFilter(Messages.PLAYLIST_SIZE_CHANGE_MESSAGE));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(playerStateChangedReceiver, new IntentFilter(Messages.PLAYER_STATE_CHANGED_MESSAGE));

        artistText = rootView.findViewById(R.id.trackArtistText);
        artistText.setSingleLine(true);
        artistText.setSelected(true);
        artistText.setEllipsize(TextUtils.TruncateAt.MARQUEE);

        titleText = rootView.findViewById(R.id.titleText);
        titleText.setSingleLine(true);
        titleText.setSelected(true);
        titleText.setEllipsize(TextUtils.TruncateAt.MARQUEE);

        setPlayButtonState(false);

        requestTrackData();

        return rootView;
    }

    private void requestTrackData(){
        Intent intent = new Intent(Messages.TRACK_DATA_REQUEST_MESSAGE);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        // removing receivers
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(stateChangedReceiver);
        LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(seekBarPosReceiver);
        LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(newTrackReceiver);
        LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(playlistSizeChangeReceiver);
        // removing listeners
        seekBar.setOnSeekBarChangeListener(null);
        nextTrackButton.setOnClickListener(null);
        playButton.setOnClickListener(null);
        previousTrackButton.setOnClickListener(null);
        super.onDestroy();
    }

    private void setTrackCount(int currentIndex, int size){
        if (size > 0){
            String text = Integer.toString(currentIndex + 1) + "/" + Integer.toString(size);
            trackCountText.setText(text);
        } else {
            trackCountText.setText(R.string.zero_out_of_zero_text);
        }
    }

    private void setPlayButtonState(boolean isPlaying){
        playing = isPlaying;

        if (playing){
            playButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void togglePlayButton(){
        playing = !playing;

        Intent message = new Intent(Messages.BUTTON_CLICKED_MESSAGE);

        if (playing){
            playButton.setImageResource(android.R.drawable.ic_media_pause);
            message.putExtra(Extras.BUTTON_CLICKED_EXTRA, Extras.PLAY_CLICKED);
        } else {
            playButton.setImageResource(android.R.drawable.ic_media_play);
            message.putExtra(Extras.BUTTON_CLICKED_EXTRA, Extras.PAUSE_CLICKED);
        }

        LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(message);
    }

    public void nextTrack(){
        // command the player service to switch to next track
        Intent message = new Intent(Messages.BUTTON_CLICKED_MESSAGE);
        message.putExtra(Extras.BUTTON_CLICKED_EXTRA, Extras.NEXT_CLICKED);
        LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(message);
    }

    public void previousTrack(){
        // command the player service to switch to previous track
        Intent message = new Intent(Messages.BUTTON_CLICKED_MESSAGE);
        message.putExtra(Extras.BUTTON_CLICKED_EXTRA, Extras.PREV_CLICKED);
        LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(message);
    }
}