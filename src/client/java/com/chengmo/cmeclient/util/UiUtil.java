package com.chengmo.cmeclient.util;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared text helpers. ImGui draws text at an arbitrary font size, but
 * {@link ImGui#calcTextSize(String)} always measures with the current base
 * font size, so every measurement used for layout must be rescaled.
 */
public final class UiUtil {
    private UiUtil() {}

    public static float textWidth(String text, float fontSize) {
        if (text == null || text.isEmpty()) return 0f;
        float base = ImGui.getFontSize();
        if (base <= 0f) return 0f;
        return ImGui.calcTextSize(text).x * fontSize / base;
    }

    /** Truncates text with an ellipsis so that it fits into maxWidth. */
    public static String fit(String text, float maxWidth, float fontSize) {
        if (text == null) return "";
        if (maxWidth <= 0f || textWidth(text, fontSize) <= maxWidth) return text;
        String ellipsis = "...";
        float ellipsisW = textWidth(ellipsis, fontSize);
        if (ellipsisW > maxWidth) return "";
        int lo = 0, hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (textWidth(text.substring(0, mid), fontSize) + ellipsisW <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return text.substring(0, lo) + ellipsis;
    }

    /** Simple character based wrapping, keeps existing line breaks. */
    public static List<String> wrap(String text, float maxWidth, float fontSize) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < paragraph.length(); i++) {
                char c = paragraph.charAt(i);
                String candidate = line.toString() + c;
                if (line.length() > 0 && textWidth(candidate, fontSize) > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(c);
            }
            if (line.length() > 0) lines.add(line.toString());
        }
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    /**
     * Applies native ImGui metrics for the current logical UI scale. Called every
     * frame because the scale changes with resolution and the uiScale setting.
     * Keeping FramePadding/ItemSpacing in the same units as the custom layout is
     * what prevents native widgets from spilling into neighbouring rows.
     */
    public static void applyStyle(float s) {
        if (s <= 0f) return;
        ImGuiStyle st = ImGui.getStyle();
        st.setWindowPadding(0f, 0f);
        st.setFramePadding(10f * s, 6f * s);
        st.setItemSpacing(8f * s, 8f * s);
        st.setItemInnerSpacing(6f * s, 6f * s);
        st.setTouchExtraPadding(4f * s, 4f * s);
        st.setWindowRounding(12f * s);
        st.setChildRounding(10f * s);
        st.setFrameRounding(8f * s);
        st.setPopupRounding(10f * s);
        st.setGrabRounding(8f * s);
        st.setScrollbarRounding(6f * s);
        st.setTabRounding(8f * s);
        st.setScrollbarSize(10f * s);
        st.setGrabMinSize(12f * s);
        st.setWindowBorderSize(0f);
        st.setChildBorderSize(0f);
        st.setPopupBorderSize(1f * s);
        st.setFrameBorderSize(0f);
        st.setTabBorderSize(0f);
        st.setButtonTextAlign(0.5f, 0.5f);
        // 修复关键点 2：移除了 setFontGlobalScale，强制依赖绝对尺寸，防止 GLES 坐标溢出
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}