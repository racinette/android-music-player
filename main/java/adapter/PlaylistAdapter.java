package adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.prett.myapplication.R;

import broadcast.Extras;
import broadcast.Messages;
import playlist.PlaylistParcel;
import playlist.TrackParcel;

public class PlaylistAdapter extends ArrayAdapter {

    private int [] colors;

    private PlaylistParcel playlist;
    private Context context;
    private boolean checkBoxVisible;
    private int screenWidthPx;

    public PlaylistAdapter(Context _context, PlaylistParcel _playlist){
        super(_context, R.layout.track_item);
        playlist = _playlist;
        context = _context;
        checkBoxVisible = false;
        screenWidthPx = Resources.getSystem().getDisplayMetrics().widthPixels;

        final int color1 = context.getResources().getColor(R.color.colorPreset1);
        final int color2 = context.getResources().getColor(R.color.colorPreset2);
        final int color3 = context.getResources().getColor(R.color.colorPreset3);

        colors = new int[]{color1, color2, color3, color3, color2, color1};

    }

    // this method checks if a given track has been checked by a user
    public boolean isChecked(int index){
        return playlist.isChecked(index);
    }

    public void trackChooserMode(boolean enabled, boolean notify){
        checkBoxVisible = enabled;
        // if the mode was disabled, get rid of all selections
        if (!enabled){
            playlist.clearSelection();
        }

        if (notify) notifyDataSetChanged();
    }

    public void setDataSource(PlaylistParcel playlist){
        this.playlist = playlist;
        playlist.clearSelection();
        notifyDataSetChanged();
    }

    public boolean check(int position){
        return playlist.check(position);
    }

    public void checkAll(){
        for (int i = 0; i < playlist.size(); i++){
            if (!playlist.isChecked(i)) playlist.check(i);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return playlist.size();
    }

    @Nullable
    @Override
    public TrackParcel getItem(int position) {
        return playlist.getTrack(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.track_item, parent, false);
        }

        TextView trackNumberText = convertView.findViewById(R.id.trackNumberText);

        TrackParcel track = getItem(position);

        String number = Integer.toString(position + 1) + ". ";
        trackNumberText.setText(number);

        CheckBox trackCheckBox = convertView.findViewById(R.id.trackCheckBox);

        final int WIDTH = screenWidthPx - (trackCheckBox.getWidth() + trackNumberText.getWidth());

        TextView trackArtistText = convertView.findViewById(R.id.trackArtistText);
        trackArtistText.setSingleLine(true);
        trackArtistText.setWidth(WIDTH);

        TextView trackTitleText = convertView.findViewById(R.id.trackTitleText);
        trackTitleText.setSingleLine(true);
        trackTitleText.setWidth(WIDTH);

        trackArtistText.setText(track.getArtist());
        trackTitleText.setText(track.getTitle());

        if (!track.hasPreset()){
            convertView.setBackgroundColor(colors[position % colors.length]);
        }

        trackCheckBox.setEnabled(checkBoxVisible);
        if (checkBoxVisible){
            trackCheckBox.setVisibility(View.VISIBLE);
        } else {
            trackCheckBox.setVisibility(View.INVISIBLE);
        }
        trackCheckBox.setChecked(playlist.isChecked(position));

        return convertView;
    }
}
