package audioeffect;

import android.media.audiofx.BassBoost;
import android.media.audiofx.EnvironmentalReverb;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;

/**
 * Created by prett on 12/25/2017.
 */

public class Effector {
    private static final int PRIORITY = 0;

    private static Effector effector;

    private int audioSessionId;

    private boolean released;

    private Equalizer equalizer;
    private PresetReverb presetReverb;
    private EnvironmentalReverb environmentalReverb;
    private Virtualizer virtualizer;
    private BassBoost bassBoost;

    private EffectBundle effectBundle;

    private Effector(int audioSessionId){
        this.audioSessionId = audioSessionId;

        equalizer = new Equalizer(PRIORITY, audioSessionId);
        presetReverb = new PresetReverb(PRIORITY, audioSessionId);
        environmentalReverb = new EnvironmentalReverb(PRIORITY, audioSessionId);
        virtualizer = new Virtualizer(PRIORITY, audioSessionId);
        bassBoost = new BassBoost(PRIORITY, audioSessionId);

        released = false;
    }

    public int getAudioSessionId(){
        return audioSessionId;
    }

    public boolean isReleased(){
        return released;
    }

    public void release(){
        equalizer.release();
        equalizer = null;

        presetReverb.release();
        presetReverb = null;

        environmentalReverb.release();
        environmentalReverb = null;

        virtualizer.release();
        virtualizer = null;

        bassBoost.release();
        bassBoost = null;

        released = true;
    }

    public void setEffectBundle(EffectBundle bundle){
        effectBundle = bundle;

        if (effectBundle.getEqualizerSettings() != null)
            equalizer.setProperties(effectBundle.getEqualizerSettings());
        if (effectBundle.getPresetReverbSettings() != null)
            presetReverb.setProperties(effectBundle.getPresetReverbSettings());
        if (effectBundle.getEnviromentalReverbSettings() != null)
            environmentalReverb.setProperties(effectBundle.getEnviromentalReverbSettings());
        if (effectBundle.getVirtualizerSettings() != null)
            virtualizer.setProperties(effectBundle.getVirtualizerSettings());
        if (effectBundle.getBassBoostSettings() != null)
            bassBoost.setProperties(effectBundle.getBassBoostSettings());

        equalizer.setEnabled(effectBundle.isEqEnabled());
        presetReverb.setEnabled(effectBundle.isPresetReverbEnabled());
        environmentalReverb.setEnabled(effectBundle.isEnviromentalReverbEnabled());
        virtualizer.setEnabled(effectBundle.isVirtualizerEnabled());
        bassBoost.setEnabled(effectBundle.isBassBoostEnabled());
    }

    // saves current effector state into the currently chosen EffectBundle, rewriting its settings
    public void saveEffectorState(){
        effectBundle.setBassBoostEnabled(bassBoost.getEnabled());
        effectBundle.setEnviromentalReverbEnabled(environmentalReverb.getEnabled());
        effectBundle.setEqEnabled(equalizer.getEnabled());
        effectBundle.setPresetReverbEnabled(presetReverb.getEnabled());
        effectBundle.setVirtualizerEnabled(virtualizer.getEnabled());

        effectBundle.setEqualizerSettings(equalizer.getProperties());
        effectBundle.setVirtualizerSettings(virtualizer.getProperties());
        effectBundle.setBassBoostSettings(bassBoost.getProperties());
        effectBundle.setEnviromentalReverbSettings(environmentalReverb.getProperties());
        effectBundle.setPresetReverbSettings(presetReverb.getProperties());
    }

    public EffectBundle getEffectorState(){
        EffectBundle effectBundle = new EffectBundle();

        effectBundle.setBassBoostEnabled(bassBoost.getEnabled());
        effectBundle.setEnviromentalReverbEnabled(environmentalReverb.getEnabled());
        effectBundle.setEqEnabled(equalizer.getEnabled());
        effectBundle.setPresetReverbEnabled(presetReverb.getEnabled());
        effectBundle.setVirtualizerEnabled(virtualizer.getEnabled());

        effectBundle.setEqualizerSettings(equalizer.getProperties());
        effectBundle.setVirtualizerSettings(virtualizer.getProperties());
        effectBundle.setBassBoostSettings(bassBoost.getProperties());
        effectBundle.setEnviromentalReverbSettings(environmentalReverb.getProperties());
        effectBundle.setPresetReverbSettings(presetReverb.getProperties());

        return effectBundle;
    }

    public Equalizer getEqualizer() {
        return equalizer;
    }

    public PresetReverb getPresetReverb() {
        return presetReverb;
    }

    public EnvironmentalReverb getEnvironmentalReverb() {
        return environmentalReverb;
    }

    public Virtualizer getVirtualizer() {
        return virtualizer;
    }

    public BassBoost getBassBoost() {
        return bassBoost;
    }

    public static synchronized Effector getInstance(int audioSessionId){
        if (effector == null){
            effector = new Effector(audioSessionId);
        } else if (effector.getAudioSessionId() != audioSessionId){
            effector.release();
            effector = new Effector(audioSessionId);
        }
        return effector;
    }
}
