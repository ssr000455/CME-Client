package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.setting.BoolSetting;
import com.chengmo.cmeclient.module.setting.FloatSetting;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用方块透视：用户可自由添加原版/模组方块，每个方块可单独配置颜色，
 * 支持全局追踪线开关和扫描距离。
 */
public class BlockESP extends Module {
    public static final float[][] PRESET_COLORS = {
            {0.20f, 0.90f, 1.00f}, // 青
            {1.00f, 0.30f, 0.30f}, // 红
            {0.30f, 1.00f, 0.30f}, // 绿
            {1.00f, 0.90f, 0.20f}, // 黄
            {0.80f, 0.40f, 1.00f}, // 紫
            {1.00f, 0.60f, 0.20f}, // 橙
            {1.00f, 1.00f, 1.00f}, // 白
    };

    public static class BlockEntry {
        public String id;
        public int colorIndex;
        public BlockEntry(String id, int colorIndex) {
            this.id = id;
            this.colorIndex = colorIndex;
        }
        public float[] color() {
            return PRESET_COLORS[Math.floorMod(colorIndex, PRESET_COLORS.length)];
        }
        public void nextColor() {
            colorIndex = (colorIndex + 1) % PRESET_COLORS.length;
        }
    }

    public final FloatSetting range = addSetting(new FloatSetting("blockesp.range", 32.0f, 8.0f, 64.0f));
    public final BoolSetting tracers = addSetting(new BoolSetting("blockesp.tracers", false));
    public final FloatSetting thickness = addSetting(new FloatSetting("blockesp.thickness", 1.5f, 0.5f, 4.0f));

    public final List<BlockEntry> blocks = new ArrayList<>();

    public BlockESP() {
        super("BlockESP", Category.RENDER);
        blocks.add(new BlockEntry("minecraft:diamond_ore", 0));
        blocks.add(new BlockEntry("minecraft:deepslate_diamond_ore", 0));
    }
}
