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

import playlist.Genre;
import playlist.GenreShell;

public class GenreAdapter extends ArrayAdapter {
    private GenreShell[] data;
    private Context context;

    public GenreAdapter(Context _context, GenreShell[] _data){
        super(_context, R.layout.genre_item);
        data = _data;
        context = _context;
    }

    @Nullable
    @Override
    public GenreShell getItem(int position) {
        return data[position];
    }

    @Override
    public int getCount() {
        return data.length;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.genre_item, parent, false);
        }

        TextView genreText = convertView.findViewById(R.id.genreItemText);

        GenreShell genre = getItem(position);

        genreText.setText(genre.getTitle());
        return convertView;
    }
}
