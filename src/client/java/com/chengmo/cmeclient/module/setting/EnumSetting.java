package com.chengmo.cmeclient.module.setting;

public class EnumSetting extends Setting<Integer> {
    private final String[] options;

    public EnumSetting(String id, int defaultIndex, String... options) {
        super(id, defaultIndex);
        this.options = options;
    }

    public String[] getOptions() { return options; }
    public String getCurrent() { return options[value]; }
    public void next() { value = (value + 1) % options.length; }
    public void prev() { value = (value - 1 + options.length) % options.length; }
}
