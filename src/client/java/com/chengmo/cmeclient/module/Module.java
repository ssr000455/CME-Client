package com.chengmo.cmeclient.module;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;

    public enum Category {
        COMBAT("战斗"),
        MOVEMENT("移动"),
        RENDER("渲染"),
        MISC("其他");

        public final String name;
        Category(String name) { this.name = name; }
    }

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = false;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public void toggle() { this.enabled = !this.enabled; }
    public void onTick() {}
}
