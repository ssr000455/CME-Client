package com.chengmo.cmeclient.gui.component;

import com.chengmo.cmeclient.gui.ClickGUI;
import com.chengmo.cmeclient.gui.HUD;
import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.setting.BoolSetting;
import com.chengmo.cmeclient.module.setting.EnumSetting;
import com.chengmo.cmeclient.module.setting.FloatSetting;
import com.chengmo.cmeclient.module.setting.Setting;
import com.chengmo.cmeclient.util.ConfigManager;
import com.chengmo.cmeclient.util.Lang;
import com.chengmo.cmeclient.util.UiUtil;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import net.minecraft.client.util.InputUtil;

public class SettingPanel {
    public static int settingsTab = 0;
    private static boolean touchScrolling = false;
    private static float touchStartY = 0f;
    private static float touchStartScroll = 0f;
    private static ImDrawList curDl;
    private static ImFont curFont;
    private static float curS = 1f;

    private static final String[] TAB_KEYS = {
            "cmeclient.gui.tab.general",
            "cmeclient.gui.tab.appearance",
            "cmeclient.gui.tab.modules",
            "cmeclient.gui.tab.keybinds",
            "cmeclient.gui.tab.about"
    };

    public static void render(ImDrawList dl, ImFont font, float x, float y, float width, float height, float s, float[] customTextColor) {
        curDl = dl;
        curFont = font;
        curS = s;
        dl.addText(font, 20f * s, x, y, Theme.COLOR_TEXT, Lang.get("cmeclient.gui.settings"));

        float tabY = y + 40 * s;
        int cols = width < 460 * s ? 2 : 5;
        int tabRows = (TAB_KEYS.length + cols - 1) / cols;
        float tabGap = 8 * s;
        float tabW = (width - tabGap * (cols - 1)) / cols;
        float tabH = 30 * s;
        float tabBlockH = tabRows * tabH + (tabRows - 1) * tabGap;

        for (int i = 0; i < TAB_KEYS.length; i++) {
            int row = i / cols;
            int col = i % cols;
            float tabX = x + col * (tabW + tabGap);
            float tabRowY = tabY + row * (tabH + tabGap);
            boolean selected = (settingsTab == i);
            boolean hovered = ImGui.isMouseHoveringRect(tabX, tabRowY, tabX + tabW, tabRowY + tabH);

            if (selected) {
                dl.addRectFilled(tabX, tabRowY, tabX + tabW, tabRowY + tabH, Theme.COLOR_ACCENT, 6f * s);
            } else if (hovered) {
                dl.addRectFilled(tabX, tabRowY, tabX + tabW, tabRowY + tabH, ImGui.getColorU32(0.3f, 0.3f, 0.3f, 0.3f), 6f * s);
            }

            String tabText = UiUtil.fit(Lang.get(TAB_KEYS[i]), Math.max(10 * s, tabW - 12 * s), 15f * s);
            float tw = UiUtil.textWidth(tabText, 15f * s);
            dl.addText(font, 15f * s, tabX + Math.max(6 * s, (tabW - tw) / 2f), tabRowY + 8 * s,
                    selected ? ImGui.getColorU32(0.1f, 0.1f, 0.1f, 1f) : Theme.COLOR_TEXT_DIM, tabText);

            ImGui.setCursorScreenPos(tabX, tabRowY);
            if (ImGui.invisibleButton("##tab_" + i, tabW, tabH)) settingsTab = i;
        }

        float dividerY = tabY + tabBlockH + 10 * s;
        dl.addLine(x, dividerY, x + width, dividerY, ImGui.getColorU32(0.3f, 0.3f, 0.3f, 0.5f), 1f);

        float contentY = dividerY + 20 * s;
        float contentH = height - (contentY - y);
        dl.pushClipRect(x, contentY, x + width, contentY + contentH, true);

        ImGui.setCursorScreenPos(x, contentY);
        ImGui.beginChild("##settings_content", width, contentH, false, ImGuiWindowFlags.NoScrollbar);

        float mouseY = ImGui.getMousePosY();
        if (!ImGui.isMouseDown(0)) {
            touchScrolling = false;
        } else if (ImGui.isMouseClicked(0) && !ImGui.isAnyItemHovered()
                && ImGui.isMouseHoveringRect(x, contentY, x + width, contentY + contentH)) {
            touchScrolling = true;
            touchStartY = mouseY;
            touchStartScroll = ImGui.getScrollY();
        }
        if (touchScrolling) {
            ImGui.setScrollY(touchStartScroll - (mouseY - touchStartY));
        }

        float pX = x + 10 * s;
        float pY = contentY + 10 * s;
        float pW = width - 20 * s;
        float sy = ImGui.getScrollY();

        switch (settingsTab) {
            case 0 -> renderGeneral(dl, font, pX, pY, pW, sy, s, customTextColor);
            case 1 -> renderAppearance(dl, font, pX, pY, pW, sy, s, customTextColor);
            case 2 -> renderModules(dl, font, pX, pY, pW, sy, s);
            case 3 -> renderKeybinds(dl, font, pX, pY, pW, sy, s);
            case 4 -> renderAbout(dl, font, pX, pY, pW, sy, s, customTextColor);
            default -> { }
        }

        float maxScroll = ImGui.getScrollMaxY();
        if (maxScroll > 0f) {
            float trackX = x + width - 6f * s;
            float viewRatio = contentH / (contentH + maxScroll);
            float thumbH = Math.max(24f * s, contentH * viewRatio);
            float thumbY = contentY + (ImGui.getScrollY() / maxScroll) * (contentH - thumbH);
            dl.addRectFilled(trackX, contentY + 2f * s, trackX + 3f * s, contentY + contentH - 2f * s,
                    Theme.withAlpha(Theme.COLOR_TEXT_DIM, 0.20f), 2f * s);
            dl.addRectFilled(trackX, thumbY, trackX + 3f * s, thumbY + thumbH,
                    Theme.withAlpha(Theme.COLOR_ACCENT, 0.85f), 2f * s);
        }

        ImGui.endChild();
        dl.popClipRect();
    }

    // ===== 常规 =====
    private static void renderGeneral(ImDrawList dl, ImFont font, float pX, float pY, float pW, float sy, float s, float[] customTextColor) {
        float y = pY + 4f * s;

        groupTitle(dl, font, "cmeclient.gui.group.interface", pX, y - sy, s);
        y += 26f * s;
        textSlider(dl, font, pX, y - sy, pW, s, "cmeclient.gui.scale", ClickGUI.uiScale, 0.7f, 1.5f, v -> {
            ClickGUI.uiScale = v;
            ConfigManager.markDirty();
        });
        y += 50f * s;

        boolean closeOutside = checkboxAt("close_outside",
                Lang.get("cmeclient.gui.close_outside"), pX, y - sy, pW, ClickGUI.closeOnOutsideClick);
        if (closeOutside != ClickGUI.closeOnOutsideClick) {
            ClickGUI.closeOnOutsideClick = closeOutside;
            ConfigManager.markDirty();
        }
        y += 34f * s;

        groupTitle(dl, font, "cmeclient.gui.group.language", pX, y - sy, s);
        y += 26f * s;
        float langGap = 8f * s;
        float langW = (pW - langGap) / 2f;
        float langH = 38f * s;
        String[] langLabels = {Lang.get("cmeclient.gui.lang.zh"), Lang.get("cmeclient.gui.lang.en")};
        boolean[] langSelected = {Lang.isChinese, !Lang.isChinese};
        for (int i = 0; i < 2; i++) {
            float lx = pX + i * (langW + langGap);
            float ly = y - sy;
            boolean langHovered = ImGui.isMouseHoveringRect(lx, ly, lx + langW, ly + langH);
            if (langSelected[i]) {
                dl.addRectFilled(lx, ly, lx + langW, ly + langH, Theme.COLOR_ACCENT, 6f * s);
            } else if (langHovered) {
                dl.addRectFilled(lx, ly, lx + langW, ly + langH,
                        ImGui.getColorU32(0.3f, 0.3f, 0.3f, 0.3f), 6f * s);
            }
            String langLabel = UiUtil.fit(langLabels[i], Math.max(10f * s, langW - 12f * s), 15f * s);
            float langTextW = UiUtil.textWidth(langLabel, 15f * s);
            dl.addText(font, 15f * s, lx + Math.max(6f * s, (langW - langTextW) / 2f),
                    ly + (langH - 15f * s) / 2f,
                    langSelected[i] ? ImGui.getColorU32(0.1f, 0.1f, 0.1f, 1f) : Theme.COLOR_TEXT_DIM,
                    langLabel);
            ImGui.setCursorScreenPos(lx, ly);
            if (ImGui.invisibleButton("##lang_" + (i == 0 ? "zh" : "en"), langW, langH)) {
                boolean chinese = i == 0;
                if (Lang.isChinese != chinese) {
                    Lang.isChinese = chinese;
                    Lang.load();
                    ConfigManager.markDirty();
                }
            }
        }
        y += 54f * s;

        groupTitle(dl, font, "cmeclient.gui.group.hud", pX, y - sy, s);
        y += 28f * s;

        boolean twoCol = pW >= 230f * s;
        float colW = twoCol ? pW / 2f : pW;
        float col2X = twoCol ? pX + colW : pX;
        float rowGap = 28f * s;

        boolean hudOn = checkboxAt("hud_on", Lang.get("cmeclient.gui.hud.enabled"), pX, y - sy, colW, HUD.hudEnabled);
        boolean hudWm = checkboxAt("hud_wm", Lang.get("cmeclient.gui.hud.watermark"), col2X, (twoCol ? y : y + rowGap) - sy, colW, HUD.showWatermark);
        if (hudOn != HUD.hudEnabled) { HUD.hudEnabled = hudOn; ConfigManager.markDirty(); }
        if (hudWm != HUD.showWatermark) { HUD.showWatermark = hudWm; ConfigManager.markDirty(); }
        y += twoCol ? 32f * s : rowGap * 2f;

        boolean hudAl = checkboxAt("hud_al", Lang.get("cmeclient.gui.hud.arraylist"), pX, y - sy, colW, HUD.showArrayList);
        boolean hudInfo = checkboxAt("hud_info", Lang.get("cmeclient.gui.hud.info"), col2X, (twoCol ? y : y + rowGap) - sy, colW, HUD.showInfo);
        if (hudAl != HUD.showArrayList) { HUD.showArrayList = hudAl; ConfigManager.markDirty(); }
        if (hudInfo != HUD.showInfo) { HUD.showInfo = hudInfo; ConfigManager.markDirty(); }
        y += twoCol ? 32f * s : rowGap * 2f;

        boolean showFps = checkboxAt("hud_fps", Lang.get("cmeclient.hud.fps"), pX, y - sy, colW, HUD.showFps);
        boolean showPing = checkboxAt("hud_ping", Lang.get("cmeclient.hud.ping"), col2X, (twoCol ? y : y + rowGap) - sy, colW, HUD.showPing);
        if (showFps != HUD.showFps) { HUD.showFps = showFps; ConfigManager.markDirty(); }
        if (showPing != HUD.showPing) { HUD.showPing = showPing; ConfigManager.markDirty(); }
        y += twoCol ? 32f * s : rowGap * 2f;

        boolean showCoords = checkboxAt("hud_coords", Lang.get("cmeclient.hud.coords"), pX, y - sy, colW, HUD.showCoords);
        boolean showEffects = checkboxAt("hud_effects", Lang.get("cmeclient.hud.effects"), col2X, (twoCol ? y : y + rowGap) - sy, colW, HUD.showEffects);
        if (showCoords != HUD.showCoords) { HUD.showCoords = showCoords; ConfigManager.markDirty(); }
        if (showEffects != HUD.showEffects) { HUD.showEffects = showEffects; ConfigManager.markDirty(); }
        y += twoCol ? 32f * s : rowGap * 2f;

        boolean showDurability = checkboxAt("hud_durability", Lang.get("cmeclient.hud.durability"), pX, y - sy, colW, HUD.showDurability);
        boolean hudBg = checkboxAt("hud_bg", Lang.get("cmeclient.hud.background"), col2X, (twoCol ? y : y + rowGap) - sy, colW, HUD.hudBackground);
        if (showDurability != HUD.showDurability) { HUD.showDurability = showDurability; ConfigManager.markDirty(); }
        if (hudBg != HUD.hudBackground) { HUD.hudBackground = hudBg; ConfigManager.markDirty(); }
        y += twoCol ? 32f * s : rowGap * 2f;

        textSlider(dl, font, pX, y - sy, pW, s, "cmeclient.hud.text_scale", HUD.hudTextScale, 0.8f, 1.3f, v -> {
            HUD.hudTextScale = v;
            ConfigManager.markDirty();
        });
        y += 50f * s;

        ImGui.setCursorScreenPos(pX, y - sy);
        String layoutLabel = UiUtil.fit(Lang.get("cmeclient.gui.hud.layout"), pW - 16f * s, 15f * s);
        if (ImGui.button(layoutLabel + "##hud_layout", pW, 40f * s)) {
            HUD.layoutMode = true;
        }
    }

    // ===== 外观 =====
    private static void renderAppearance(ImDrawList dl, ImFont font, float pX, float pY, float pW, float sy, float s, float[] customTextColor) {
        float y = pY + 4f * s;
        dl.addText(font, 16f * s, pX, y - sy, Theme.COLOR_TEXT, Lang.get("cmeclient.gui.theme"));
        y += 28f * s;

        float gap = 8 * s;
        int perRow = 3;
        float swatchW = (pW - gap * (perRow - 1)) / perRow;
        float swatchH = 42 * s;
        float labelSize = 12 * s;
        float rowH = swatchH + 34 * s;
        for (int i = 0; i < ClickGUI.THEME_KEYS.length; i++) {
            int col = i % perRow;
            int row = i / perRow;
            float sx = pX + col * (swatchW + gap);
            float swatchY = y + row * rowH - sy;
            boolean selected = ClickGUI.themeIndex == i;
            dl.addRectFilled(sx, swatchY, sx + swatchW, swatchY + swatchH, Theme.previewAccent(i), 8 * s);
            if (ImGui.isMouseHoveringRect(sx, swatchY, sx + swatchW, swatchY + swatchH)) {
                dl.addRectFilled(sx, swatchY, sx + swatchW, swatchY + swatchH,
                        ImGui.getColorU32(1f, 1f, 1f, 0.12f), 8 * s);
            }
            if (selected) {
                dl.addRect(sx, swatchY, sx + swatchW, swatchY + swatchH, Theme.COLOR_ACCENT, 8 * s, 0, 2 * s);
            }
            String themeName = UiUtil.fit(Lang.get(ClickGUI.THEME_KEYS[i]), swatchW - 4 * s, labelSize);
            float tw = UiUtil.textWidth(themeName, labelSize);
            dl.addText(font, labelSize, sx + (swatchW - tw) / 2f, swatchY + swatchH + 8 * s,
                    selected ? Theme.COLOR_ACCENT : Theme.COLOR_TEXT_DIM, themeName);

            ImGui.setCursorScreenPos(sx, swatchY);
            if (ImGui.invisibleButton("##theme_" + i, swatchW, swatchH + 26 * s)) {
                ClickGUI.themeIndex = i;
                ConfigManager.markDirty();
            }
        }
        int rows = (ClickGUI.THEME_KEYS.length + perRow - 1) / perRow;
        y += 30f * s + rows * rowH + 6f * s;

        groupTitle(dl, font, "cmeclient.gui.group.style", pX, y - sy, s);
        y += 26f * s;

        textSlider(dl, font, pX, y - sy, pW, s, "cmeclient.gui.rounding", Theme.roundingScale, 0.3f, 2f, v -> {
            Theme.roundingScale = v;
            ConfigManager.markDirty();
        });
        y += 50f * s;

        textSlider(dl, font, pX, y - sy, pW, s, "cmeclient.gui.opacity", Theme.panelOpacity, 0.5f, 1f, v -> {
            Theme.panelOpacity = v;
            Theme.apply(ClickGUI.themeIndex, customTextColor);
            ConfigManager.markDirty();
        });
        y += 50f * s;

        boolean accentOn = checkboxAt("accent_custom", Lang.get("cmeclient.gui.accent"), pX, y - sy, pW, Theme.customAccentEnabled);
        if (accentOn != Theme.customAccentEnabled) {
            Theme.customAccentEnabled = accentOn;
            Theme.apply(ClickGUI.themeIndex, customTextColor);
            ConfigManager.markDirty();
        }
        y += 30f * s;
        ImGui.setCursorScreenPos(pX, y - sy);
        ImGui.setNextItemWidth(pW);
        if (ImGui.colorEdit3("##accentColor", Theme.customAccent)) {
            Theme.apply(ClickGUI.themeIndex, customTextColor);
            ConfigManager.markDirty();
        }
        y += 44f * s;

        dl.addText(font, 16f * s, pX, y - sy, Theme.COLOR_TEXT, Lang.get("cmeclient.gui.text_color"));
        y += 28f * s;
        ImGui.setCursorScreenPos(pX, y - sy);
        ImGui.setNextItemWidth(pW);
        if (ImGui.colorEdit3("##textColor", customTextColor)) ConfigManager.markDirty();
    }

    // ===== 模块设置（每个模块的 Setting 都在这里统一展示）=====
    private static void renderModules(ImDrawList dl, ImFont font, float pX, float pY, float pW, float sy, float s) {
        float y = pY + 4f * s;
        dl.addText(font, 13f * s, pX, y - sy, Theme.COLOR_TEXT_DIM,
                UiUtil.fit(Lang.get("cmeclient.gui.tab.modules_hint"), pW, 13f * s));
        y += 24f * s;

        for (Module m : ModuleManager.INSTANCE.getModules()) {
            boolean expanded = ModuleCard.isExpanded(m.getId());
            float headerH = 34f * s;
            boolean hovered = ImGui.isMouseHoveringRect(pX, y - sy, pX + pW, y - sy + headerH);
            if (hovered) {
                dl.addRectFilled(pX, y - sy, pX + pW, y - sy + headerH,
                        ImGui.getColorU32(0.25f, 0.25f, 0.30f, 0.35f), 6f * s);
            }

            int accent = Theme.categoryColor(m.getCategory());
            dl.addCircleFilled(pX + 8f * s, y - sy + headerH / 2f, 3f * s, accent);
            dl.addText(font, 15f * s, pX + 18f * s, y - sy + 8f * s, Theme.COLOR_TEXT, m.getName());
            dl.addText(font, 11f * s, pX + pW - 22f * s, y - sy + 10f * s,
                    m.isEnabled() ? Theme.COLOR_ACCENT : Theme.COLOR_TEXT_DIM,
                    m.isEnabled() ? "ON" : "OFF");

            ImGui.setCursorScreenPos(pX, y - sy);
            if (ImGui.invisibleButton("##mod_h_" + m.getId(), pW - 10f * s, headerH)) {
                ModuleCard.toggleExpanded(m.getId());
            }
            y += headerH + 2f * s;

            if (expanded) {
                float innerX = pX + 18f * s;
                float innerW = pW - 18f * s;
                float rowH = 32f * s;
                int count = m.getSettings().size();
                if (count == 0) {
                    dl.addText(font, 12f * s, innerX, y - sy + 6f * s, Theme.COLOR_TEXT_DIM,
                            UiUtil.fit(Lang.get("cmeclient.gui.no_settings"), innerW, 12f * s));
                    y += 30f * s;
                } else {
                    for (Setting<?> setting : m.getSettings()) {
                        drawModuleSetting(dl, font, setting, innerX, y - sy, innerW, s, accent);
                        y += rowH;
                    }
                }
                y += 6f * s;
            }
        }
    }

    private static void drawModuleSetting(ImDrawList dl, ImFont font, Setting<?> setting,
                                          float x, float rowY, float width, float s, int accent) {
        String label = UiUtil.fit(Lang.get(setting.getNameKey()), width * 0.45f, 13f * s);
        dl.addText(font, 13f * s, x, rowY + 9f * s, Theme.COLOR_TEXT_DIM, label);

        float right = x + width;
        float ctrlW = Math.min(width * 0.5f, 180f * s);
        float ctrlX = right - ctrlW;
        float cy = rowY + 16f * s;

        if (setting instanceof BoolSetting bs) {
            float w = 36f * s, h = 20f * s;
            float tx = ctrlX + ctrlW - w;
            float ty = cy - h / 2f;
            boolean hovered = ImGui.isMouseHoveringRect(ctrlX, cy - 14f * s, right, cy + 14f * s);
            dl.addRectFilled(tx, ty, tx + w, ty + h,
                    bs.get() ? accent : ImGui.getColorU32(1f, 1f, 1f, 0.16f), h / 2f);
            float knobX = bs.get() ? tx + w - h / 2f : tx + h / 2f;
            dl.addCircleFilled(knobX, cy, h / 2f - 2f * s,
                    bs.get() ? ImGui.getColorU32(0.1f, 0.1f, 0.1f, 1f)
                             : ImGui.getColorU32(0.85f, 0.85f, 0.9f, 1f));
            if (hovered && ImGui.isMouseClicked(0)) {
                bs.toggle();
                ConfigManager.markDirty();
            }
        } else if (setting instanceof FloatSetting fs) {
            float valueW = 38f * s;
            float trackX = ctrlX;
            float trackW = ctrlW - valueW - 6f * s;
            float min = fs.getMin(), max = fs.getMax();
            float ratio = max > min ? (fs.get() - min) / (max - min) : 0f;
            ratio = Math.max(0f, Math.min(1f, ratio));
            float trackY = cy - 3f * s;
            dl.addRectFilled(trackX, trackY, trackX + trackW, trackY + 6f * s,
                    ImGui.getColorU32(1f, 1f, 1f, 0.14f), 3f * s);
            dl.addRectFilled(trackX, trackY, trackX + trackW * ratio, trackY + 6f * s, accent, 3f * s);
            dl.addCircleFilled(trackX + trackW * ratio, cy, 6f * s, accent);
            String v = String.format("%.1f", fs.get());
            float vw = UiUtil.textWidth(v, 12f * s);
            dl.addText(font, 12f * s, right - vw, cy - 7f * s, Theme.COLOR_TEXT, v);

            boolean hovered = ImGui.isMouseHoveringRect(trackX - 6f * s, cy - 14f * s,
                    trackX + trackW + 6f * s, cy + 14f * s);
            String sliderId = "settings_" + fs.getNameKey();
            if (hovered && ImGui.isMouseClicked(0)) {
                ModuleCard.mouseOverControl = true;
                ClickGUI.listeningModule = null;
                fs.setClamped(min + (max - min) * Math.max(0f,
                        Math.min(1f, (ImGui.getMousePosX() - trackX) / trackW)));
                ConfigManager.markDirty();
            }
        } else if (setting instanceof EnumSetting es) {
            float w = ctrlW, h = 24f * s;
            float px = ctrlX, py = cy - h / 2f;
            boolean hovered = ImGui.isMouseHoveringRect(ctrlX, cy - 14f * s, right, cy + 14f * s);
            dl.addRectFilled(px, py, px + w, py + h,
                    ImGui.getColorU32(1f, 1f, 1f, hovered ? 0.16f : 0.09f), 6f * s);
            dl.addRect(px, py, px + w, py + h, Theme.withAlpha(accent, 0.65f), 6f * s, 0, 1f * s);
            String cur = UiUtil.fit(Lang.option(es.getNameKey(), es.get(), es.getCurrent()),
                    w - 16f * s, 12f * s);
            float tw = UiUtil.textWidth(cur, 12f * s);
            dl.addText(font, 12f * s, px + (w - tw) / 2f, cy - 7f * s, Theme.COLOR_TEXT, cur);
            if (hovered && ImGui.isMouseClicked(0)) {
                if (ImGui.getMousePosX() < px + w * 0.32f) es.prev();
                else es.next();
                ConfigManager.markDirty();
            }
        }
    }

    // ===== 快捷键 =====
    private static void renderKeybinds(ImDrawList dl, ImFont font, float pX, float pY, float pW, float sy, float s) {
        float y = pY + 4f * s;
        dl.addText(font, 13f * s, pX, y - sy, Theme.COLOR_TEXT_DIM,
                UiUtil.fit(Lang.get("cmeclient.gui.keybind_hint"), pW, 13f * s));
        y += 32f * s;

        for (Module m : ModuleManager.INSTANCE.getModules()) {
            ImGui.setCursorScreenPos(pX, y - sy);
            String keyName = m.keybind == -1
                    ? Lang.get("cmeclient.gui.unbound")
                    : InputUtil.fromKeyCode(m.keybind, 0).getLocalizedText().getString();
            if (ClickGUI.listeningModule == m) keyName = Lang.get("cmeclient.gui.press_key");

            String keyLabel = UiUtil.fit(m.getName() + "  [" + keyName + "]", pW - 16 * s, 15f * s);
            if (ImGui.button(keyLabel + "##kb_" + m.getId(), pW, 35 * s)) {
                ClickGUI.listeningModule = m;
            }
            y += 42f * s;
        }
    }

    // ===== 关于 =====
    private static void renderAbout(ImDrawList dl, ImFont font, float pX, float pY, float pW, float sy, float s, float[] customTextColor) {
        int color = ImGui.getColorU32(customTextColor[0], customTextColor[1], customTextColor[2], 1f);
        String[] keys = {
                "cmeclient.gui.author",
                "cmeclient.gui.github",
                "cmeclient.gui.bilibili"
        };
        float y = pY + 4f * s;
        for (String k : keys) {
            dl.addText(font, 15f * s, pX, y - sy, color,
                    UiUtil.fit(Lang.get(k), Math.max(10f * s, pW - 8f * s), 15f * s));
            y += 26f * s;
        }
        dl.addText(font, 12f * s, pX, y + 20f * s - sy, Theme.COLOR_TEXT_DIM,
                UiUtil.fit(Lang.get("cmeclient.gui.version"), Math.max(10f * s, pW - 8f * s), 12f * s));
    }

    // ===== 小工具 =====
    private static void groupTitle(ImDrawList dl, ImFont font, String key, float x, float y, float s) {
        String title = Lang.get(key);
        dl.addCircleFilled(x + 3f * s, y + 8f * s, 3f * s, Theme.COLOR_ACCENT);
        dl.addText(font, 15f * s, x + 12f * s, y, Theme.COLOR_ACCENT, title);
        float tw = UiUtil.textWidth(title, 15f * s);
        dl.addLine(x + 18f * s + tw, y + 9f * s, x + 18f * s + tw + 40f * s, y + 9f * s,
                Theme.withAlpha(Theme.COLOR_ACCENT, 0.30f), 1f * s);
    }

    private static void textSlider(ImDrawList dl, ImFont font, float x, float y, float w, float s,
                                   String labelKey, float value, float min, float max,
                                   java.util.function.Consumer<Float> setter) {
        dl.addText(font, 13f * s, x, y, Theme.COLOR_TEXT_DIM,
                UiUtil.fit(Lang.get(labelKey) + String.format(": %.2f", value), w, 13f * s));
        ImGui.setCursorScreenPos(x, y + 20f * s);
        ImGui.setNextItemWidth(w);
        float[] arr = {value};
        if (ImGui.sliderFloat("##" + labelKey, arr, min, max, "")) {
            setter.accept(arr[0]);
        }
    }

    private static boolean checkboxAt(String id, String label, float x, float y, float w, boolean value) {
        float s = curS;
        float rowH = 22f * s;
        float box = 16f * s;
        ImGui.setCursorScreenPos(x, y);
        boolean clicked = ImGui.invisibleButton("##cb_" + id, Math.max(1f, w), rowH);
        boolean hovered = ImGui.isMouseHoveringRect(x, y, x + w, y + rowH);
        if (hovered) {
            curDl.addRectFilled(x, y, x + w, y + rowH, ImGui.getColorU32(0.3f, 0.3f, 0.3f, 0.22f), 5f * s);
        }
        float boxY = y + (rowH - box) / 2f;
        if (value) {
            curDl.addRectFilled(x, boxY, x + box, boxY + box, Theme.COLOR_ACCENT, 4f * s);
            int ink = ImGui.getColorU32(0.1f, 0.1f, 0.1f, 1f);
            curDl.addLine(x + box * 0.24f, boxY + box * 0.52f,
                    x + box * 0.43f, boxY + box * 0.72f, ink, 2f * s);
            curDl.addLine(x + box * 0.43f, boxY + box * 0.72f,
                    x + box * 0.78f, boxY + box * 0.28f, ink, 2f * s);
        } else {
            curDl.addRectFilled(x, boxY, x + box, boxY + box,
                    ImGui.getColorU32(0.18f, 0.18f, 0.20f, 0.9f), 4f * s);
            curDl.addRect(x, boxY, x + box, boxY + box,
                    ImGui.getColorU32(0.45f, 0.45f, 0.50f, 1f), 4f * s, 0, 1f * s);
        }
        String text = UiUtil.fit(label, Math.max(8f * s, w - box - 10f * s), 14f * s);
        curDl.addText(curFont, 14f * s, x + box + 8f * s,
                y + (rowH - 14f * s) / 2f, Theme.COLOR_TEXT, text);
        return clicked ? !value : value;
    }
}
