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

import java.io.File;

public class BrowserAdapter extends ArrayAdapter {
    private final Context _context;
    private File [] _values;

    public BrowserAdapter(Context context, File [] values){
        super(context, R.layout.directory_chooser_item, values);
        _values = values;
        _context = context;
    }

    @Nullable
    @Override
    public File getItem(int position) {
        return _values[position];
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.directory_chooser_item, parent, false);
        }
        TextView text = convertView.findViewById(R.id.nameText);
        text.setText(_values[position].getName());
        return convertView;
    }
}
