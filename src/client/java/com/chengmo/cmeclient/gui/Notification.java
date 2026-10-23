package com.chengmo.cmeclient.gui;

import com.chengmo.cmeclient.gui.component.Theme;
import com.chengmo.cmeclient.util.UiUtil;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;

import java.util.ArrayList;
import java.util.List;

public class Notification {
    public enum Type { INFO, SUCCESS, WARN, ERROR }

    private static class Toast {
        String text;
        Type type;
        long startTime;
        Toast(String text, Type type) { this.text = text; this.type = type; this.startTime = System.currentTimeMillis(); }
    }

    private static final List<Toast> toasts = new ArrayList<>();
    private static final long DURATION = 2600;
    private static final int MAX_TOASTS = 5;

    public static void info(String t)    { add(new Toast(t, Type.INFO)); }
    public static void success(String t) { add(new Toast(t, Type.SUCCESS)); }
    public static void warn(String t)    { add(new Toast(t, Type.WARN)); }
    public static void error(String t)   { add(new Toast(t, Type.ERROR)); }
    public static void show(String t)    { success(t); }

    private static void add(Toast toast) {
        toasts.add(toast);
        while (toasts.size() > MAX_TOASTS) toasts.remove(0);
    }

    private static int dotColor(Type t, float a) {
        return switch (t) {
            case INFO    -> ImGui.getColorU32(0.35f, 0.68f, 1.00f, a);
            case SUCCESS -> ImGui.getColorU32(0.30f, 0.80f, 0.40f, a);
            case WARN    -> ImGui.getColorU32(0.95f, 0.75f, 0.20f, a);
            case ERROR   -> ImGui.getColorU32(0.90f, 0.25f, 0.25f, a);
        };
    }

    /** Kept for compatibility; notifications are now drawn on the foreground list. */
    public static void render(ImDrawList drawList, float screenW, float screenH) {
        render(screenW, screenH);
    }

    public static void render(float screenW, float screenH) {
        if (!ImGuiManager.isInitialized()) return;
        long now = System.currentTimeMillis();
        float hs = Math.max(0.75f, Math.min(1.4f, screenH / 720f));
        float textSize = 15f * hs;
        float lineH = 20f * hs;
        float padX = 16f * hs;
        float padY = 12f * hs;
        float boxW = Math.min(320f * hs, screenW - 32f * hs);
        float textMax = boxW - padX * 2f - 26f * hs;
        float gap = 8f * hs;

        ImDrawList dl = ImGui.getForegroundDrawList();
        ImFont font = ImGui.getFont();
        float cursorY = screenH - 22f * hs;

        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast toast = toasts.get(i);
            long elapsed = now - toast.startTime;
            float alpha, slideX;
            if (elapsed < 250) {
                float t = elapsed / 250f; alpha = t; slideX = 120f * hs * (1f - t);
            } else if (elapsed > DURATION - 250) {
                float t = (elapsed - (DURATION - 250)) / 250f; alpha = 1f - t; slideX = 120f * hs * t;
            } else { alpha = 1f; slideX = 0f; }
            if (alpha <= 0.01f) continue;

            List<String> lines = UiUtil.wrap(toast.text, textMax, textSize);
            if (lines.size() > 3) lines = lines.subList(0, 3);
            float boxH = Math.max(46f * hs, lines.size() * lineH + padY * 2f);
            float x = screenW - boxW - 18f * hs + slideX;
            float y = cursorY - boxH;
            cursorY = y - gap;
            float radius = Theme.r(10f, hs);

            float mouseX = ImGui.getMousePosX();
            float mouseY = ImGui.getMousePosY();
            boolean hovering = mouseX >= x && mouseX <= x + boxW && mouseY >= y && mouseY <= y + boxH;
            if (hovering && ImGui.isMouseClicked(0)) {
                toasts.remove(i);
                continue;
            }

            int bg = Theme.withAlpha(Theme.COLOR_PANEL_BG, 0.96f * alpha);
            int border = Theme.withAlpha(Theme.COLOR_ACCENT, 0.65f * alpha);
            dl.addRectFilled(x + 3f * hs, y + 4f * hs, x + boxW + 3f * hs, y + boxH + 4f * hs,
                    ImGui.getColorU32(0f, 0f, 0f, 0.35f * alpha), radius);
            dl.addRectFilled(x, y, x + boxW, y + boxH, bg, radius);
            dl.addRect(x, y, x + boxW, y + boxH, border, radius, 0, 1.2f * hs);

            if (hovering) {
                float cx = x + boxW - 14f * hs;
                float cy = y + 14f * hs;
                int closeCol = ImGui.getColorU32(1f, 1f, 1f, 0.75f * alpha);
                dl.addLine(cx - 3f * hs, cy - 3f * hs, cx + 3f * hs, cy + 3f * hs, closeCol, 1.4f * hs);
                dl.addLine(cx + 3f * hs, cy - 3f * hs, cx - 3f * hs, cy + 3f * hs, closeCol, 1.4f * hs);
            }

            int accent = dotColor(toast.type, alpha);
            dl.addRectFilled(x, y + 8f * hs, x + 3f * hs, y + boxH - 8f * hs, accent, 2f * hs);

            float iconX = x + padX + 4f * hs;
            float iconY = y + boxH / 2f;
            float iconR = 8f * hs;
            dl.addCircleFilled(iconX, iconY, iconR, dotColor(toast.type, 0.20f * alpha));
            if (toast.type == Type.SUCCESS) {
                dl.addLine(iconX - 3.5f * hs, iconY, iconX - 1f * hs, iconY + 2.5f * hs, accent, 1.6f * hs);
                dl.addLine(iconX - 1f * hs, iconY + 2.5f * hs, iconX + 4f * hs, iconY - 3f * hs, accent, 1.6f * hs);
            } else if (toast.type == Type.WARN) {
                dl.addLine(iconX, iconY - 4f * hs, iconX, iconY + 1.5f * hs, accent, 1.6f * hs);
                dl.addCircleFilled(iconX, iconY + 4.5f * hs, 1.1f * hs, accent);
            } else if (toast.type == Type.INFO) {
                dl.addCircleFilled(iconX, iconY - 4f * hs, 1.1f * hs, accent);
                dl.addLine(iconX, iconY - 1f * hs, iconX, iconY + 4.5f * hs, accent, 1.6f * hs);
            } else {
                dl.addLine(iconX - 3f * hs, iconY - 3f * hs, iconX + 3f * hs, iconY + 3f * hs, accent, 1.6f * hs);
                dl.addLine(iconX + 3f * hs, iconY - 3f * hs, iconX - 3f * hs, iconY + 3f * hs, accent, 1.6f * hs);
            }

            int textColor = ImGui.getColorU32(0.94f, 0.93f, 0.98f, alpha);
            float textX = x + padX + 22f * hs;
            float textY = y + padY;
            for (String line : lines) {
                dl.addText(font, textSize, textX, textY, textColor, line);
                textY += lineH;
            }
        }
        toasts.removeIf(t -> now - t.startTime > DURATION);
    }
}