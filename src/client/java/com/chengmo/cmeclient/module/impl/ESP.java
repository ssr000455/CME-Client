package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.setting.BoolSetting;
import com.chengmo.cmeclient.module.setting.FloatSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体透视：3D 边框高亮。支持预设分类，也支持自定义实体列表。
 * 当 customEntities 非空时，优先使用自定义列表（忽略预设分类）。
 */
public class ESP extends Module {
    public static final float[][] PRESET_COLORS = {
            {0.20f, 0.90f, 1.00f},
            {1.00f, 0.30f, 0.30f},
            {0.30f, 1.00f, 0.30f},
            {1.00f, 0.90f, 0.20f},
            {0.80f, 0.40f, 1.00f},
            {1.00f, 0.60f, 0.20f},
            {1.00f, 1.00f, 1.00f},
    };

    public static class EntityEntry {
        public String id;
        public int colorIndex;
        public EntityEntry(String id, int colorIndex) { this.id = id; this.colorIndex = colorIndex; }
        public float[] color() { return PRESET_COLORS[Math.floorMod(colorIndex, PRESET_COLORS.length)]; }
        public void nextColor() { colorIndex = (colorIndex + 1) % PRESET_COLORS.length; }
    }

    public final FloatSetting range = addSetting(new FloatSetting("esp.range", 64.0f, 16.0f, 128.0f));
    public final BoolSetting names = addSetting(new BoolSetting("esp.names", true));
    public final BoolSetting health = addSetting(new BoolSetting("esp.health", false));
    public final BoolSetting tracers = addSetting(new BoolSetting("esp.tracers", false));
    public final BoolSetting showInvisible = addSetting(new BoolSetting("esp.show_invisible", false));

    public final BoolSetting players = addSetting(new BoolSetting("esp.players", true));
    public final BoolSetting mobs = addSetting(new BoolSetting("esp.mobs", true));
    public final BoolSetting animals = addSetting(new BoolSetting("esp.animals", false));
    public final BoolSetting items = addSetting(new BoolSetting("esp.items", false));

    public final List<EntityEntry> customEntities = new ArrayList<>();

    public ESP() {
        super("ESP", Category.RENDER);
    }
}
