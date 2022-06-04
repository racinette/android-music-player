package activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import com.example.prett.myapplication.R;

import java.io.File;
import java.util.ArrayList;

import adapter.DirectoryAdapter;
import playlist.Track;

public class DirActivity extends AppCompatActivity {

    ListView dirListView;

    Button addDirButton;
    Button removeDirButton;

    DirectoryAdapter adapter;

    ArrayList<File> dirList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dir);

        dirListView = (ListView) findViewById(R.id.dirListView);

        dirListView.setEmptyView(findViewById(R.id.emptyView));

        adapter = new DirectoryAdapter(this, dirList);
        dirListView.setAdapter(adapter);
        dirListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                CheckBox checkBox = view.findViewById(R.id.selectCheckBox);
                checkBox.setChecked((adapter.check(i)));
            }
        });

        addDirButton = (Button) findViewById(R.id.addDirButton);
        addDirButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DirActivity.this, DirectoryChooser.class);
                startActivity(intent);
                finish();
            }
        });
        removeDirButton = (Button) findViewById(R.id.removeDirButton);
        removeDirButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean selected [] = adapter.getCheckedPos();
                int offset = 0;
                for (int i = 0; i < selected.length; i++){
                    if (selected[i]){
                        //removeDirectory(i - (offset++));
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    public void addDirectory(File directory){
        dirList.add(directory);
        recursiveSearch(directory);
    }

    private void recursiveSearch(File directory){
        File [] files = directory.listFiles();
        for (int i = 0; i < files.length; i++){
            String path = files[i].getAbsolutePath();
            if (path.endsWith(".mp3") || path.endsWith(".flac")){
                addTrackToPlayer(new Track(path));
            } else if (files[i].isDirectory()){
                recursiveSearch(files[i]);
            }
        }
    }

    public void addTrackToPlayer(Track track){
        /*
        // adding track to the main playlist
        MainPlaylist.get().addTrack(track);

        // adding track to an album
        if (!track.albumIsUnknown()){
            // tries to find the album
            Album album = MenuFragment.findAlbum(track.getAlbum());
            // if it doesn't find it, it creates new one
            if (album == null){
                album = new Album(track.getAlbum(), track.getArtist());
                MenuFragment.addAlbum(album);
            }
            album.addTrack(track);
        }
        */
    }

    public void addDirectory(File directory, ArrayList<File> ignore){
        dirList.add(directory);
        recursiveSearch(directory, ignore);
    }

    private void recursiveSearch(File directory, ArrayList<File> ignore){
        File [] files = directory.listFiles();
        for (int i = 0; i < files.length; i++){
            String path = files[i].getAbsolutePath();
            if (path.endsWith(".mp3") || path.endsWith(".flac")){
                addTrackToPlayer(new Track(path));
            } else if (files[i].isDirectory() && !ignore.contains(files[i])){
                recursiveSearch(files[i], ignore);
            }
        }
    }

    /*
    private void removeDirectory(int index){
        // removes the directory from dirList and gets its path
        String pathPrefix = dirList.remove(index).getAbsolutePath();
        // saves reference to the main playlist to iterate through it later on
        AbstractPlaylist main = Data.getInstance().getMainPlaylist();
        // iterate through the main playlist and find every track
        // whose path starts with the removed directory
        // and remove them from all playlists
        boolean playbackStopped = false;
        int offset = 0;
        for (int i = 0; i - offset < main.size(); i++){
            Track track = main.getTrack(i - offset);
            if (track.getPath().startsWith(pathPrefix)){
                // if a track which is currently playing has been removed
                // stop playback
                if (!playbackStopped && Data.getInstance().getCurrentTrack() != null && track.equals(Data.getInstance().getCurrentTrack())){
                    // stop playback
                    Intent message = new Intent(Messages.TRACK_FROM_PLAYLIST_MESSAGE);
                    message.putExtra("track_index", -1);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(message);
                    playbackStopped = true;
                }
                main.removeTrack(i - offset);
                track.remove();
                offset++;
            }
        }
    }
    */
}
