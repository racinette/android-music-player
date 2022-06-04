package playlist;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.util.ArrayList;

import pools.ImagePool;

public class Album extends AbstractPlaylist{
    private static final int NO_COVER = -1;

    private int albumCoverKey;
    private String artist;
    private final int HEIGHT_AND_WIDTH = 70;
    private final double RATIO = 0.5;

    public Album(String _title, String _artist){
        super(_title, false);
        artist = _artist;
        albumCoverKey = NO_COVER;
    }

    public int getCoverKey(){
        return albumCoverKey;
    }

    public String getArtist(){
        return artist;
    }

    @Override
    public void addTrack(Track track) {

        if (albumCoverKey == NO_COVER){
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            try {
                retriever.setDataSource(track.getPath());
                byte [] image = retriever.getEmbeddedPicture();
                if (image != null){
                    Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);

                    int size = (int) (dpToPx(HEIGHT_AND_WIDTH) * RATIO);
                    bitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);

                    albumCoverKey = ImagePool.addImage(bitmap);
                }
            } catch (Exception ex) { ex.printStackTrace(); }

            retriever.release();
        }

        super.addTrack(track);
    }

    public static int dpToPx(int dp){
        return (int)(dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
