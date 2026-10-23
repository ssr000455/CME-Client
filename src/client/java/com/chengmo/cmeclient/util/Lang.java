package com.chengmo.cmeclient.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class Lang {
    public static boolean isChinese = true;
    private static final Map<String, String> map = new HashMap<>();

    private Lang() {}

    public static void load() {
        map.clear();
        String file = isChinese ? "zh_cn.json" : "en_us.json";
        try (InputStream is = Lang.class.getResourceAsStream("/assets/cmeclient/lang/" + file)) {
            if (is != null) {
                JsonObject obj = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
                for (String key : obj.keySet()) {
                    map.put(key, obj.get(key).getAsString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String get(String key, Object... args) {
        String s = map.getOrDefault(key, key);
        if (args.length > 0) {
            try { s = String.format(s, args); } catch (Exception ignored) {}
        }
        return s;
    }

    public static String category(String catName) {
        return get("cmeclient.category." + catName);
    }

    public static String moduleName(String moduleId) {
        return get("cmeclient.module." + moduleId + ".name");
    }

    public static String moduleDesc(String moduleId) {
        return get("cmeclient.module." + moduleId + ".description");
    }

    /** Enum option label, e.g. cmeclient.setting.godmode.mode.0. */
    public static String option(String settingKey, int index, String fallback) {
        String translated = map.get(settingKey + "." + index);
        return translated != null ? translated : fallback;
    }
}
