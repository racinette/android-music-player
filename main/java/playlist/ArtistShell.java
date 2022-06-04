package playlist;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by prett on 2/21/2018.
 */

public class ArtistShell extends PlaylistShell {
    public ArtistShell(Artist artist){
        super(artist);
    }

    /*              PARCELABLE               */

    ArtistShell(Parcel in){
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

    public static final Parcelable.Creator<ArtistShell> CREATOR
            = new Parcelable.Creator<ArtistShell>() {
        public ArtistShell createFromParcel(Parcel in) {
            return new ArtistShell(in);
        }

        public ArtistShell[] newArray(int size) {
            return new ArtistShell[size];
        }
    };

}
