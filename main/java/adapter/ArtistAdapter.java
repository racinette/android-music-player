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

import playlist.ArtistShell;

public class ArtistAdapter extends ArrayAdapter {
    private Context context;
    private ArtistShell[] data;

    public ArtistAdapter(Context _context, ArtistShell [] _data){
        super(_context, R.layout.artist_item);
        context = _context;
        data = _data;
    }

    @Override
    public int getCount() {
        return data.length;
    }

    @Nullable
    @Override
    public ArtistShell getItem(int position) {
        return data[position];
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.artist_item, parent, false);
        }
        TextView artist = convertView.findViewById(R.id.artistItemText);
        artist.setText(getItem(position).getTitle());

        return convertView;
    }
}
