package com.chengmo.cmeclient.module.setting;

public class FloatSetting extends Setting<Float> {
    private final float min;
    private final float max;

    public FloatSetting(String id, float defaultValue, float min, float max) {
        super(id, defaultValue);
        this.min = min;
        this.max = max;
    }

    public float getMin() { return min; }
    public float getMax() { return max; }
    public void setClamped(float v) { this.value = Math.max(min, Math.min(max, v)); }
}
