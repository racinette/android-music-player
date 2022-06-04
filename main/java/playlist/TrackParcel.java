package playlist;

import android.os.Parcel;
import android.os.Parcelable;

public class TrackParcel implements Parcelable {
    private String album;
    private String artist;
    private String title;
    private boolean hasPreset;

    TrackParcel(Track track){
        album = track.getAlbum();
        artist = track.getArtist();
        title = track.getTitle();
        hasPreset = track.hasPreset();
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public boolean hasPreset(){
        return hasPreset;
    }

    @Override
    public String toString(){
        return getArtist() + " - " + getTitle();
    }

    // PARCELABLE METHODS
    private TrackParcel(Parcel in){
        artist = in.readString();
        title = in.readString();
        album = in.readString();
        hasPreset = in.readByte() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(artist);
        parcel.writeString(title);
        parcel.writeString(album);
        parcel.writeByte(hasPreset ? (byte) 1 : 0);
    }

    public static final Parcelable.Creator<TrackParcel> CREATOR
            = new Parcelable.Creator<TrackParcel>() {
        public TrackParcel createFromParcel(Parcel in) {
            return new TrackParcel(in);
        }

        public TrackParcel[] newArray(int size) {
            return new TrackParcel[size];
        }
    };
}
