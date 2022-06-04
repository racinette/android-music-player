package fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.prett.myapplication.R;

/**
 * Created by prett on 12/24/2017.
 */

public class EffectFragment extends Fragment {

    private static final int EQ_TAB = 0;
    private static final int REVERB_TAB = 1;
    private static final int MISC_TAB = 2;

    private int state;

    private TabLayout effectChoiceTabLayout;

    private FragmentManager fragmentManager;

    private final TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            int number = tab.getPosition();

            switch (number){
                case EQ_TAB:
                    if (state != EQ_TAB){
                        state = EQ_TAB;
                        fragmentManager.beginTransaction().replace(R.id.effectFragmentContainer, new EqualizerFragment()).commit();
                    }
                    break;
                case REVERB_TAB:
                    if (state != REVERB_TAB){
                        state = REVERB_TAB;
                        fragmentManager.beginTransaction().replace(R.id.effectFragmentContainer, new ReverbFragment()).commit();
                    }
                    break;
                case MISC_TAB:
                    if (state != MISC_TAB){
                        state = MISC_TAB;
                        fragmentManager.beginTransaction().replace(R.id.effectFragmentContainer, new MiscFragment()).commit();
                    }
                    break;
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.effect_fragment, container, false);

        fragmentManager = getActivity().getFragmentManager();

        effectChoiceTabLayout = rootView.findViewById(R.id.effectChoiceTabLayout);
        effectChoiceTabLayout.addOnTabSelectedListener(onTabSelectedListener);

        state = EQ_TAB;
        fragmentManager.beginTransaction().add(R.id.effectFragmentContainer, new EqualizerFragment()).commit();

        return rootView;
    }
}
