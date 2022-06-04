package fragment;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.prett.myapplication.R;

import audioeffect.Effector;
import broadcast.Extras;
import broadcast.Messages;
import widget.VerticalSeekBar;

public class EqualizerFragment extends Fragment {

    private final String DB = "dB";

    private Equalizer equalizer;

    private VerticalSeekBar [] bands;
    private TextView maxDbText;
    private TextView minDbText;
    private GridLayout eqBandsGrid;

    private short minMilibells;
    private short maxMilibells;

    private int bandsHeight;

    private final BroadcastReceiver audioSessionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {

            final int audioSession = intent.getIntExtra(Extras.AUDIO_SESSION_EXTRA, 0);

            // try
            equalizer = Effector.getInstance(audioSession).getEqualizer();
            // catch


            Log.e("EQ", "EqualizerFragment: hasControl() - " + equalizer.hasControl() + "; getEnabled() - " + equalizer.getEnabled());

            short numberOfBands = equalizer.getNumberOfBands();

            short [] bandLevelRange = equalizer.getBandLevelRange();

            int [] bandCenterFreq = new int[numberOfBands];
            for (short i = 0; i <  numberOfBands; i++){
                bandCenterFreq[i] = equalizer.getCenterFreq(i);
            }


            short [] bandLevels = new short[numberOfBands];
            for (short i = 0; i < numberOfBands; i++){
                bandLevels[i] = equalizer.getBandLevel(i);
            }


            minMilibells = bandLevelRange[0];
            maxMilibells = bandLevelRange[1];

            String minDbString = Integer.toString(minMilibells / 100) + DB;
            String maxDbString = Integer.toString(maxMilibells / 100) + DB;

            minDbText.setText(minDbString);
            maxDbText.setText(maxDbString);

            eqBandsGrid.setColumnCount(numberOfBands);
            bands = new VerticalSeekBar[numberOfBands];

            for (short i = 0; i < numberOfBands; i++){
                VerticalSeekBar band = new VerticalSeekBar(getActivity());

                final short num = i;

                bands[i] = band;

                eqBandsGrid.addView(band);

                // setting layout params
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(band.getLayoutParams());
                params.setGravity(Gravity.CENTER_HORIZONTAL);
                params.height = bandsHeight;
                params.columnSpec = GridLayout.spec(i, 1f);
                params.rowSpec = GridLayout.spec(0);

                // since you cannot set min value, and it is always zero:
                band.setMax(maxMilibells + Math.abs(minMilibells));
                band.setProgress(bandLevels[i] + Math.abs(minMilibells));

                VerticalSeekBar.OnSeekBarChangeListener listener = new VerticalSeekBar.OnSeekBarChangeListener(){
                    short number = num;

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        short pos = (short) (progress - Math.abs(minMilibells));
                        Log.e("EQ", minMilibells + " < " + pos + " < " + maxMilibells);
                        equalizer.setBandLevel(number, pos);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                };
                band.setOnSeekBarChangeListener(listener);

                band.setLayoutParams(params);

                // unregister itself
                LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(this);
            }

        }
    };

    private void demandAudioSessionData(){
        Intent intent = new Intent(Messages.AUDIO_SESSION_DEMAND_MESSAGE);
        intent.putExtra(Extras.AUDIO_SESSION_DEMAND_EXTRA, true);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.equalizer_fragment, container, false);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(audioSessionReceiver, new IntentFilter(Messages.AUDIO_SESSION_MESSAGE));

        maxDbText = rootView.findViewById(R.id.maxDbValueText);
        minDbText = rootView.findViewById(R.id.minDbValueText);

        eqBandsGrid = rootView.findViewById(R.id.eqBandsGrid);

        // measure the views
        maxDbText.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        minDbText.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        // find bands height
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        // height is the same as width, because the imageview is square
        int fragmentHeight = metrics.widthPixels;

        bandsHeight = Math.abs(fragmentHeight - (maxDbText.getMeasuredHeight() + minDbText.getMeasuredHeight()));
        // demand audio session id
        demandAudioSessionData();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
