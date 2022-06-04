package adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.prett.myapplication.R;

import java.util.ArrayList;

import playlist.AbstractPlaylist;
import playlist.PlaylistShell;

public class PlaylistListAdapter extends ArrayAdapter{
    private Context context;
    private PlaylistShell [] data;

    public PlaylistListAdapter(Context _context, PlaylistShell[] data){
        super(_context, R.layout.playlist_item, data);
        context = _context;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.length;
    }

    @Nullable
    @Override
    public PlaylistShell getItem(int position) {
        return data[position];
    }

    public void setData(PlaylistShell [] data){
        this.data = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.playlist_item, parent, false);
        }
        TextView playlistTitleText = convertView.findViewById(R.id.selectedPlaylistTitle);
        playlistTitleText.setText(getItem(position).getTitle());
        return convertView;
    }
}
