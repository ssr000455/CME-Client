package com.chengmo.cmeclient.module.setting;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String id, boolean defaultValue) {
        super(id, defaultValue);
    }
    public void toggle() { value = !value; }
}
