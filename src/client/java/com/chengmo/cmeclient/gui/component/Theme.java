package com.chengmo.cmeclient.gui.component;

import com.chengmo.cmeclient.module.Module;
import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;

public class Theme {
    public static int COLOR_PANEL_BG, COLOR_NAV_BG, COLOR_ACCENT, COLOR_TEXT, COLOR_TEXT_DIM;
    public static int COLOR_CARD_BG, COLOR_CARD_BORDER;

    /** GUI-only appearance knobs, persisted by ConfigManager. */
    public static float roundingScale = 1f;
    public static float panelOpacity = 1f;
    public static boolean customAccentEnabled = false;
    public static final float[] customAccent = {0.85f, 0.65f, 0.13f};

    /** Scales a base corner radius with the UI scale. */
    public static float r(float base, float s) {
        return Math.max(0f, base * s * roundingScale);
    }

    private static int scaleAlpha(int packed, float factor) {
        float a = ((packed >> 24) & 0xFF) / 255f;
        return withAlpha(packed, a * factor);
    }

    public static int categoryColor(Module.Category category) {
        return switch (category) {
            case COMBAT -> ImGui.getColorU32(0.95f, 0.38f, 0.42f, 1.0f);
            case MOVEMENT -> ImGui.getColorU32(0.36f, 0.84f, 0.56f, 1.0f);
            case RENDER -> ImGui.getColorU32(0.42f, 0.72f, 1.00f, 1.0f);
            case MISC -> ImGui.getColorU32(0.92f, 0.72f, 0.30f, 1.0f);
            case SINGLEPLAYER -> ImGui.getColorU32(0.60f, 0.85f, 0.55f, 1.0f);
            default -> ImGui.getColorU32(0.62f, 0.62f, 0.68f, 1.0f);
        };
    }

    public static int previewAccent(int themeIndex) {
        return switch (themeIndex) {
            case 0 -> ImGui.getColorU32(0.85f, 0.65f, 0.13f, 1.0f);
            case 1 -> ImGui.getColorU32(0.90f, 0.94f, 1.00f, 0.95f);
            case 2 -> ImGui.getColorU32(0.90f, 0.90f, 0.90f, 1.0f);
            case 3 -> ImGui.getColorU32(0.30f, 0.70f, 1.00f, 1.0f);
            default -> ImGui.getColorU32(1.00f, 0.85f, 0.10f, 1.0f);
        };
    }

    public static int withAlpha(int packed, float alpha) {
        float r = (packed & 0xFF) / 255f;
        float g = ((packed >> 8) & 0xFF) / 255f;
        float b = ((packed >> 16) & 0xFF) / 255f;
        return ImGui.getColorU32(r, g, b, alpha);
    }

    /** Native ImGui widgets (slider/input/checkbox/tabs) follow the active theme. */
    public static void applyNativeStyle() {
        ImGuiStyle style = ImGui.getStyle();
        style.setColor(ImGuiCol.FrameBg, ImGui.getColorU32(1f, 1f, 1f, 0.06f));
        style.setColor(ImGuiCol.FrameBgHovered, ImGui.getColorU32(1f, 1f, 1f, 0.10f));
        style.setColor(ImGuiCol.FrameBgActive, ImGui.getColorU32(1f, 1f, 1f, 0.14f));
        style.setColor(ImGuiCol.Button, ImGui.getColorU32(1f, 1f, 1f, 0.08f));
        style.setColor(ImGuiCol.ButtonHovered, withAlpha(COLOR_ACCENT, 0.42f));
        style.setColor(ImGuiCol.ButtonActive, withAlpha(COLOR_ACCENT, 0.62f));
        style.setColor(ImGuiCol.Header, withAlpha(COLOR_ACCENT, 0.28f));
        style.setColor(ImGuiCol.HeaderHovered, withAlpha(COLOR_ACCENT, 0.44f));
        style.setColor(ImGuiCol.HeaderActive, withAlpha(COLOR_ACCENT, 0.55f));
        style.setColor(ImGuiCol.CheckMark, COLOR_ACCENT);
        style.setColor(ImGuiCol.SliderGrab, withAlpha(COLOR_ACCENT, 0.85f));
        style.setColor(ImGuiCol.SliderGrabActive, COLOR_ACCENT);
        style.setColor(ImGuiCol.ScrollbarBg, ImGui.getColorU32(0f, 0f, 0f, 0.18f));
        style.setColor(ImGuiCol.ScrollbarGrab, withAlpha(COLOR_ACCENT, 0.45f));
        style.setColor(ImGuiCol.ScrollbarGrabHovered, withAlpha(COLOR_ACCENT, 0.65f));
        style.setColor(ImGuiCol.ScrollbarGrabActive, withAlpha(COLOR_ACCENT, 0.90f));
        style.setColor(ImGuiCol.PopupBg, ImGui.getColorU32(0.09f, 0.07f, 0.13f, 0.98f));
        style.setColor(ImGuiCol.Separator, ImGui.getColorU32(1f, 1f, 1f, 0.10f));
        style.setColor(ImGuiCol.TextSelectedBg, withAlpha(COLOR_ACCENT, 0.40f));
        style.setColor(ImGuiCol.Tab, ImGui.getColorU32(1f, 1f, 1f, 0.05f));
        style.setColor(ImGuiCol.TabHovered, withAlpha(COLOR_ACCENT, 0.30f));
        style.setColor(ImGuiCol.TabActive, withAlpha(COLOR_ACCENT, 0.50f));
    }

    public static void apply(int themeIndex, float[] customTextColor) {
        COLOR_TEXT = ImGui.getColorU32(customTextColor[0], customTextColor[1], customTextColor[2], 1.0f);
        
        if (themeIndex == 0) { 
            // 皇家紫金
            COLOR_PANEL_BG = ImGui.getColorU32(0.10f, 0.05f, 0.16f, 0.98f);
            COLOR_NAV_BG = ImGui.getColorU32(0.06f, 0.02f, 0.10f, 1.00f);
            COLOR_ACCENT = ImGui.getColorU32(0.85f, 0.65f, 0.13f, 1.00f);
            COLOR_TEXT_DIM = ImGui.getColorU32(0.60f, 0.55f, 0.65f, 1.00f);
            COLOR_CARD_BG = ImGui.getColorU32(0.16f, 0.10f, 0.22f, 1.00f);
            COLOR_CARD_BORDER = ImGui.getColorU32(0.4f, 0.3f, 0.5f, 0.5f);
        } else if (themeIndex == 1) { 
            // 透水玻璃
            COLOR_PANEL_BG = ImGui.getColorU32(0.15f, 0.18f, 0.22f, 0.55f);
            COLOR_NAV_BG = ImGui.getColorU32(0.10f, 0.12f, 0.15f, 0.40f);
            COLOR_ACCENT = ImGui.getColorU32(1.0f, 1.0f, 1.0f, 0.9f);
            COLOR_TEXT_DIM = ImGui.getColorU32(customTextColor[0]*0.7f, customTextColor[1]*0.7f, customTextColor[2]*0.7f, 1.0f);
            COLOR_CARD_BG = ImGui.getColorU32(1.0f, 1.0f, 1.0f, 0.10f);
            COLOR_CARD_BORDER = ImGui.getColorU32(1.0f, 1.0f, 1.0f, 0.25f);
        } else if (themeIndex == 2) { 
            // 墨白
            COLOR_PANEL_BG = ImGui.getColorU32(0.08f, 0.08f, 0.08f, 0.98f);
            COLOR_NAV_BG = ImGui.getColorU32(0.04f, 0.04f, 0.04f, 1.00f);
            COLOR_ACCENT = ImGui.getColorU32(0.9f, 0.9f, 0.9f, 1.00f);
            COLOR_TEXT_DIM = ImGui.getColorU32(0.50f, 0.50f, 0.50f, 1.00f);
            COLOR_CARD_BG = ImGui.getColorU32(0.14f, 0.14f, 0.14f, 1.00f);
            COLOR_CARD_BORDER = ImGui.getColorU32(0.3f, 0.3f, 0.3f, 0.5f);
        } else if (themeIndex == 3) { 
            // 天蓝
            COLOR_PANEL_BG = ImGui.getColorU32(0.05f, 0.09f, 0.16f, 0.98f);
            COLOR_NAV_BG = ImGui.getColorU32(0.02f, 0.05f, 0.10f, 1.00f);
            COLOR_ACCENT = ImGui.getColorU32(0.3f, 0.7f, 1.0f, 1.00f);
            COLOR_TEXT_DIM = ImGui.getColorU32(0.5f, 0.6f, 0.7f, 1.00f);
            COLOR_CARD_BG = ImGui.getColorU32(0.08f, 0.14f, 0.24f, 1.00f);
            COLOR_CARD_BORDER = ImGui.getColorU32(0.2f, 0.4f, 0.6f, 0.5f);
        } else { 
            // 高对比
            COLOR_PANEL_BG = ImGui.getColorU32(0.01f, 0.01f, 0.01f, 1.00f);
            COLOR_NAV_BG = ImGui.getColorU32(0.00f, 0.00f, 0.00f, 1.00f);
            COLOR_ACCENT = ImGui.getColorU32(1.00f, 0.85f, 0.10f, 1.00f);
            COLOR_TEXT_DIM = ImGui.getColorU32(0.82f, 0.82f, 0.82f, 1.00f);
            COLOR_CARD_BG = ImGui.getColorU32(0.07f, 0.07f, 0.07f, 1.00f);
            COLOR_CARD_BORDER = ImGui.getColorU32(0.90f, 0.90f, 0.90f, 0.85f);
        }

        if (customAccentEnabled) {
            COLOR_ACCENT = ImGui.getColorU32(customAccent[0], customAccent[1], customAccent[2], 1.0f);
        }
        if (panelOpacity < 0.999f) {
            COLOR_PANEL_BG = scaleAlpha(COLOR_PANEL_BG, panelOpacity);
            COLOR_NAV_BG = scaleAlpha(COLOR_NAV_BG, panelOpacity);
            COLOR_CARD_BG = scaleAlpha(COLOR_CARD_BG, panelOpacity);
        }

        applyNativeStyle();
    }
}
