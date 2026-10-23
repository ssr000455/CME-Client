package com.chengmo.cmeclient.module;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();
    private final List<Module> modules = new ArrayList<>();

    public void addModule(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public Module findById(String id) {
        for (Module module : modules) {
            if (module.getId().equals(id)) return module;
        }
        return null;
    }

    /** Toggles every module bound to the given GLFW key. Returns what changed, if anything. */
    public List<Module> toggleByKeybind(int key) {
        List<Module> toggled = new ArrayList<>();
        if (key <= 0) return toggled;
        for (Module module : modules) {
            if (module.keybind == key) {
                module.toggle();
                toggled.add(module);
            }
        }
        return toggled;
    }

    public void onTick() {
        for (Module m : modules) {
            if (m.isEnabled()) m.onTick();
        }
    }
}
