package com.chengmo.cmeclient.module.setting;

public abstract class Setting<T> {
    private final String id;
    private final String nameKey;
    protected T value;
    private final T defaultValue;

    public Setting(String id, T defaultValue) {
        this.id = id;
        this.nameKey = "cmeclient.setting." + id;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public String getId() { return id; }
    public String getNameKey() { return nameKey; }
    public T get() { return value; }
    public void set(T v) { this.value = v; }
    public T getDefault() { return defaultValue; }
    /** UI helper: restore the value the module was constructed with. */
    public void reset() { this.value = defaultValue; }
}
