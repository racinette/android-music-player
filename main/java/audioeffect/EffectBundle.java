package audioeffect;

import android.media.audiofx.BassBoost;
import android.media.audiofx.EnvironmentalReverb;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.Virtualizer;

public class EffectBundle {

    private static final EffectBundle NO_PRESET = new EffectBundle();
    private static final String NO_PRESET_NAME = "No preset";

    private boolean deleted;

    private String name;

    private boolean eqEnabled;
    private Equalizer.Settings equalizerSettings;

    private boolean bassBoostEnabled;
    private BassBoost.Settings bassBoostSettings;

    private boolean presetReverbEnabled;
    private PresetReverb.Settings presetReverbSettings;

    private boolean enviromentalReverbEnabled;
    private EnvironmentalReverb.Settings enviromentalReverbSettings;

    private boolean virtualizerEnabled;
    private Virtualizer.Settings virtualizerSettings;

    public EffectBundle(){
        name = NO_PRESET_NAME;
        eqEnabled = false;
        bassBoostEnabled = false;
        presetReverbEnabled = false;
        enviromentalReverbEnabled = false;
        virtualizerEnabled = false;
        deleted = false;
    }

    public EffectBundle(String name){
        this.name = name;
        eqEnabled = false;
        bassBoostEnabled = false;
        presetReverbEnabled = false;
        enviromentalReverbEnabled = false;
        virtualizerEnabled = false;
        deleted = false;
    }

    public static EffectBundle getStandardPreset(){
        return NO_PRESET;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isStandard(){
        return this == NO_PRESET;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void delete(){
        name = null;
        equalizerSettings = null;
        bassBoostSettings = null;
        enviromentalReverbSettings = null;
        virtualizerSettings = null;
        presetReverbSettings = null;
        deleted = true;
    }

    public boolean isEqEnabled() {
        return eqEnabled;
    }

    public void setEqEnabled(boolean eqEnabled) {
        this.eqEnabled = eqEnabled;
    }

    public Equalizer.Settings getEqualizerSettings() {
        return equalizerSettings;
    }

    public void setEqualizerSettings(Equalizer.Settings equalizerSettings) {
        this.equalizerSettings = equalizerSettings;
    }

    public boolean isBassBoostEnabled() {
        return bassBoostEnabled;
    }

    public void setBassBoostEnabled(boolean bassBoostEnabled) {
        this.bassBoostEnabled = bassBoostEnabled;
    }

    public BassBoost.Settings getBassBoostSettings() {
        return bassBoostSettings;
    }

    public void setBassBoostSettings(BassBoost.Settings bassBoostSettings) {
        this.bassBoostSettings = bassBoostSettings;
    }

    public boolean isPresetReverbEnabled() {
        return presetReverbEnabled;
    }

    public void setPresetReverbEnabled(boolean presetReverbEnabled) {
        this.presetReverbEnabled = presetReverbEnabled;
    }

    public PresetReverb.Settings getPresetReverbSettings() {
        return presetReverbSettings;
    }

    public void setPresetReverbSettings(PresetReverb.Settings presetReverbSettings) {
        this.presetReverbSettings = presetReverbSettings;
    }

    public boolean isEnviromentalReverbEnabled() {
        return enviromentalReverbEnabled;
    }

    public void setEnviromentalReverbEnabled(boolean enviromentalReverbEnabled) {
        this.enviromentalReverbEnabled = enviromentalReverbEnabled;
    }

    public EnvironmentalReverb.Settings getEnviromentalReverbSettings() {
        return enviromentalReverbSettings;
    }

    public void setEnviromentalReverbSettings(EnvironmentalReverb.Settings enviromentalReverbSettings) {
        this.enviromentalReverbSettings = enviromentalReverbSettings;
    }

    public boolean isVirtualizerEnabled() {
        return virtualizerEnabled;
    }

    public void setVirtualizerEnabled(boolean virtualizerEnabled) {
        this.virtualizerEnabled = virtualizerEnabled;
    }

    public Virtualizer.Settings getVirtualizerSettings() {
        return virtualizerSettings;
    }

    public void setVirtualizerSettings(Virtualizer.Settings virtualizerSettings) {
        this.virtualizerSettings = virtualizerSettings;
    }
}
