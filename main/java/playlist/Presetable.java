package playlist;

import audioeffect.EffectBundle;

public interface Presetable {
    EffectBundle getPreset();
    void setPreset(EffectBundle preset);
    boolean hasPreset();
}
