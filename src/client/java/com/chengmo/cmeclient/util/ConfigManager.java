package com.chengmo.cmeclient.util;

import com.chengmo.cmeclient.gui.ClickGUI;
import com.chengmo.cmeclient.gui.HUD;
import com.chengmo.cmeclient.gui.component.ModuleCard;
import com.chengmo.cmeclient.gui.component.Theme;
import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.BlockESP;
import com.chengmo.cmeclient.module.impl.ESP;
import com.chengmo.cmeclient.module.setting.BoolSetting;
import com.chengmo.cmeclient.module.setting.EnumSetting;
import com.chengmo.cmeclient.module.setting.FloatSetting;
import com.chengmo.cmeclient.module.setting.Setting;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final String FILE_NAME = "cmeclient-ui.json";
    private static boolean dirty = false;

    private ConfigManager() {}

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    public static void markDirty() { dirty = true; }

    public static void tick() {
        if (!dirty) return;
        save();
    }

    public static void load() {
        Path path = path();
        if (!Files.exists(path)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();

            JsonObject ui = object(root, "ui");
            if (ui != null) {
                ClickGUI.uiScale = UiUtil.clamp(num(ui, "scale", 1f), 0.7f, 1.5f);
                ClickGUI.themeIndex = Math.max(0, Math.min(4, integer(ui, "theme", 0)));
                ClickGUI.panelOffsetX = num(ui, "panelX", 0f);
                ClickGUI.panelOffsetY = num(ui, "panelY", 0f);
                ClickGUI.closeOnOutsideClick = bool(ui, "closeOutside", false);
                Theme.roundingScale = UiUtil.clamp(num(ui, "rounding", 1f), 0.3f, 2f);
                Theme.panelOpacity = UiUtil.clamp(num(ui, "opacity", 1f), 0.5f, 1f);
                Theme.customAccentEnabled = bool(ui, "accentCustom", false);
                JsonArray accent = array(ui, "accent");
                if (accent != null && accent.size() >= 3) {
                    Theme.customAccent[0] = UiUtil.clamp(accent.get(0).getAsFloat(), 0f, 1f);
                    Theme.customAccent[1] = UiUtil.clamp(accent.get(1).getAsFloat(), 0f, 1f);
                    Theme.customAccent[2] = UiUtil.clamp(accent.get(2).getAsFloat(), 0f, 1f);
                }
                String view = string(ui, "view", "MISC");
                ClickGUI.favoritesView = "FAVORITES".equals(view);
                ClickGUI.recentView = "RECENT".equals(view);
                if (!ClickGUI.favoritesView && !ClickGUI.recentView) {
                    try {
                        ClickGUI.currentCategory = Module.Category.valueOf(view);
                    } catch (IllegalArgumentException ignored) {
                        ClickGUI.currentCategory = Module.Category.MISC;
                    }
                }
                JsonArray color = array(ui, "textColor");
                if (color != null && color.size() >= 3) {
                    ClickGUI.customTextColor[0] = UiUtil.clamp(color.get(0).getAsFloat(), 0f, 1f);
                    ClickGUI.customTextColor[1] = UiUtil.clamp(color.get(1).getAsFloat(), 0f, 1f);
                    ClickGUI.customTextColor[2] = UiUtil.clamp(color.get(2).getAsFloat(), 0f, 1f);
                }
            }

            JsonObject hud = object(root, "hud");
            if (hud != null) {
                HUD.hudEnabled = bool(hud, "enabled", HUD.hudEnabled);
                HUD.showWatermark = bool(hud, "watermark", HUD.showWatermark);
                HUD.showArrayList = bool(hud, "arraylist", HUD.showArrayList);
                HUD.showInfo = bool(hud, "info", HUD.showInfo);
                HUD.showFps = bool(hud, "fps", HUD.showFps);
                HUD.showPing = bool(hud, "ping", HUD.showPing);
                HUD.showCoords = bool(hud, "coords", HUD.showCoords);
                HUD.showEffects = bool(hud, "effects", HUD.showEffects);
                HUD.showDurability = bool(hud, "durability", HUD.showDurability);
                HUD.hudTextScale = UiUtil.clamp(num(hud, "textScale", HUD.hudTextScale), 0.8f, 1.3f);
                HUD.hudBackground = bool(hud, "background", HUD.hudBackground);
                HUD.hudRightAlign = bool(hud, "infoRight", HUD.hudRightAlign);
                HUD.watermarkX = UiUtil.clamp(num(hud, "wmX", HUD.watermarkX), 0f, 1f);
                HUD.watermarkY = UiUtil.clamp(num(hud, "wmY", HUD.watermarkY), 0f, 1f);
                HUD.arrayRight = UiUtil.clamp(num(hud, "arrayRight", HUD.arrayRight), 0f, 1f);
                HUD.arrayTop = UiUtil.clamp(num(hud, "arrayTop", HUD.arrayTop), 0f, 1f);
                HUD.infoX = UiUtil.clamp(num(hud, "infoX", HUD.infoX), 0f, 1f);
                HUD.infoY = UiUtil.clamp(num(hud, "infoY", HUD.infoY), 0f, 1f);
            }

            JsonArray favorites = array(root, "favorites");
            if (favorites != null) {
                ModuleCard.favorites.clear();
                for (JsonElement element : favorites) ModuleCard.favorites.add(element.getAsString());
            }

            JsonArray recents = array(root, "recents");
            if (recents != null) {
                ModuleCard.recents.clear();
                for (JsonElement element : recents) {
                    String id = element.getAsString();
                    if (!ModuleCard.recents.contains(id)) ModuleCard.recents.add(id);
                }
            }

            JsonArray expanded = array(root, "expanded");
            if (expanded != null) {
                java.util.List<String> ids = new java.util.ArrayList<>();
                for (JsonElement element : expanded) ids.add(element.getAsString());
                ModuleCard.restoreExpanded(ids);
            }

            JsonObject modules = object(root, "modules");
            if (modules != null) {
                for (Module module : ModuleManager.INSTANCE.getModules()) {
                    try { // 极其关键：每个模块单独 try-catch，防止一错全错
                        JsonElement element = modules.get(module.getId());
                        if (element == null || !element.isJsonObject()) continue;
                        JsonObject data = element.getAsJsonObject();
                        module.keybind = integer(data, "keybind", module.keybind);
                        JsonObject settings = object(data, "settings");
                        if (settings != null) {
                            for (Setting<?> setting : module.getSettings()) {
                                JsonElement value = settings.get(setting.getId());
                                if (value != null) restore(setting, value);
                            }
                        }
                        if (module instanceof BlockESP be) {
                            JsonArray blocks = array(data, "blocks");
                            if (blocks != null) {
                                be.blocks.clear();
                                for (JsonElement el : blocks) {
                                    if (!el.isJsonObject()) continue;
                                    JsonObject obj = el.getAsJsonObject();
                                    String bid = string(obj, "id", null);
                                    int colorIdx = integer(obj, "color", 0);
                                    if (bid != null) be.blocks.add(new BlockESP.BlockEntry(bid, colorIdx));
                                }
                            }
                        }
                        if (module instanceof ESP esp) {
                            JsonArray entities = array(data, "entities");
                            if (entities != null) {
                                esp.customEntities.clear();
                                for (JsonElement el : entities) {
                                    if (!el.isJsonObject()) continue;
                                    JsonObject obj = el.getAsJsonObject();
                                    String eid = string(obj, "id", null);
                                    int colorIdx = integer(obj, "color", 0);
                                    if (eid != null) esp.customEntities.add(new ESP.EntityEntry(eid, colorIdx));
                                }
                            }
                        }
                        
                        // 静默加载，不触发 onEnable，绝不自杀
                        module.setEnabledSilently(bool(data, "enabled", false));
                        
                    } catch (Exception e) {
                        System.err.println("[CME] 读取 " + module.getId() + " 配置异常，已跳过");
                        e.printStackTrace();
                    }
                }
            }

            String lang = string(root, "lang", null);
            if (lang != null) Lang.isChinese = "zh_cn".equals(lang);
        } catch (Exception e) {
            System.err.println("[CME] 配置文件解析严重错误，未进行任何修改！");
            e.printStackTrace();
        }
    }

    public static void save() {
        dirty = false;
        try {
            JsonObject root = new JsonObject();
            
            JsonObject ui = new JsonObject();
            ui.addProperty("scale", ClickGUI.uiScale);
            ui.addProperty("theme", ClickGUI.themeIndex);
            ui.addProperty("view", ClickGUI.favoritesView ? "FAVORITES" : ClickGUI.recentView ? "RECENT" : ClickGUI.currentCategory.name());
            ui.addProperty("panelX", ClickGUI.panelOffsetX);
            ui.addProperty("panelY", ClickGUI.panelOffsetY);
            ui.addProperty("closeOutside", ClickGUI.closeOnOutsideClick);
            ui.addProperty("rounding", Theme.roundingScale);
            ui.addProperty("opacity", Theme.panelOpacity);
            ui.addProperty("accentCustom", Theme.customAccentEnabled);
            JsonArray accent = new JsonArray();
            accent.add(Theme.customAccent[0]);
            accent.add(Theme.customAccent[1]);
            accent.add(Theme.customAccent[2]);
            ui.add("accent", accent);
            JsonArray color = new JsonArray();
            color.add(ClickGUI.customTextColor[0]);
            color.add(ClickGUI.customTextColor[1]);
            color.add(ClickGUI.customTextColor[2]);
            ui.add("textColor", color);
            root.add("ui", ui);

            JsonObject hud = new JsonObject();
            hud.addProperty("enabled", HUD.hudEnabled);
            hud.addProperty("watermark", HUD.showWatermark);
            hud.addProperty("arraylist", HUD.showArrayList);
            hud.addProperty("info", HUD.showInfo);
            hud.addProperty("fps", HUD.showFps);
            hud.addProperty("ping", HUD.showPing);
            hud.addProperty("coords", HUD.showCoords);
            hud.addProperty("effects", HUD.showEffects);
            hud.addProperty("durability", HUD.showDurability);
            hud.addProperty("textScale", HUD.hudTextScale);
            hud.addProperty("background", HUD.hudBackground);
            hud.addProperty("infoRight", HUD.hudRightAlign);
            hud.addProperty("wmX", HUD.watermarkX);
            hud.addProperty("wmY", HUD.watermarkY);
            hud.addProperty("arrayRight", HUD.arrayRight);
            hud.addProperty("arrayTop", HUD.arrayTop);
            hud.addProperty("infoX", HUD.infoX);
            hud.addProperty("infoY", HUD.infoY);
            root.add("hud", hud);

            JsonArray favorites = new JsonArray();
            for (String id : ModuleCard.favorites) favorites.add(id);
            root.add("favorites", favorites);

            JsonArray recents = new JsonArray();
            for (String id : ModuleCard.recents) recents.add(id);
            root.add("recents", recents);

            JsonArray expanded = new JsonArray();
            for (String id : ModuleCard.expandedSnapshot()) expanded.add(id);
            root.add("expanded", expanded);

            JsonObject modules = new JsonObject();
            for (Module module : ModuleManager.INSTANCE.getModules()) {
                JsonObject data = new JsonObject();
                data.addProperty("enabled", module.isEnabled());
                data.addProperty("keybind", module.keybind);
                JsonObject settings = new JsonObject();
                for (Setting<?> setting : module.getSettings()) {
                    Object value = setting.get();
                    if (value instanceof Number number) settings.addProperty(setting.getId(), number);
                    else if (value instanceof Boolean flag) settings.addProperty(setting.getId(), flag);
                    else if (value != null) settings.addProperty(setting.getId(), value.toString());
                }
                data.add("settings", settings);

                if (module instanceof BlockESP be) {
                    JsonArray blocks = new JsonArray();
                    for (BlockESP.BlockEntry entry : be.blocks) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("id", entry.id);
                        obj.addProperty("color", entry.colorIndex);
                        blocks.add(obj);
                    }
                    data.add("blocks", blocks);
                }
                if (module instanceof ESP esp) {
                    JsonArray entities = new JsonArray();
                    for (ESP.EntityEntry entry : esp.customEntities) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("id", entry.id);
                        obj.addProperty("color", entry.colorIndex);
                        entities.add(obj);
                    }
                    data.add("entities", entities);
                }

                modules.add(module.getId(), data);
            }
            root.add("modules", modules);
            root.addProperty("lang", Lang.isChinese ? "zh_cn" : "en_us");

            Path path = path();
            Files.createDirectories(path.getParent());
            Files.writeString(path, root.toString());
        } catch (Exception ignored) {}
    }

    private static void restore(Setting<?> setting, JsonElement element) {
        try {
            if (setting instanceof FloatSetting fs) {
                float v = element.getAsFloat();
                if (fs.getId().equals("aimassist.speed") && v < 100f) v = 400f;
                fs.setClamped(v);
            } else if (setting instanceof BoolSetting bs) {
                bs.set(element.getAsBoolean());
            } else if (setting instanceof EnumSetting es) {
                int index = element.getAsInt();
                if (index >= 0 && index < es.getOptions().length) es.set(index);
            }
        } catch (Exception ignored) {}
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static float num(JsonObject parent, String key, float fallback) {
        try { return parent.has(key) ? parent.get(key).getAsFloat() : fallback; } catch (Exception e) { return fallback; }
    }

    private static int integer(JsonObject parent, String key, int fallback) {
        try { return parent.has(key) ? parent.get(key).getAsInt() : fallback; } catch (Exception e) { return fallback; }
    }

    private static boolean bool(JsonObject parent, String key, boolean fallback) {
        try { return parent.has(key) ? parent.get(key).getAsBoolean() : fallback; } catch (Exception e) { return fallback; }
    }

    private static String string(JsonObject parent, String key, String fallback) {
        try { return parent.has(key) ? parent.get(key).getAsString() : fallback; } catch (Exception e) { return fallback; }
    }
}
