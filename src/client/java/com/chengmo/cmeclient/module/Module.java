package com.chengmo.cmeclient.module;

import com.chengmo.cmeclient.module.setting.Setting;
import com.chengmo.cmeclient.util.Lang;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    private final String id;
    private final Category category;
    private boolean enabled;
    public int keybind = -1;
    private final List<Setting<?>> settings = new ArrayList<>();

    public enum Category {
        COMBAT, MOVEMENT, RENDER, MISC, SINGLEPLAYER, SETTINGS;
        public String displayName() {
            if (this == SETTINGS) return Lang.get("cmeclient.gui.settings");
            return Lang.category(name());
        }
    }

    public Module(String id, Category category) {
        this.id = id;
        this.category = category;
        this.enabled = false;
    }

    public String getId() { return id; }
    public String getName() { return Lang.moduleName(id); }
    public String getDescription() { return Lang.moduleDesc(id); }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean value) {
        if (this.enabled == value) return;
        this.enabled = value;
        if (value) onEnable();
        else onDisable();
    }

    /** 读档专用：只改状态，不触发任何钩子 */
    public void setEnabledSilently(boolean value) {
        this.enabled = value;
    }

    public void toggle() { setEnabled(!enabled); }

    public List<Setting<?>> getSettings() { return settings; }
    protected <S extends Setting<?>> S addSetting(S s) { settings.add(s); return s; }
    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
}
