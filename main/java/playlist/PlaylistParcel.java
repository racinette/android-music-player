package playlist;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/*
    Base class of any playlist, holding all the required information to pass around the app
    it is immutable (except for the name) for classes outside the package
 */

public class PlaylistParcel implements Parcelable{

    private boolean hasPreset;

    private String title;

    private boolean shuffled;
    private boolean editable;

    private ArrayList<TrackParcel> tracks;
    private int[] shuffledIndices;

    private boolean [] checked;
    private int checkedCount;

    private boolean current;

    PlaylistParcel(AbstractPlaylist playlist, boolean isCurrent){
        tracks = new ArrayList<>(playlist.size());
        for (int i = 0; i < playlist.size(); i++){
            tracks.add(new TrackParcel(playlist.getTrackWithoutShuffle(i)));
        }
        this.title = playlist.getTitle();
        this.editable = playlist.isEditable();
        this.shuffled = playlist.isShuffled();

        current = isCurrent;

        hasPreset = playlist.hasPreset();

        checkedCount = 0;
        checked = new boolean[size()];

        if (isShuffled()){
            this.shuffledIndices = playlist.getShuffledIndices();
        }
    }

    public boolean isCurrent(){
        return current;
    }

    public final boolean isEditable() {
        return editable;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle(){
        return title;
    }

    public boolean isShuffled(){
        return shuffled;
    }

    public final int size() {
        return tracks.size();
    }

    public final boolean isEmpty() {
        return size() == 0;
    }

    public TrackParcel getTrack(int number) {
        if (isShuffled()) {
            return tracks.get(shuffledIndices[number]);
        } else {
            return tracks.get(number);
        }
    }

    public boolean check(int position){
        checked[position] = !checked[position];
        if (checked[position]){
            checkedCount++;
        } else {
            checkedCount--;
        }
        return checked[position];
    }

    public boolean hasPreset(){
        return hasPreset;
    }

    public void removeCheckedTracks(){
        final int [] checkedIndices = getCheckedIndices();

        if (checkedCount > 0){
            final int SIZE = tracks.size() - checkedCount;

            final ArrayList<TrackParcel> newTracks = new ArrayList<>(SIZE);

            /*
             * This loop is done in pursuit of getting rid of sorting the newTracks array.
             * If the tracks were added in the shuffled order, it would require sorting afterwards.
             * In this case the original indices are retrieved and the sorting order will be kept.
             */
            if (isShuffled()){
                for (int i = 0; i < checkedIndices.length; i++){
                    checkedIndices[i] = shuffledIndices[checkedIndices[i]];
                }
                Arrays.sort(checkedIndices);
            }

            int j = 0;

            for (int i = 0; i < tracks.size(); i++){
                if (j >= checkedIndices.length || i != checkedIndices[j]){
                    newTracks.add(tracks.get(i));
                } else {
                    j++;
                    Log.e("Track removed: ", tracks.get(i).toString() );
                }
            }

            tracks = newTracks;
            clearSelection();
        }
    }

    public int [] getCheckedIndices(){
        int [] checkedIndices = new int[checkedCount];
        int index = 0;
        for (int i = 0; i < size(); i++){
            if (checked[i]){
                checkedIndices[index++] = i;
            }
        }
        return checkedIndices;
    }

    public void clearSelection(){
        checkedCount = 0;
        checked = new boolean[tracks.size()];
    }

    // this method checks if a given track has been checked by a user
    public boolean isChecked(int index){
        return checked[index];
    }

    public final void unshuffle(){
        shuffled = false;
    }

    public final boolean shuffle(AbstractPlaylist.ShuffleSettings settings){
        shuffled = true;
        int [] temp = settings.getShuffledIndices();
        if (temp.length == size()){
            shuffledIndices = temp;
            return true;
        }
        return false;
    }

    public void setCurrent(boolean current){
        this.current = current;
    }

    // PARCELABLE

    private PlaylistParcel(Parcel in){
        title = in.readString();

        boolean data[] = new boolean[3];
        in.readBooleanArray(data);
        editable = data[0];
        shuffled = data[1];
        current = data[2];
        hasPreset = data[3];

        int size = in.readInt();
        tracks = new ArrayList<>(size);

        in.readTypedList(tracks, TrackParcel.CREATOR);

        if (isShuffled()) {
            shuffledIndices = new int[size];
            in.readIntArray(shuffledIndices);
        }
    }

    public int getCheckedCount(){
        return checkedCount;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(title);
        parcel.writeBooleanArray(new boolean[]{isEditable(), isShuffled(), isCurrent(), hasPreset()});
        parcel.writeInt(size());
        parcel.writeTypedList(tracks);

        // if the playlist is not shuffled, there is no reason to read/write
        // the shuffled indices array
        // why use space for something redundant?
        if (isShuffled()) {
            parcel.writeIntArray(shuffledIndices);
        }
    }

    public static final Parcelable.Creator<PlaylistParcel> CREATOR
            = new Parcelable.Creator<PlaylistParcel>() {
        public PlaylistParcel createFromParcel(Parcel in) {
            return new PlaylistParcel(in);
        }

        public PlaylistParcel[] newArray(int size) {
            return new PlaylistParcel[size];
        }
    };
}
