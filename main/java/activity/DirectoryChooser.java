package activity;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;

import adapter.BrowserAdapter;

public class DirectoryChooser extends AppCompatActivity {

    // getting the root directory
    static final File ROOT_DIR = Environment.getExternalStorageDirectory();
    File currentDir;

    File [] dirContent;

    Button chooseButton;
    Button backButton;

    ListView browseListView;

    BrowserAdapter browserAdapter;

    TextView currentDirText;

    private int MAX_LENGTH;

    /*
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory_chooser);

        MAX_LENGTH = getResources().getConfiguration().screenWidthDp / 14;

        chooseButton = (Button) findViewById(R.id.chooseButton);
        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // add current directory to the directory list

                // checking if given directory is not a subdirectory of an already added directory
                // or if the given directory is not already on the list
                for (File f : DirActivity.dirList){
                    if (isSubdirectory(f, currentDir) || f.equals(currentDir)){
                        Toast.makeText(getApplicationContext(), R.string.toast_cannot_add, Toast.LENGTH_LONG).show();
                        runDirActivity();
                        return;
                    }
                }

                // checking if any of the directories is not a subdirectory of the given directory
                // if at least one of them is, user is presented with a dialogue window in which they
                // decide if they want to replace found directories with the last added one
                final ArrayList <File> subdirs = new ArrayList<>();
                for (File f : DirActivity.dirList) {
                    if (isSubdirectory(currentDir, f)) {
                        subdirs.add(f);
                    }
                }
                // if the added directory is a parent directory to any of the dirList's contents, do
                if (!subdirs.isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(DirectoryChooser.this);
                    builder.setTitle(R.string.dialog_title);
                    builder.setMessage(R.string.dialog_text);
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            for (File f : subdirs){
                                DirActivity.dirList.remove(f);
                            }
                            DirActivity.addDirectory(currentDir, subdirs);
                            runDirActivity();
                        }
                    });
                    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            runDirActivity();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    // else everything is fine, we can add the directory without any problems
                    DirActivity.addDirectory(currentDir);
                    runDirActivity();
                }
            }
        });

        backButton = (Button) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goBack();
            }
        });

        browseListView = (ListView) findViewById(R.id.browseListView);

        currentDirText = (TextView) findViewById(R.id.currentDirText);

        openDirectory(ROOT_DIR);

        browseListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                openDirectory(browserAdapter.getItem(i));
            }
        });
    }
    */

    private void openDirectory(File dir){
        if (dir.equals(ROOT_DIR)){
            backButton.setEnabled(false);
        } else {
            backButton.setEnabled(true);
        }
        // set current directory to target dir value
        currentDir = dir;
        // update the current directory info
        String path = currentDir.getAbsolutePath();
        if (path.length() >= MAX_LENGTH){
            currentDirText.setText(cropPath(path));
        } else {
            currentDirText.setText(path);
        }
        // listing all directories in the target directory
        dirContent = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });

        // setting the data set for the adapter
        browserAdapter = new BrowserAdapter(this, dirContent);
        browseListView.setAdapter(browserAdapter);
    }

    private String cropPath(String path){
        while (path.length() + 3 >= MAX_LENGTH && path.indexOf('/', 1) != -1){
            int index = path.indexOf('/', 1);
            path = path.substring(index);
        }
        return "..." + path;
    }

    private void runDirActivity(){
        Intent intent = new Intent(getApplicationContext(), DirActivity.class);
        startActivity(intent);
        finish();
    }

    private void goBack(){
        if (!currentDir.equals(ROOT_DIR)){
            openDirectory(currentDir.getParentFile());
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), DirActivity.class);
        startActivity(intent);
        super.onBackPressed();
    }

    private static boolean isSubdirectory(File dir, File subdir){
        return subdir.getPath().startsWith(dir.getPath());
    }
}