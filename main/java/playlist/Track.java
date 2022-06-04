package playlist;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import audioeffect.EffectBundle;

public class Track implements Presetable {
    private static final String UNKNOWN = "<unknown>";
    private static final String UNTITLED = "Untitled";

    private EffectBundle preset;

    private int listens;

    private String album;
    private String artist;
    private String title;

    private Uri uri;
    private String path;
    private String [] genres;
    private int year;
    private boolean flag;

    // arraylist of all playlists, which contain this track
    private final LinkedList<AbstractPlaylist> playlists = new LinkedList<>();

    public Track(String _path){
        super();
        path = _path;
        uri = Uri.fromFile(new File(path));
        flag = false;
        listens = 0;
        preset = EffectBundle.getStandardPreset();

        /*
        // set standard artist and title

        int first_index = path.lastIndexOf('/') + 1;
        int last_index = path.lastIndexOf('.');
        String fileName = path.substring(first_index, last_index);
        // index of -
        int ind = fileName.indexOf('-');
        // if it wasn't found, find another delimiter
        if (ind < 0){
            // index of -- (long -)
            ind = fileName.indexOf('–');
        }
        if (ind >= 0){
            setArtist(fileName.substring(0, ind));
            setTitle(fileName.substring(ind + 2, fileName.length()));
       }
       */
    }

    public void delete(Context context){
        context.getContentResolver().delete(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                MediaStore.MediaColumns.DATA + "='" + getPath() + "'", null
        );
        remove();
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public boolean hasAlbum(){
        return !album.equals(UNKNOWN);
    }

    // increments listens
    public void listened(){
        listens++;
    }

    public int getListens() {
        return listens;
    }

    /*
    public boolean delete(){
        File trackFile = new File(getPath());
        return trackFile.delete();
    }
    */

    // this method makes this track remove itself from every playlist it has been added to
    private void remove(){
        while (!playlists.isEmpty()){
            playlists.pop().removeTrack(this);
        }
    }

    public void toParcel(Intent in, String extraName){
        in.putExtra(extraName, new TrackParcel(this));
    }

    void addedToPlaylist(AbstractPlaylist playlist){
        playlists.add(playlist);
    }

    @Override
    public String toString(){
        return getArtist() + " - " + getTitle();
    }

    void removedFromPlaylist(AbstractPlaylist playlist){
        playlists.remove(playlist);
    }

    public boolean equals(Track track){
        return path.equals(track.getPath());
    }

    @Override
    public EffectBundle getPreset() {
        return preset;
    }

    @Override
    public void setPreset(EffectBundle preset) {
        this.preset = preset;
    }

    @Override
    public boolean hasPreset() {
        return !preset.isStandard() && !preset.isDeleted();
    }

    public String [] getGenres() {
        return genres;
    }

    public boolean hasGenres(){
        return genres.length > 0;
    }

    public void setGenres(String [] genres) {
        this.genres = genres;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setFlag(boolean value){
        flag = value;
    }

    public boolean getFlag(){
        return flag;
    }

    public String getPath() {
        return path;
    }

    public Uri getUri(){
        return uri;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getTitle() {
        return title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean hasArtist(){
        return !artist.equals(UNKNOWN);
    }
}
