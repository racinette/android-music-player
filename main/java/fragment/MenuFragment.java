package fragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.prett.myapplication.R;

import adapter.AlbumAdapter;
import adapter.ArtistAdapter;
import adapter.GenreAdapter;
import adapter.PlaylistListAdapter;
import alertdialog.AddPlaylistAlertDialog;
import broadcast.Extras;
import broadcast.Messages;
import helper.Methods;
import playlist.AbstractPlaylist;
import playlist.AlbumShell;
import playlist.ArtistShell;
import playlist.GenreShell;
import playlist.PlaylistShell;

public class MenuFragment extends Fragment {

    private final String SELECTED_TAB_KEY = "selected_tab";

    private int selectedTab;
    private final int PLAYLIST = 0;
    private final int ALBUM = 1;
    private final int ARTIST = 2;
    private final int GENRE = 3;

    private PlaylistShell [] playlists;
    private AlbumShell [] albums;
    private ArtistShell [] artists;
    private GenreShell [] genres;

    private final BroadcastReceiver newPlaylistReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PlaylistShell playlist = intent.getParcelableExtra(Extras.ADDED_PLAYLIST_EXTRA);
            if (playlist != null){
                addPlaylist(playlist);
            }
        }
    };

    private ListView playlistsListView;

    private AlbumAdapter albumAdapter;
    private ArtistAdapter artistAdapter;
    private GenreAdapter genreAdapter;
    private PlaylistListAdapter playlistListAdapter;

    private final AdapterView.OnItemLongClickListener playlistOnItemLongClick = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int itemLongClickedIndex, long l) {
            if (playlists[itemLongClickedIndex].isEditable()){
                PopupMenu popupMenu = new PopupMenu(getActivity(), view);
                popupMenu.inflate(R.menu.playlist_popup);

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        int id = menuItem.getItemId();
                        if (!getActivity().isFinishing()){
                            switch (id){
                                // if user chose to delete the playlist
                                case R.id.deletePlaylistItem :
                                    buildDeletePlaylistDialog(itemLongClickedIndex, getActivity()).show();
                                    break;
                                // if the user chose to rename the playlist
                                case R.id.renamePlaylistItem :
                                    buildRenamePlaylistDialog(itemLongClickedIndex, getActivity()).show();
                                    break;
                            }
                        }

                        return true;
                    }
                });
                popupMenu.show();
            }
            return true;
        }
    };

    private AlertDialog.Builder buildDeletePlaylistDialog(final int index, final Context context){
        // create an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_playlist_dialog_title);
        builder.setMessage(R.string.delete_playlist_dialog_text);
        // user agreed to delete the playlist
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                removePlaylist(index);
                sendDeletePlaylistMessage(index);
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        return builder;
    }

    private void sendDeletePlaylistMessage(final int index){
        final Intent message = new Intent(Messages.DELETE_PLAYLIST_MESSAGE);
        message.putExtra(Extras.INDEX_EXTRA, index);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(message);
    }

    private void removePlaylist(final int index){
        final PlaylistShell [] temp = new PlaylistShell[playlists.length - 1];
        System.arraycopy(playlists, 0, temp, 0, index);
        System.arraycopy(playlists, index + 1, temp, index, playlists.length - index - 1);
        playlists = temp;
        playlistListAdapter.setData(temp);
    }

    private AlertDialog.Builder buildRenamePlaylistDialog(final int index, final Context context){
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        final EditText playlistTitleInput = new EditText(context);
        playlistTitleInput.setHint(R.string.playlist_edittext_hint);
        dialogBuilder.setView(playlistTitleInput);
        dialogBuilder.setTitle(R.string.rename_playlist_dialog_title);
        dialogBuilder.setMessage(R.string.rename_playlist_dialog_text);
        dialogBuilder.setPositiveButton(R.string.rename_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String title = playlistTitleInput.getText().toString().trim();
                title = Methods.formatTitle(title);
                if (title.length() > 0){
                    playlists[index].setTitle(title);
                    sendRenamePlaylistMessage(index, title);
                    playlistListAdapter.notifyDataSetChanged();
                    dialogInterface.cancel();
                } else {
                    Toast.makeText(context, R.string.incorrect_input, Toast.LENGTH_LONG).show();
                }
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        return dialogBuilder;
    }

    private void sendRenamePlaylistMessage(final int index, final String title){
        final Intent message = new Intent(Messages.RENAME_PLAYLIST_MESSAGE);
        message.putExtra(Extras.INDEX_EXTRA, index);
        message.putExtra(Extras.NAME_EXTRA, title);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(message);
    }

    private final AdapterView.OnItemClickListener onPlaylistClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            final int PLAYLIST_FRAGMENT_INDEX = 2;
            sendChangeSelectedPlaylistMessage(selectedTab, i);
            sendJumpToTabMessage(PLAYLIST_FRAGMENT_INDEX);
        }
    };

    private Button addPlaylistButton;

    private void addPlaylist(PlaylistShell playlist){
        PlaylistShell [] temp = new PlaylistShell[playlists.length + 1];

        // arraycopy is faster than manual array copying
        System.arraycopy(playlists, 0, temp, 0, playlists.length);

        temp[temp.length - 1] = playlist;

        playlists = temp;
        playlistListAdapter.setData(temp);
    }

    private final View.OnClickListener addPlaylistOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            AddPlaylistAlertDialog.show(getActivity(), onAddClickListener);
        }
    };

    private AddPlaylistAlertDialog.OnAddClickListener onAddClickListener = new AddPlaylistAlertDialog.OnAddClickListener() {
        @Override
        public void onAdd(String title) {

            addPlaylist(new PlaylistShell(title, true));

            // send a message to the PlayerService to add a new playlist
            Intent addPlaylistMessage = new Intent(Messages.ADD_PLAYLIST_MESSAGE);
            addPlaylistMessage.putExtra(Extras.PLAYLIST_TITLE_EXTRA, title);
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(addPlaylistMessage);
        }
    };

    private TabLayout tabLayout;

    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playlists = (PlaylistShell[]) intent.getParcelableArrayExtra(Extras.PLAYLISTS_EXTRA);
            albums = (AlbumShell[]) intent.getParcelableArrayExtra(Extras.ALBUMS_EXTRA);
            artists = (ArtistShell[]) intent.getParcelableArrayExtra(Extras.ARTISTS_EXTRA);
            genres = (GenreShell[]) intent.getParcelableArrayExtra(Extras.GENRES_EXTRA);

            // preparing adapters for the listview
            playlistListAdapter = new PlaylistListAdapter(getActivity(), playlists);
            albumAdapter = new AlbumAdapter(getActivity(), albums);
            artistAdapter = new ArtistAdapter(getActivity(), artists);
            genreAdapter = new GenreAdapter(getActivity(), genres);

            // select playlists tab at start
            playlistsListView.setOnItemClickListener(onPlaylistClickListener);
            TabLayout.Tab playlistsTab = tabLayout.getTabAt(selectedTab);
            if (playlistsTab != null) {
                playlistsTab.select();
            }
            selectTab(selectedTab);
        }
    };

    private void selectTab(int position){
        switch (position){
            case PLAYLIST:
                selectedTab = PLAYLIST;
                playlistsListView.setAdapter(playlistListAdapter);
                addPlaylistButton.setEnabled(true);
                addPlaylistButton.setVisibility(View.VISIBLE);
                playlistsListView.setOnItemLongClickListener(playlistOnItemLongClick);
                break;

            case ALBUM:
                selectedTab = ALBUM;
                playlistsListView.setAdapter(albumAdapter);
                addPlaylistButton.setEnabled(false);
                addPlaylistButton.setVisibility(View.INVISIBLE);
                playlistsListView.setOnItemLongClickListener(null);
                break;

            case ARTIST:
                selectedTab = ARTIST;
                playlistsListView.setAdapter(artistAdapter);
                addPlaylistButton.setEnabled(false);
                addPlaylistButton.setVisibility(View.INVISIBLE);
                playlistsListView.setOnItemLongClickListener(null);
                break;

            case GENRE:
                selectedTab = GENRE;
                playlistsListView.setAdapter(genreAdapter);
                addPlaylistButton.setEnabled(false);
                addPlaylistButton.setVisibility(View.INVISIBLE);
                playlistsListView.setOnItemLongClickListener(null);
                break;
        }
    }

    private final TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            int position = tab.getPosition();
            selectTab(position);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private void sendJumpToTabMessage(int number){
        Intent message = new Intent(Messages.JUMP_TO_TAB_MESSAGE);
        message.putExtra(Extras.NUMBER_EXTRA, number);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(message);
    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.activity_menu, container, false);

        addPlaylistButton = rootView.findViewById(R.id.addPlaylistButton);
        addPlaylistButton.setOnClickListener(addPlaylistOnClickListener);

        playlistsListView = rootView.findViewById(R.id.playlistsListView);

        // tab layout which provides user with navigation between playlist types
        tabLayout = rootView.findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(onTabSelectedListener);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(dataReceiver, new IntentFilter(Messages.MENU_DATA_MESSAGE));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(newPlaylistReceiver, new IntentFilter(Messages.MENU_FRAGMENT_NEW_PLAYLIST_MESSAGE));

        selectedTab = PLAYLIST;

        requestData();

        return rootView;
    }

    private void requestData(){
        Intent request = new Intent(Messages.MENU_DATA_REQUEST_MESSAGE);
        request.putExtra(Extras.REQUESTED_EXTRA, true);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(request);
    }

    // type is the type of playlist: artist, genre, etc.
    private void sendChangeSelectedPlaylistMessage(int type, int index){
        Intent message = new Intent(Messages.CHANGE_SELECTED_PLAYLIST_MESSAGE);
        message.putExtra(Extras.TYPE_EXTRA, type);
        message.putExtra(Extras.INDEX_EXTRA, index);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(message);
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(dataReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(newPlaylistReceiver);
        super.onDestroyView();
    }

    private void update(){
        playlistListAdapter.notifyDataSetChanged();
    }
}
