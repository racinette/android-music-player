package playlist;

import android.os.Parcel;
import android.os.Parcelable;

public class AlbumShell extends PlaylistShell{

    private String artist;
    private int imageKey;

    public AlbumShell(Album toParcel){
        super(toParcel);
        artist = toParcel.getArtist();
        imageKey = toParcel.getCoverKey();
    }

    public int getImageKey() {
        return imageKey;
    }

    public String getArtist() {
        return artist;
    }

    /*              PARCELABLE               */

    private AlbumShell(Parcel in){
        super(in);
        artist = in.readString();
        imageKey = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeString(artist);
        parcel.writeInt(imageKey);
    }

    public static final Parcelable.Creator<AlbumShell> CREATOR
            = new Parcelable.Creator<AlbumShell>() {
        public AlbumShell createFromParcel(Parcel in) {
            return new AlbumShell(in);
        }

        public AlbumShell[] newArray(int size) {
            return new AlbumShell[size];
        }
    };
}
