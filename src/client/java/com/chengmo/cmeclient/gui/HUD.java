package com.chengmo.cmeclient.gui;

import com.chengmo.cmeclient.gui.component.Theme;
import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.MiniMap;
import com.chengmo.cmeclient.util.ConfigManager;
import com.chengmo.cmeclient.util.Lang;
import com.chengmo.cmeclient.util.UiUtil;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HUD {
    public static boolean hudEnabled = true;
    public static boolean showWatermark = true;
    public static boolean showArrayList = true;
    public static boolean showInfo = true;
    public static boolean showFps = true;
    public static boolean showPing = true;
    public static boolean showCoords = true;
    public static boolean showEffects = true;
    public static boolean showDurability = true;
    public static float hudTextScale = 1f;
    public static boolean hudBackground = true;
    public static boolean hudRightAlign = false;

    public static float watermarkX = 0.012f;
    public static float watermarkY = 0.016f;
    public static float arrayRight = 0.988f;
    public static float arrayTop = 0.016f;
    public static float infoX = 0.012f;
    public static float infoY = 0.905f;

    public static boolean layoutMode = false;

    private static final Map<String, Float> arrayAnims = new HashMap<>();
    private static int dragTarget = 0;
    private static float dragOffX, dragOffY;

    private static final class Rect {
        float x1, y1, x2, y2;
        Rect(float x1, float y1, float x2, float y2) { this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; }
        boolean contains(float px, float py) { return px >= x1 && px <= x2 && py >= y1 && py <= y2; }
    }

    public static void render() {
        if (!hudEnabled || !ImGuiManager.isInitialized()) return;
        if (!ClickGUI.isOpen) layoutMode = false;

        ImDrawList dl = ImGui.getForegroundDrawList();
        ImFont font = ImGui.getFont();
        MinecraftClient mc = MinecraftClient.getInstance();

        float screenW = ImGui.getIO().getDisplaySizeX();
        float screenH = ImGui.getIO().getDisplaySizeY();
        float hs = UiUtil.clamp(screenH / 720f, 0.72f, 1.5f);
        float baseAlpha = layoutMode ? 1f : (ClickGUI.isOpen ? 0.45f : 1f);

        Rect rWm = showWatermark ? drawWatermark(dl, font, screenW, screenH, hs, baseAlpha) : null;
        Rect rArray = showArrayList ? drawArrayList(dl, font, mc, screenW, screenH, hs, baseAlpha) : null;
        Rect rInfo = showInfo ? drawInfo(dl, font, mc, screenW, screenH, hs, baseAlpha) : null;

        // 渲染 MiniMap
        Module miniMapMod = ModuleManager.INSTANCE.findById("MiniMap");
        if (miniMapMod instanceof MiniMap miniMap && miniMap.isEnabled()) {
            float mapSize = miniMap.size.get();
            float mapX = screenW - mapSize - 20;
            float mapY = screenH / 2f - mapSize / 2f - 100;
            miniMap.render(dl, font, mapX + mapSize / 2f, mapY + mapSize / 2f);
        }

        if (layoutMode) {
            if (rWm != null) markLayout(dl, font, rWm, hs, Lang.get("cmeclient.gui.hud.watermark"));
            if (rArray != null) markLayout(dl, font, rArray, hs, Lang.get("cmeclient.gui.hud.arraylist"));
            if (rInfo != null) markLayout(dl, font, rInfo, hs, Lang.get("cmeclient.gui.hud.info"));
            handleDrag(rWm, 1, screenW, screenH);
            handleDrag(rArray, 2, screenW, screenH);
            handleDrag(rInfo, 3, screenW, screenH);
            drawGuides(dl, screenW, screenH, hs);
            drawLayoutHelp(dl, font, screenW, hs);
        }
    }

    private static Rect drawWatermark(ImDrawList dl, ImFont font, float screenW, float screenH, float hs, float alpha) {
        String title = Lang.get("cmeclient.title");
        String sub = Lang.get("cmeclient.version");
        float titleSize = 16f * hs;
        float subSize = 11f * hs;
        float padX = 13f * hs;
        float textW = UiUtil.textWidth(title, titleSize);
        float subW = UiUtil.textWidth(sub, subSize);
        float dot = 7f * hs;
        float boxW = padX + dot + 9f * hs + textW + 10f * hs + subW + padX;
        float boxH = 36f * hs;

        float x = watermarkX * screenW;
        float y = watermarkY * screenH;
        x = UiUtil.clamp(x, 0f, Math.max(0f, screenW - boxW));
        y = UiUtil.clamp(y, 0f, Math.max(0f, screenH - boxH));

        dl.addRectFilled(x + 2f * hs, y + 4f * hs, x + boxW + 2f * hs, y + boxH + 4f * hs,
                ImGui.getColorU32(0, 0, 0, 0.28f * alpha), 10f * hs);
        dl.addRectFilled(x, y, x + boxW, y + boxH,
                Theme.withAlpha(Theme.COLOR_PANEL_BG, 0.86f * alpha), 10f * hs);
        dl.addRect(x, y, x + boxW, y + boxH,
                Theme.withAlpha(Theme.COLOR_ACCENT, 0.55f * alpha), 10f * hs, 0, 1.2f * hs);

        float pulse = 0.72f + 0.28f * (float) Math.sin(System.currentTimeMillis() / 420.0);
        float cx = x + padX + dot / 2f;
        float cy = y + boxH / 2f;
        dl.addCircleFilled(cx, cy, dot * 2.4f, Theme.withAlpha(Theme.COLOR_ACCENT, 0.14f * alpha));
        dl.addCircleFilled(cx, cy, dot / 2f, Theme.withAlpha(Theme.COLOR_ACCENT, pulse * alpha));

        float tx = x + padX + dot + 9f * hs;
        dl.addText(font, titleSize, tx, y + 8f * hs, Theme.withAlpha(Theme.COLOR_TEXT, alpha), title);
        dl.addText(font, subSize, tx + textW + 10f * hs, y + 17f * hs,
                Theme.withAlpha(Theme.COLOR_TEXT_DIM, alpha), sub);

        return new Rect(x, y, x + boxW, y + boxH);
    }

    private static Rect drawArrayList(ImDrawList dl, ImFont font, MinecraftClient mc, float screenW, float screenH, float hs, float alpha) {
        float scale = UiUtil.clamp(hudTextScale, 0.8f, 1.3f);
        float itemH = 22f * hs * scale;
        float gap = 2f * hs;
        float textSize = 14f * hs * scale;

        List<Module> modules = ModuleManager.INSTANCE.getModules();
        List<String> names = new ArrayList<>();
        List<Float> widths = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (Module m : modules) {
            float anim = arrayAnims.getOrDefault(m.getId(), 0f);
            float target = m.isEnabled() ? 1f : 0f;
            anim = anim + (target - anim) * (1f - (float) Math.exp(-16f * Math.min(ImGui.getIO().getDeltaTime(), 0.1f)));
            if (anim < 0.01f) {
                arrayAnims.remove(m.getId());
                continue;
            }
            arrayAnims.put(m.getId(), anim);
            names.add(m.getName());
            widths.add(UiUtil.textWidth(m.getName(), textSize));
            colors.add(Theme.categoryColor(m.getCategory()));
        }
        if (names.isEmpty()) return null;

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) order.add(i);
        order.sort((a, b) -> Float.compare(widths.get(b), widths.get(a)));

        float right = arrayRight * screenW;
        float top = arrayTop * screenH;
        float maxW = 0f;
        for (Integer i : order) maxW = Math.max(maxW, widths.get(i) + 18f * hs);
        right = UiUtil.clamp(right, maxW + 4f * hs, screenW - 4f * hs);
        top = UiUtil.clamp(top, 0f, Math.max(0f, screenH - order.size() * (itemH + gap)));

        float y = top;
        float totalW = 0f;
        for (Integer i : order) {
            float w = (widths.get(i) + 18f * hs);
            float x2 = right;
            float x1 = x2 - w;
            if (hudBackground) {
                int bg = Theme.withAlpha(Theme.COLOR_PANEL_BG, 0.62f * alpha);
                dl.addRectFilled(x1, y, x2, y + itemH, bg, 4f * hs);
            }
            dl.addRectFilled(x2 - 3f * hs, y + 3f * hs, x2, y + itemH - 3f * hs,
                    Theme.withAlpha(colors.get(i), alpha), 1.5f * hs);
            dl.addText(font, textSize, x1 + 9f * hs, y + (itemH - textSize) / 2f - 1f * hs,
                    Theme.withAlpha(Theme.COLOR_TEXT, alpha), names.get(i));
            totalW = Math.max(totalW, w);
            y += itemH + gap;
        }

        if (layoutMode) {
            dl.addRect(right - totalW, top, right, y - gap,
                    Theme.withAlpha(Theme.COLOR_ACCENT, 0.65f), 4f * hs, 0, 1.2f * hs);
        }
        return new Rect(right - totalW, top, right, y - gap);
    }

    private static Rect drawInfo(ImDrawList dl, ImFont font, MinecraftClient mc, float screenW, float screenH, float hs, float alpha) {
        float scale = UiUtil.clamp(hudTextScale, 0.8f, 1.3f);
        float textSize = 13f * hs * scale;
        float lineH = 17f * hs * scale;

        List<String> lines = new ArrayList<>();
        if (showFps) lines.add(Lang.get("cmeclient.hud.fps") + " " + mc.getCurrentFps());
        if (showPing) {
            String ping = "-";
            if (mc.player != null && mc.getNetworkHandler() != null) {
                PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) ping = entry.getLatency() + "ms";
            }
            lines.add(Lang.get("cmeclient.hud.ping") + " " + ping);
        }
        if (showCoords && mc.player != null) {
            lines.add(String.format("X %.1f  Y %.1f  Z %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        }
        if (showEffects && mc.player != null) {
            int shown = 0;
            for (var effect : mc.player.getStatusEffects()) {
                if (shown++ >= 4) break;
                String name = effect.getEffectType().getName().getString();
                lines.add(name + "  " + (effect.getDuration() / 20) + "s");
            }
        }
        if (showDurability && mc.player != null) {
            var stack = mc.player.getMainHandStack();
            if (stack.isDamageable() && stack.getMaxDamage() > 0) {
                int pct = Math.round((stack.getMaxDamage() - stack.getDamage()) * 100f / stack.getMaxDamage());
                lines.add(Lang.get("cmeclient.hud.durability") + " " + pct + "%");
            }
        }
        if (lines.isEmpty()) return null;

        float padX = 11f * hs;
        float padY = 8f * hs;
        float boxW = 0f;
        for (String line : lines) boxW = Math.max(boxW, UiUtil.textWidth(line, textSize));
        boxW += padX * 2f + 6f * hs;
        float boxH = padY * 2f + lineH * lines.size();

        float x = infoX * screenW;
        float y = infoY * screenH;
        if (hudRightAlign) x -= boxW;
        x = UiUtil.clamp(x, 0f, Math.max(0f, screenW - boxW));
        y = UiUtil.clamp(y, 0f, Math.max(0f, screenH - boxH));

        if (hudBackground) {
            dl.addRectFilled(x, y, x + boxW, y + boxH,
                    Theme.withAlpha(Theme.COLOR_PANEL_BG, 0.72f * alpha), 8f * hs);
            dl.addRect(x, y, x + boxW, y + boxH,
                    Theme.withAlpha(Theme.COLOR_CARD_BORDER, 0.8f * alpha), 8f * hs, 0, 1f * hs);
            dl.addRectFilled(x, y + 6f * hs, x + 3f * hs, y + boxH - 6f * hs,
                    Theme.withAlpha(Theme.COLOR_ACCENT, alpha), 1.5f * hs);
        }

        float tx = x + padX + 4f * hs;
        for (int i = 0; i < lines.size(); i++) {
            dl.addText(font, textSize, tx, y + padY + lineH * i,
                    Theme.withAlpha(i == 0 ? Theme.COLOR_TEXT : Theme.COLOR_TEXT_DIM, alpha), lines.get(i));
        }
        return new Rect(x, y, x + boxW, y + boxH);
    }

    private static void markLayout(ImDrawList dl, ImFont font, Rect r, float hs, String label) {
        int col = Theme.withAlpha(Theme.COLOR_ACCENT, 0.8f);
        float x1 = r.x1 - 3f * hs;
        float y1 = r.y1 - 3f * hs;
        float x2 = r.x2 + 3f * hs;
        float y2 = r.y2 + 3f * hs;

        float dash = 6f * hs;
        float step = dash * 2f;
        for (float x = x1; x < x2; x += step) {
            dl.addLine(x, y1, Math.min(x + dash, x2), y1, col, 1.2f * hs);
            dl.addLine(x, y2, Math.min(x + dash, x2), y2, col, 1.2f * hs);
        }
        for (float y = y1; y < y2; y += step) {
            dl.addLine(x1, y, x1, Math.min(y + dash, y2), col, 1.2f * hs);
            dl.addLine(x2, y, x2, Math.min(y + dash, y2), col, 1.2f * hs);
        }

        float labelSize = 11f * hs;
        float lw = UiUtil.textWidth(label, labelSize);
        float lx = UiUtil.clamp((x1 + x2 - lw) / 2f, 0f, Math.max(0f, ImGui.getIO().getDisplaySizeX() - lw));
        float ly = Math.max(2f * hs, y1 - labelSize - 5f * hs);
        dl.addRectFilled(lx - 4f * hs, ly - 2f * hs, lx + lw + 4f * hs, ly + labelSize + 2f * hs,
                Theme.withAlpha(Theme.COLOR_PANEL_BG, 0.85f), 4f * hs);
        dl.addText(font, labelSize, lx, ly, Theme.withAlpha(Theme.COLOR_ACCENT, 0.95f), label);
    }

    private static void drawGuides(ImDrawList dl, float screenW, float screenH, float hs) {
        if (dragTarget == 0) return;
        int col = Theme.withAlpha(Theme.COLOR_ACCENT, 0.35f);
        float cx = screenW / 2f;
        float cy = screenH / 2f;
        float dash = 8f * hs;
        for (float y = 0f; y < screenH; y += dash * 2f) {
            dl.addLine(cx, y, cx, Math.min(y + dash, screenH), col, 1f * hs);
        }
        for (float x = 0f; x < screenW; x += dash * 2f) {
            dl.addLine(x, cy, Math.min(x + dash, screenW), cy, col, 1f * hs);
        }
    }

    private static void handleDrag(Rect r, int target, float screenW, float screenH) {
        if (r == null) return;
        float mx = ImGui.getMousePosX();
        float my = ImGui.getMousePosY();

        if (dragTarget == 0 && r.contains(mx, my) && ImGui.isMouseClicked(0)) {
            dragTarget = target;
            dragOffX = mx - r.x1;
            dragOffY = my - r.y1;
        }
        if (dragTarget != target) return;

        if (ImGui.isMouseDown(0)) {
            float px = snap(mx - dragOffX, screenW);
            float py = snap(my - dragOffY, screenH);
            float nx = px / screenW;
            float ny = py / screenH;
            switch (target) {
                case 1 -> { watermarkX = UiUtil.clamp(nx, 0f, 0.98f); watermarkY = UiUtil.clamp(ny, 0f, 0.96f); }
                case 2 -> { arrayRight = UiUtil.clamp(snap(mx + (r.x2 - r.x1) - dragOffX, screenW) / screenW, 0.05f, 1f); arrayTop = UiUtil.clamp(ny, 0f, 0.96f); }
                case 3 -> { infoX = UiUtil.clamp((hudRightAlign ? mx + (r.x2 - r.x1) - dragOffX : px) / screenW, 0f, 0.98f); infoY = UiUtil.clamp(ny, 0f, 0.96f); }
                default -> { }
            }
            ConfigManager.markDirty();
        } else {
            dragTarget = 0;
        }
    }

    private static float snap(float value, float total) {
        float threshold = 10f;
        float center = total / 2f;
        if (Math.abs(value - center) < threshold) return center;
        if (Math.abs(value) < threshold) return 0f;
        if (Math.abs(value - total) < threshold) return total;
        return value;
    }

    private static void drawLayoutHelp(ImDrawList dl, ImFont font, float screenW, float hs) {
        String hint = Lang.get("cmeclient.hud.layout_hint");
        String done = Lang.get("cmeclient.hud.done");
        String reset = Lang.get("cmeclient.hud.reset");

        float textSize = 13f * hs;
        float pad = 12f * hs;
        float btnW = 74f * hs;
        float btnH = 30f * hs;
        float hintW = UiUtil.textWidth(hint, textSize);
        float boxW = hintW + btnW * 2f + pad * 4f;
        float x = (screenW - boxW) / 2f;
        float y = 14f * hs;

        dl.addRectFilled(x, y, x + boxW, y + btnH + pad, Theme.withAlpha(Theme.COLOR_PANEL_BG, 0.94f), 10f * hs);
        dl.addRect(x, y, x + boxW, y + btnH + pad, Theme.withAlpha(Theme.COLOR_ACCENT, 0.7f), 10f * hs, 0, 1.2f * hs);
        dl.addText(font, textSize, x + pad, y + (btnH + pad - textSize) / 2f - 1f * hs, Theme.COLOR_TEXT, hint);

        float rx = x + pad + hintW + pad;
        float ry = y + pad / 2f;
        boolean resetHover = ImGui.isMouseHoveringRect(rx, ry, rx + btnW, ry + btnH);
        dl.addRectFilled(rx, ry, rx + btnW, ry + btnH,
                resetHover ? Theme.withAlpha(Theme.COLOR_ACCENT, 0.35f) : ImGui.getColorU32(1f, 1f, 1f, 0.08f), 7f * hs);
        dl.addText(font, 12f * hs, rx + (btnW - UiUtil.textWidth(reset, 12f * hs)) / 2f, ry + 8f * hs, Theme.COLOR_TEXT, reset);

        float dx = rx + btnW + pad * 0.5f;
        boolean doneHover = ImGui.isMouseHoveringRect(dx, ry, dx + btnW, ry + btnH);
        dl.addRectFilled(dx, ry, dx + btnW, ry + btnH,
                doneHover ? Theme.withAlpha(Theme.COLOR_ACCENT, 0.85f) : Theme.withAlpha(Theme.COLOR_ACCENT, 0.60f), 7f * hs);
        dl.addText(font, 12f * hs, dx + (btnW - UiUtil.textWidth(done, 12f * hs)) / 2f, ry + 8f * hs, ImGui.getColorU32(0.08f, 0.06f, 0.10f, 1f), done);

        if (resetHover && ImGui.isMouseClicked(0)) {
            watermarkX = 0.012f; watermarkY = 0.016f;
            arrayRight = 0.988f; arrayTop = 0.016f;
            infoX = 0.012f; infoY = 0.905f;
            ConfigManager.markDirty();
        }
        if (doneHover && ImGui.isMouseClicked(0)) {
            layoutMode = false;
            ConfigManager.markDirty();
        }
    }
}
