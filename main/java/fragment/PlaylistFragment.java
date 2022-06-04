package fragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.prett.myapplication.R;

import adapter.PlaylistAdapter;
import adapter.TitlesAdapter;
import alertdialog.AddPlaylistAlertDialog;
import broadcast.Extras;
import broadcast.Messages;
import playlist.AbstractPlaylist;
import playlist.PlaylistParcel;

public class PlaylistFragment extends Fragment{

    private Button currentPlaylistButton;

    private TextView selectedPlaylistTitle;

    private Button cancelSelectionButton;
    private Button addToButton;
    private Button removeTrackButton;
    private Button deleteTrackButton;
    private Button shuffleButton;

    private TableRow buttonRow;

    private ListView trackListView;
    private PlaylistAdapter playlistAdapter;

    private PlaylistParcel selectedPlaylist;
    //private String currentPlaylistTitle;

    private boolean initialized;

    private SelectOnItemClickListener selectOnItemClickListener;

    private final BroadcastReceiver playlistReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // when the fragment receives the playlist, it must fetch it
            // and initialize itself

            selectedPlaylist = intent.getParcelableExtra(Extras.SELECTED_PLAYLIST_EXTRA);
            final String currentPlaylistTitle = intent.getStringExtra(Extras.CURRENT_PLAYLIST_TITLE_EXTRA);

            // the data was fetched
            // if the fragment had not been initialized previously, do it

            if (!isInitialized()){
                initialize(selectedPlaylist);
            }

            selectPlaylist(selectedPlaylist, currentPlaylistTitle);
        }
    };

    private final View.OnClickListener deleteOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            buildDeleteDialog(getActivity()).show();
        }
    };

    private final BroadcastReceiver shuffleSettingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AbstractPlaylist.ShuffleSettings settings = intent.getParcelableExtra(Extras.SETTINGS_EXTRA);

            shuffleButton.setText(R.string.unshuffle_text);
            selectedPlaylist.shuffle(settings);
            playlistAdapter.notifyDataSetChanged();
        }
    };

    private final View.OnClickListener shuffleOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (selectedPlaylist.isShuffled()){
                shuffleButton.setText(R.string.shuffle_text);
                selectedPlaylist.unshuffle();
                playlistAdapter.notifyDataSetChanged();
                sendPlaylistStateMessage(false);
            } else {
                // the else part of this expression does not do anything to the playlist
                // it sends a message to the PlayerService to shuffle the playlist
                // and waits for its reply, which contains an instance of ShuffleSettings
                sendPlaylistStateMessage(true);
            }
        }
    };

    // REMOVE FUNCTIONALITY

    private final View.OnClickListener removeOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            buildRemoveDialog().show();
        }
    };

    private AlertDialog.Builder buildRemoveDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.remove_tracks_dialog_title);
        builder.setMessage(R.string.remove_tracks_dialog_message);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final int [] removed = selectedPlaylist.getCheckedIndices();

                // notify that tracks are to be removed
                final Intent message = new Intent(Messages.REMOVE_TRACKS_MESSAGE);
                message.putExtra(Extras.INDICES_ARRAY_EXTRA, removed);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(message);
                // remove tracks from playlist
                selectedPlaylist.removeCheckedTracks();
                // cancel selection mode
                cancel(!selectedPlaylist.isShuffled());
                // dismiss dialog
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        return builder;
    }

    // REMOVE FUNCTIONALITY ENDED

    // DELETE FUNCTIONALITY

    private AlertDialog.Builder buildDeleteDialog(final Context context){
        // start building a dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // setting title
        builder.setTitle(R.string.delete_tracks_alert_title);
        // setting message
        builder.setMessage(R.string.delete_tracks_alert_message);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final int [] deleted = selectedPlaylist.getCheckedIndices();

                // remove tracks from playlist
                selectedPlaylist.removeCheckedTracks();
                // notify that tracks are to be removed
                final Intent message = new Intent(Messages.REMOVE_TRACKS_MESSAGE);
                message.putExtra(Extras.INDICES_ARRAY_EXTRA, deleted);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(message);
                // cancel selection mode
                cancel(!selectedPlaylist.isShuffled());
                // dismiss dialog
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        return builder;
    }

    // DELETE FUNCTIONALITY ENDED

    // ADD TO FUNCTIONALITY

    private final View.OnClickListener addToOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            sendPlaylistTitlesRequest();
        }
    };

    private final BroadcastReceiver playlistTitlesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String [] titles = intent.getStringArrayExtra(Extras.PLAYLIST_TITLES_ARRAY_EXTRA);
            buildAddDialog(titles).show();
        }
    };

    private void sendPlaylistTitlesRequest(){
        Intent intent = new Intent(Messages.REQUEST_PLAYLIST_TITLES_MESSAGE);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    private void sendAddTracksMessage(int playlistIndex, int [] selectedTracks){
        Intent message = new Intent(Messages.ADD_TRACKS_MESSAGE);
        message.putExtra(Extras.INDICES_ARRAY_EXTRA, selectedTracks);
        message.putExtra(Extras.PLAYLIST_INDEX_EXTRA, playlistIndex);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(message);
    }

    private AlertDialog.Builder buildAddDialog(String [] playlistTitles){

        final int PADDING_TOP = dpToPx(5);
        final int PADDING_LEFT_RIGHT = dpToPx(15);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.alert_dialog_add_to_message);

        builder.setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        // creating a spinner which will allow to chose a playlist user wants to add tracks to
        final Spinner playlistSpinner = new Spinner(getActivity());
        playlistSpinner.setPadding(PADDING_LEFT_RIGHT, PADDING_TOP, PADDING_LEFT_RIGHT, 0);
        final TitlesAdapter adapter = new TitlesAdapter(playlistTitles, getActivity());
        playlistSpinner.setAdapter(adapter);
        builder.setView(playlistSpinner);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int position = playlistSpinner.getSelectedItemPosition();
                if (position > Spinner.INVALID_POSITION){
                    sendAddTracksMessage(position, selectedPlaylist.getCheckedIndices());
                    cancel(true);
                    dialogInterface.dismiss();
                } else {
                    Toast.makeText(getActivity(), R.string.no_playlist_selected_alert, Toast.LENGTH_LONG).show();
                }
            }
        });

        // create a new playlist and add the tracks to it
        builder.setNeutralButton(R.string.new_playlist_alert_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                AddPlaylistAlertDialog.show(getActivity(), new AddPlaylistAlertDialog.OnAddClickListener() {
                    @Override
                    public void onAdd(String title) {
                        // send a message to the PlayerService to add a new playlist
                        Intent addPlaylistMessage = new Intent(Messages.ADD_PLAYLIST_MESSAGE);
                        addPlaylistMessage.putExtra(Extras.PLAYLIST_TITLE_EXTRA, title);
                        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(addPlaylistMessage);

                        sendAddTracksMessage(-1, selectedPlaylist.getCheckedIndices());
                        cancel(true);
                    }
                });
            }
        });

        return builder;
    }

    // ADD TO FUNCTIONALITY ENDED



    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean needsToUpdate = intent.getBooleanExtra(Extras.UPDATE_PLAYLIST_EXTRA, false);
            if (needsToUpdate){
                demandPlaylistData();
                if (selectedPlaylist.isShuffled()){
                    shuffleButton.setText(R.string.unshuffle_text);
                } else {
                    shuffleButton.setText(R.string.shuffle_text);
                }
            }
        }
    };

    private final BroadcastReceiver playlistRenamedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final boolean currentRenamed = intent.getBooleanExtra(Extras.CURRENT_RENAMED_EXTRA, false);
            final boolean selectedRenamed = intent.getBooleanExtra(Extras.SELECTED_RENAMED_EXTRA, false);
            final String title = intent.getStringExtra(Extras.NAME_EXTRA);

            if (currentRenamed){
                currentPlaylistButton.setText(title);
            }

            if (selectedRenamed){
                selectedPlaylist.setTitle(title);
                selectedPlaylistTitle.setText(title);
            }
        }
    };

    private final AdapterView.OnItemLongClickListener selectAllOnLongClick = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
            playlistAdapter.checkAll();

            addToButton.setEnabled(true);
            if (selectedPlaylist.isEditable()){
                removeTrackButton.setEnabled(true);
            }
            deleteTrackButton.setEnabled(true);

            return true;
        }
    };

    private final AdapterView.OnItemClickListener tracklistOnClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Intent message = new Intent(Messages.TRACK_FROM_PLAYLIST_MESSAGE);
            if (!selectedPlaylist.isCurrent()){
                selectedPlaylist.setCurrent(true);
                currentPlaylistButton.setVisibility(View.INVISIBLE);
                currentPlaylistButton.setEnabled(false);
                message.putExtra(Extras.PLAYLIST_CHANGE_FLAG_EXTRA, true);
            } else {
                message.putExtra(Extras.PLAYLIST_CHANGE_FLAG_EXTRA, false);
            }
            message.putExtra(Extras.TRACK_INDEX_EXTRA, i);
            message.putExtra(Extras.START_PLAYING_MUSIC_EXTRA, true);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(message);
        }
    };

    private final View.OnClickListener backButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            sendPlaylistChangeMessage();
        }
    };

    private final AdapterView.OnItemLongClickListener trackOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
            CheckBox checkBox = view.findViewById(R.id.trackCheckBox);
            checkBox.setChecked(playlistAdapter.check(i));
            playlistAdapter.trackChooserMode(true, true);
            adapterView.setOnItemLongClickListener(selectAllOnLongClick);

            selectOnItemClickListener = new SelectOnItemClickListener();
            trackListView.setOnItemClickListener(selectOnItemClickListener);

            showButtons();

            return true;
        }
    };

    private final View.OnClickListener cancelButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            cancel(true);
        }
    };



    // METHODS START HERE !!!

    private void cancel(boolean notify){
        playlistAdapter.trackChooserMode(false, notify);
        trackListView.setOnItemClickListener(tracklistOnClickListener);
        trackListView.setOnItemLongClickListener(trackOnItemLongClickListener);
        hideButtons();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlist, container, false);

        initialized = false;

        cancelSelectionButton = rootView.findViewById(R.id.cancelSelectionButton);

        addToButton = rootView.findViewById(R.id.addToButton);

        removeTrackButton = rootView.findViewById(R.id.removeTrackButton);

        deleteTrackButton = rootView.findViewById(R.id.deleteTrackButton);

        shuffleButton = rootView.findViewById(R.id.shuffleButton);

        buttonRow = rootView.findViewById(R.id.controlButtonsRow);

        selectedPlaylistTitle = rootView.findViewById(R.id.selectedPlaylistTitle);
        currentPlaylistButton = rootView.findViewById(R.id.currentPlaylistButton);

        trackListView = rootView.findViewById(R.id.trackListView);
        trackListView.setEmptyView(rootView.findViewById(R.id.emptyView));

        // register receiver of playlist data
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(playlistReceiver, new IntentFilter(Messages.PLAYLIST_DATA_MESSAGE));
        // request playlist data which the previously registered receiver will fetch
        demandPlaylistData();

        return rootView;
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public void initialize(final PlaylistParcel playlist){
        initialized = true;

        playlistAdapter = new PlaylistAdapter(getActivity(), playlist);
        trackListView.setAdapter(playlistAdapter);

        // set on click listeners
        cancelSelectionButton.setOnClickListener(cancelButtonOnClickListener);
        removeTrackButton.setOnClickListener(removeOnClickListener);
        addToButton.setOnClickListener(addToOnClickListener);
        deleteTrackButton.setOnClickListener(deleteOnClickListener);
        shuffleButton.setOnClickListener(shuffleOnClickListener);
        currentPlaylistButton.setOnClickListener(backButtonOnClickListener);
        trackListView.setOnItemClickListener(tracklistOnClickListener);
        trackListView.setOnItemLongClickListener(trackOnItemLongClickListener);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(shuffleSettingsReceiver, new IntentFilter(Messages.SHUFFLE_SETTINGS_MESSAGE));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(updateReceiver, new IntentFilter(Messages.UPDATE_PLAYLIST_MESSAGE));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(playlistRenamedReceiver, new IntentFilter(Messages.PLAYLIST_RENAMED_MESSAGE));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(playlistTitlesReceiver, new IntentFilter(Messages.PLAYLIST_FRAGMENT_TITLES_MESSAGE));

        hideButtons();
    }

    public void demandPlaylistData(){
        Intent demand = new Intent(Messages.PLAYLIST_DEMAND_MESSAGE);
        demand.putExtra(Extras.PLAYLIST_DEMAND_FLAG_EXTRA, true);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(demand);
    }

    private void selectPlaylist(PlaylistParcel playlist, String currentPlaylistTitle){
        selectedPlaylist = playlist;

        selectedPlaylistTitle.setText(selectedPlaylist.getTitle());

        playlistAdapter.setDataSource(selectedPlaylist);

        if (selectedPlaylist.isShuffled()){
            shuffleButton.setText(R.string.unshuffle_text);
        } else {
            shuffleButton.setText(R.string.shuffle_text);
        }

        if (selectedPlaylist.isCurrent()){
            currentPlaylistButton.setVisibility(View.INVISIBLE);
            currentPlaylistButton.setEnabled(false);
        } else {
            currentPlaylistButton.setVisibility(View.VISIBLE);
            currentPlaylistButton.setEnabled(true);
            currentPlaylistButton.setText(currentPlaylistTitle);
        }
    }

    private void sendPlaylistChangeMessage(){
        // this methods sends a message that selected playlist has become current
        Intent message = new Intent(Messages.CHANGE_PLAYLIST_MESSAGE);
        message.putExtra(Extras.PLAYLIST_CHANGE_FLAG_EXTRA, true);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(message);
    }

    private void hideButtons(){
        cancelSelectionButton.setEnabled(false);
        cancelSelectionButton.setVisibility(View.INVISIBLE);

        addToButton.setEnabled(false);
        addToButton.setVisibility(View.INVISIBLE);

        removeTrackButton.setEnabled(false);
        removeTrackButton.setVisibility(View.INVISIBLE);

        deleteTrackButton.setEnabled(false);
        deleteTrackButton.setVisibility(View.INVISIBLE);

        buttonRow.setVisibility(View.GONE);

        shuffleButton.setVisibility(View.VISIBLE);
        shuffleButton.setEnabled(!selectedPlaylist.isEmpty());
    }

    private void showButtons(){
        cancelSelectionButton.setEnabled(true);
        cancelSelectionButton.setVisibility(View.VISIBLE);

        addToButton.setEnabled(true);
        addToButton.setVisibility(View.VISIBLE);

        if (selectedPlaylist.isEditable()){
            removeTrackButton.setEnabled(true);
            removeTrackButton.setVisibility(View.VISIBLE);
        }

        deleteTrackButton.setEnabled(true);
        deleteTrackButton.setVisibility(View.VISIBLE);

        buttonRow.setVisibility(View.VISIBLE);

        shuffleButton.setVisibility(View.GONE);
        shuffleButton.setEnabled(false);
    }

    private boolean isInitialized(){
        return initialized;
    }

    private void sendPlaylistStateMessage(boolean shuffled){
        Intent playlistStateMessage = new Intent(Messages.PLAYLIST_STATE_CHANGED_MESSAGE);
        playlistStateMessage.putExtra(Extras.IS_SHUFFLED_EXTRA, shuffled);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(playlistStateMessage);
    }

    private class SelectOnItemClickListener implements AdapterView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            CheckBox checkBox = view.findViewById(R.id.trackCheckBox);
            boolean checked = playlistAdapter.check(i);
            checkBox.setChecked(checked);

            if (selectedPlaylist.getCheckedCount() > 0){
                addToButton.setEnabled(true);

                if (selectedPlaylist.isEditable()){
                    removeTrackButton.setEnabled(true);
                }

                deleteTrackButton.setEnabled(true);
            } else {
                addToButton.setEnabled(false);

                removeTrackButton.setEnabled(false);

                deleteTrackButton.setEnabled(false);
            }
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(playlistReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(updateReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(shuffleSettingsReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(playlistRenamedReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(playlistTitlesReceiver);

        super.onDestroy();
    }
}
