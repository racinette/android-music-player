package adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.prett.myapplication.R;

import java.util.ArrayList;

import playlist.Album;
import playlist.AlbumShell;
import pools.ImagePool;

public class AlbumAdapter extends ArrayAdapter {
    private AlbumShell[] data;
    private Context context;

    public AlbumAdapter(Context _context, AlbumShell[] _data){
        super(_context, R.layout.album_item, _data);
        data = _data;
        context = _context;
    }

    @Nullable
    @Override
    public AlbumShell getItem(int position) {
        return data[position];
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.album_item, parent, false);
        }

        ImageView albumCover = convertView.findViewById(R.id.albumCoverImage);
        TextView albumTitle = convertView.findViewById(R.id.albumTitleText);
        TextView artistName = convertView.findViewById(R.id.artistNameText);

        AlbumShell album = getItem(position);

        albumCover.setImageBitmap(ImagePool.getImage(album.getImageKey()));
        albumTitle.setText(album.getTitle());
        artistName.setText(album.getArtist());

        return convertView;
    }
}
