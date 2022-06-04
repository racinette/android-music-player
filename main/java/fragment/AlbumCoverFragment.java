package fragment;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.example.prett.myapplication.R;

import broadcast.Extras;
import broadcast.Messages;
import fragment.PlayerFragment;
import widget.SquareImageView;

/**
 * Created by prett on 12/17/2017.
 */

public class AlbumCoverFragment extends Fragment {

    private final MyGestureDetector myGestureDetector = new MyGestureDetector();
    private final GestureDetector gestureDetector = new GestureDetector(getActivity(), myGestureDetector);

    private SquareImageView albumCover;

    private final BroadcastReceiver newCoverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean empty = intent.getBooleanExtra(Extras.ALBUM_COVER_FLAG_EXTRA, true);
            if (!empty){
                String path = intent.getStringExtra(Extras.TRACK_PATH_EXTRA);

                // retrieving album cover
                MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                metadataRetriever.setDataSource(path);
                byte [] cover = metadataRetriever.getEmbeddedPicture();
                metadataRetriever.release();

                if (cover != null){
                    albumCover.setImageBitmap(BitmapFactory.decodeByteArray(cover, 0, cover.length));
                } else {
                    albumCover.setImageResource(R.color.colorPrimary);
                }

                /*
                final ViewTreeObserver vto = albumCover.getViewTreeObserver();
                vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    public boolean onPreDraw() {
                        Data.getInstance().setLastMeasuredHeight(albumCover.getMeasuredHeight());
                        vto.removeOnPreDrawListener(this);
                        return true;
                    }
                });
                */
            } else {
                albumCover.setImageResource(R.color.colorPrimary);
            }

        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.album_cover_fragment, container, false);

        albumCover = rootView.findViewById(R.id.albumCoverSquareView);

        albumCover.setFocusable(true);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(newCoverReceiver, new IntentFilter(Messages.ALBUM_COVER_CHANGED_MESSAGE));

        requestAlbumCover();

        albumCover.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return gestureDetector.onTouchEvent(motionEvent);
            }
        });

        return rootView;
    }

    private void requestAlbumCover(){
        Intent message = new Intent(Messages.ALBUM_COVER_REQUEST_MESSAGE);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(message);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(newCoverReceiver);

        super.onDestroy();
    }


    private class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_MIN_DISTANCE = 80;
        private static final int SWIPE_MAX_OFF_PATH = 250;
        private static final int SWIPE_THRESHOLD_VELOCITY = 100;
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(e1.getX() - e2.getX()) > SWIPE_MAX_OFF_PATH)
                return false;
            if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
                // fling up
                Intent intent = new Intent(Messages.STATE_CHANGED_MESSAGE);
                intent.putExtra(Extras.STATE_EXTRA, PlayerFragment.EFFECT_STATE);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
            } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY){
                // fling down
                Intent playlistStateMessage = new Intent(Messages.CURRENT_PLAYLIST_SHUFFLE_MESSAGE);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(playlistStateMessage);
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }
}
