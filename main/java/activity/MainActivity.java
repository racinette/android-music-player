package activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import service.PlayerService;
import com.example.prett.myapplication.R;

import broadcast.Extras;
import broadcast.Messages;
import fragment.MenuFragment;
import fragment.PlayerFragment;
import fragment.PlaylistFragment;

public class MainActivity extends AppCompatActivity {
    private static final int READ_EXTERNAL_STORAGE_PERMISSION = 1;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION = 2;

    private ViewPager viewPager;
    private SwipeAdapter swipeAdapter;

    private BroadcastReceiver jumpToTabReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int number = intent.getIntExtra(Extras.NUMBER_EXTRA, -1);
            if (number > -1) {
                viewPager.setCurrentItem(2);
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case READ_EXTERNAL_STORAGE_PERMISSION : {
                if (grantResults.length > 0){
                    if (!PlayerService.isRunning()){
                        startService(new Intent(getApplicationContext(), PlayerService.class));
                    }
                } else {
                    Toast.makeText(this, "Couldn't read your external storage. Please, check if you permitted the player to do so!", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.swipe_activity);

        // checking for permission to read and write the external storage
        checkReadPermission();
        checkWritePermission();

        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED){
            // start player service
            if (!PlayerService.isRunning()){
                startService(new Intent(getApplicationContext(), PlayerService.class));
            }
        }

        viewPager = findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(2);

        swipeAdapter = new SwipeAdapter(getFragmentManager());

        LocalBroadcastManager.getInstance(this).registerReceiver(jumpToTabReceiver, new IntentFilter(Messages.JUMP_TO_TAB_MESSAGE));

        viewPager.setAdapter(swipeAdapter);
    }

    // SWIPE ADAPTER CLASS
    public static class SwipeAdapter extends FragmentPagerAdapter{
        private static final int NUM_PAGES = 3;

        public SwipeAdapter(FragmentManager fragmentManager){
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new MenuFragment();
                case 1:
                    return new PlayerFragment();
                case 2:
                    return new PlaylistFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(jumpToTabReceiver);
        super.onDestroy();
    }

    private void checkReadPermission(){
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION);
        }
    }

    private void checkWritePermission(){
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
    }
}