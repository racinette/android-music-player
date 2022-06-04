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

public class TitlesAdapter extends ArrayAdapter<String> {
    private Context context;
    private String[] titles;

    public TitlesAdapter(String [] titles, Context context){
        super(context, R.layout.spinner_playlist_item, R.id.playlistSpinnerText);
        this.context = context;
        this.titles = titles;
    }

    @Override
    public int getCount() {
        return titles.length;
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return titles[position];
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.spinner_playlist_item, parent, false);
        }

        final TextView title = convertView.findViewById(R.id.playlistSpinnerText);
        title.setText(titles[position]);

        return convertView;
    }
}
