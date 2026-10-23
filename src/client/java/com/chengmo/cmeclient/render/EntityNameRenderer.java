package com.chengmo.cmeclient.render;

import com.chengmo.cmeclient.gui.EntityESPManager;
import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.ESP;
import com.chengmo.cmeclient.util.ScreenProjector;
import com.chengmo.cmeclient.util.UiUtil;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import net.minecraft.client.MinecraftClient;

public final class EntityNameRenderer {
    private EntityNameRenderer() {}

    public static void render() {
        ESP mod = (ESP) ModuleManager.INSTANCE.findById("ESP");
        if (mod == null || !mod.isEnabled()) return;
        if (!mod.names.get()) return;

        var found = EntityESPManager.getFound();
        if (found.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;

        int fbW = mc.getWindow().getFramebufferWidth();
        int fbH = mc.getWindow().getFramebufferHeight();
        float fov = mc.options.getFov().getValue().floatValue();

        ImDrawList dl = ImGui.getForegroundDrawList();
        ImFont font = ImGui.getFont();
        float s = Math.max(0.75f, Math.min(1.5f, fbH / 720f));
        float textSize = 13f * s;

        for (var box : found) {
            if (box.name == null || box.name.isEmpty()) continue;

            float wx = box.cx;
            float wy = box.cy + box.height + 0.35f;
            float wz = box.cz;

            float[] pos = ScreenProjector.worldToScreen(wx, wy, wz, fov, fbW, fbH);
            if (pos == null) continue;
            if (pos[0] < -200 || pos[0] > fbW + 200 || pos[1] < -50 || pos[1] > fbH + 50) continue;

            String name = box.name;
            float textW = UiUtil.textWidth(name, textSize);
            float tx = pos[0] - textW / 2f;
            float ty = pos[1] - textSize / 2f;

            int outline = ImGui.getColorU32(0f, 0f, 0f, 0.85f);
            int mainColor = box.argb;
            dl.addText(font, textSize, tx - 1f, ty, outline, name);
            dl.addText(font, textSize, tx + 1f, ty, outline, name);
            dl.addText(font, textSize, tx, ty - 1f, outline, name);
            dl.addText(font, textSize, tx, ty + 1f, outline, name);
            dl.addText(font, textSize, tx, ty, mainColor, name);
        }
    }
}
