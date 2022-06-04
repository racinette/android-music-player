package playlist;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by prett on 2/21/2018.
 */

public class PlaylistShell implements Parcelable {

    private String title;
    private boolean editable;

    public PlaylistShell(AbstractPlaylist playlist){
        title = playlist.getTitle();
        editable = playlist.isEditable();
    }

    public PlaylistShell(String title, boolean editable){
        this.title = title;
        this.editable = editable;
    }

    public String getTitle(){
        return title;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public boolean isEditable() {
        return editable;
    }

    /*              PARCELABLE               */

    PlaylistShell(Parcel in){
        title = in.readString();
        editable = in.readInt() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(title);
        parcel.writeInt(editable ? 1 : 0);
    }

    public static final Parcelable.Creator<PlaylistShell> CREATOR
            = new Parcelable.Creator<PlaylistShell>() {
        public PlaylistShell createFromParcel(Parcel in) {
            return new PlaylistShell(in);
        }

        public PlaylistShell[] newArray(int size) {
            return new PlaylistShell[size];
        }
    };
}
