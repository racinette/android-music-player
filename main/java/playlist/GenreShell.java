package playlist;

import android.os.Parcel;
import android.os.Parcelable;

public class GenreShell extends PlaylistShell {
    public GenreShell(Genre genre){
        super(genre);
    }

    /*              PARCELABLE               */

    GenreShell(Parcel in){
        super(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
    }

    public static final Parcelable.Creator<GenreShell> CREATOR
            = new Parcelable.Creator<GenreShell>() {
        public GenreShell createFromParcel(Parcel in) {
            return new GenreShell(in);
        }

        public GenreShell[] newArray(int size) {
            return new GenreShell[size];
        }
    };
}
