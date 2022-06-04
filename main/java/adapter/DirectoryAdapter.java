package adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.example.prett.myapplication.R;

import java.io.File;
import java.util.ArrayList;

public class DirectoryAdapter extends ArrayAdapter {
    private final Context _context;
    private ArrayList <File> _values;
    private boolean checkedPos [];

    public DirectoryAdapter(Context context, ArrayList<File> values){
        super(context, R.layout.directory_chooser_item, values);
        _values = values;
        checkedPos = new boolean[_values.size()];
        _context = context;
    }

    @Nullable
    @Override
    public File getItem(int position) {
        return _values.get(position);
    }

    public boolean [] getCheckedPos(){
        return checkedPos;
    }

    public boolean check(int position){
        checkedPos[position] = !checkedPos[position];
        return checkedPos[position];
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.directory_item, parent, false);
        }
        TextView pathText = convertView.findViewById(R.id.pathText);
        pathText.setText(_values.get(position).getPath());
        CheckBox selectCheckBox = convertView.findViewById(R.id.selectCheckBox);
        selectCheckBox.setSelected(checkedPos[position]);
        return convertView;
    }
}
