package com.chengmo.cmeclient.gui.component;

import com.chengmo.cmeclient.gui.Notification;
import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.impl.BlockESP;
import com.chengmo.cmeclient.module.impl.ESP;
import com.chengmo.cmeclient.module.setting.BoolSetting;
import com.chengmo.cmeclient.module.setting.EnumSetting;
import com.chengmo.cmeclient.module.setting.FloatSetting;
import com.chengmo.cmeclient.module.setting.Setting;
import com.chengmo.cmeclient.util.BlockIds;
import com.chengmo.cmeclient.util.EntityIds;
import com.chengmo.cmeclient.util.ConfigManager;
import com.chengmo.cmeclient.util.Lang;
import com.chengmo.cmeclient.util.UiUtil;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModuleCard {
    private static final Map<String, Float> toggleAnims = new HashMap<>();
    private static final Map<String, Float> hoverAnims = new HashMap<>();
    private static final Map<String, Float> starAnims = new HashMap<>();
    private static final Set<String> expandedCards = new HashSet<>();
    private static final Map<String, Long> clickTimes = new HashMap<>();
    private static final Map<String, Long> tooltipStarts = new HashMap<>();

    public static final Set<String> favorites = new LinkedHashSet<>();
    public static final List<String> recents = new ArrayList<>();
    private static final int MAX_RECENTS = 8;

    private static String activeSlider = null;

    public static Module hoveredModule = null;
    public static boolean mouseOverControl = false;

    public static float clipMinX = -Float.MAX_VALUE, clipMinY = -Float.MAX_VALUE;
    public static float clipMaxX = Float.MAX_VALUE, clipMaxY = Float.MAX_VALUE;

    private static final float CARD_H = 70f;
    private static final float ROW_H = 34f;
    private static final float BLOCK_ROW_H = 32f;

    // ===== 方块 / 实体添加弹窗状态 =====
    private static boolean addPopupRequested = false;
    private static BlockESP addPopupTarget = null;
    private static ESP addEntityPopupTarget = null;
    private static final ImString addPopupSearch = new ImString();
    private static String addPopupCachedQuery = "\0";
    private static final List<String> addPopupResults = new ArrayList<>();

    private static boolean hovering(float x1, float y1, float x2, float y2) {
        if (x2 <= clipMinX || x1 >= clipMaxX || y2 <= clipMinY || y1 >= clipMaxY) return false;
        return ImGui.isMouseHoveringRect(x1, y1, x2, y2);
    }

    public static float lerp(float current, float target, float speed) {
        float dt = ImGui.getIO().getDeltaTime();
        if (dt > 0.1f) dt = 0.1f;
        return current + (target - current) * (1f - (float)Math.exp(-speed * dt));
    }

    public static boolean isExpanded(String id) { return expandedCards.contains(id); }

    /** 当前展开的卡片 ID 快照（只读），供 ConfigManager 序列化。 */
    public static java.util.Set<String> expandedSnapshot() {
        return new java.util.HashSet<>(expandedCards);
    }

    /** 从配置恢复展开的卡片集合。 */
    public static void restoreExpanded(java.util.Collection<String> ids) {
        expandedCards.clear();
        if (ids != null) expandedCards.addAll(ids);
    }

    public static void toggleExpanded(String id) {
        if (!expandedCards.remove(id)) expandedCards.add(id);
    }

    private static void touchRecent(String id) {
        recents.remove(id);
        recents.add(0, id);
        while (recents.size() > MAX_RECENTS) recents.remove(recents.size() - 1);
    }

    public static void moveFavorite(String id, int targetIndex) {
        List<String> order = new ArrayList<>(favorites);
        int from = order.indexOf(id);
        if (from < 0) return;
        order.remove(from);
        int to = Math.max(0, Math.min(order.size(), targetIndex));
        order.add(to, id);
        favorites.clear();
        favorites.addAll(order);
        ConfigManager.markDirty();
    }

    private static boolean isDoubleClick(String key) {
        long now = System.currentTimeMillis();
        Long last = clickTimes.put(key, now);
        return last != null && now - last <= 320L;
    }

    private static boolean tooltipVisible(String key, boolean hovered) {
        long now = System.currentTimeMillis();
        if (!hovered) {
            // 800ms 宽限期：鼠标在边缘抖动时不重置计时，避免 tooltip 闪烁
            Long start = tooltipStarts.get(key);
            if (start != null && now - start > 800L) {
                tooltipStarts.remove(key);
            }
            return false;
        }
        Long start = tooltipStarts.get(key);
        if (start == null) {
            tooltipStarts.put(key, now);
            return false;
        }
        return now - start >= 300L;
    }

    public static float draw(ImDrawList dl, ImFont font, Module module, float x, float y, float width, float s, int themeIndex) {
        String id = module.getId();
        float cardH = CARD_H * s;

        int settingCount = module.getSettings().size();
        boolean expanded = settingCount > 0 && expandedCards.contains(id);

        BlockESP beModule = module instanceof BlockESP ? (BlockESP) module : null;
        ESP espModule = module instanceof ESP ? (ESP) module : null;
        float extraBlockH = 0f;
        if (beModule != null) {
            extraBlockH = 6f * s + beModule.blocks.size() * BLOCK_ROW_H * s + 36f * s;
        }
        float extraEntityH = 0f;
        if (espModule != null) {
            extraEntityH = 6f * s + espModule.customEntities.size() * BLOCK_ROW_H * s + 36f * s;
        }

        float settingsH = settingCount == 0 ? 0f
                : (10f + settingCount * ROW_H + 8f) * s + (expanded ? extraBlockH + extraEntityH : 0f);
        float totalH = cardH + (expanded ? settingsH : 0f);

        boolean isHovered = hovering(x, y, x + width, y + cardH);
        if (isHovered) hoveredModule = module;

        float cur = hoverAnims.getOrDefault(id + "_card", 0f);
        cur = lerp(cur, isHovered ? 1f : 0f, 20f);
        hoverAnims.put(id + "_card", cur);

        // 卡片不随 hover 位移，反馈用阴影 + 发光代替
        float floatY = 0f;

        int cardBg = Theme.COLOR_CARD_BG;
        int borderColor = isHovered || expanded ? Theme.COLOR_ACCENT : Theme.COLOR_CARD_BORDER;
        int accent = Theme.categoryColor(module.getCategory());

        float cardRadius = Theme.r(10f, s);
        if (cur > 0.01f && !expanded) {
            dl.addRectFilled(x + 4*s, y + 8*s + floatY, x + width - 4*s, y + cardH + 8*s + floatY,
                    ImGui.getColorU32(0, 0, 0, 0.3f * cur), cardRadius);
            float glow = 2f * s * cur;
            dl.addRect(x - glow, y - glow, x + width + glow, y + cardH + glow,
                    Theme.withAlpha(Theme.COLOR_ACCENT, 0.22f * cur),
                    cardRadius + glow, 0, 1.2f * s * cur);
        }

        dl.addRectFilled(x, y + floatY, x + width, y + cardH + floatY, cardBg, cardRadius);
        dl.addRect(x, y + floatY, x + width, y + cardH + floatY, borderColor, cardRadius, 0, isHovered || expanded ? 1.5f*s : 1f*s);
        dl.addRectFilled(x + 10*s, y + 14*s + floatY, x + 13*s, y + cardH - 14*s + floatY, accent, 1.5f*s);

        float swW = 50*s, swH = 28*s;
        float swX = x + width - 20*s - swW;

        boolean favorite = favorites.contains(id);
        float starHitR = Math.min(x + 40*s, swX - 46*s);
        boolean starHovered = hovering(x + 6*s, y + floatY, Math.max(x + 22*s, starHitR), y + 44*s + floatY);
        if (starHovered) mouseOverControl = true;
        float starAnim = starAnims.getOrDefault(id, 0f);
        starAnim = lerp(starAnim, favorite ? 1f : 0f, 18f);
        starAnims.put(id, starAnim);

        float starCX = x + 29*s;
        float starCY = y + 22*s + floatY;
        float starR = 8f * s;
        ImVec2[] star = starPoints(starCX, starCY, starR, starR * 0.45f);
        int starColor = favorite
                ? ImGui.getColorU32(1f, 0.78f, 0.26f, 1f)
                : (starHovered ? Theme.COLOR_TEXT : Theme.COLOR_TEXT_DIM);
        if (starAnim > 0.05f) {
            dl.addPolyline(star, star.length, Theme.withAlpha(ImGui.getColorU32(1f, 0.78f, 0.26f, 1f), 0.25f * starAnim), 0, 4f * s);
        }
        if (favorite) {
            dl.addConvexPolyFilled(star, star.length, starColor);
        } else {
            dl.addPolyline(star, star.length, starColor, 0, 1.2f * s);
        }
        if (starHovered && ImGui.isMouseClicked(0)) {
            if (!favorites.remove(id)) favorites.add(id);
            ConfigManager.markDirty();
            Notification.show(Lang.get(favorites.contains(id)
                    ? "cmeclient.gui.favorite_added" : "cmeclient.gui.favorite_removed", module.getName()));
        }

        float textX = x + 46*s;
        float nameRight = settingCount > 0 ? swX - 48f * s : swX - 8f * s;
        float textMaxW = Math.max(1f, nameRight - textX);
        dl.addText(font, 18f*s, textX, y + 12*s + floatY, Theme.COLOR_TEXT,
                UiUtil.fit(module.getName(), textMaxW, 18f*s));

        float descMaxW = textMaxW;
        if (module.keybind >= 0) {
            String keyName = InputUtil.fromKeyCode(module.keybind, 0).getLocalizedText().getString();
            float chipSize = 12f * s;
            float chipW = Math.min(textMaxW * 0.45f, UiUtil.textWidth(keyName, chipSize) + 12f * s);
            float chipH = 17f * s;
            float chipX = swX - 42f * s - chipW;
            float chipY = y + 35f * s + floatY;
            dl.addRectFilled(chipX, chipY, chipX + chipW, chipY + chipH,
                    Theme.withAlpha(accent, 0.18f), Theme.r(5f, s));
            dl.addText(font, chipSize, chipX + 6f * s, chipY + 2f * s, accent,
                    UiUtil.fit(keyName, chipW - 12f * s, chipSize));
            descMaxW = Math.max(1f, chipX - textX - 6f * s);
        }

        String desc = module.getDescription();
        boolean descClipped = UiUtil.textWidth(desc, 14f * s) > descMaxW;
        dl.addText(font, 14f*s, textX, y + 38*s + floatY, Theme.COLOR_TEXT_DIM,
                UiUtil.fit(desc, descMaxW, 14f*s));

        boolean descHovered = descClipped && hovering(textX, y + 34*s + floatY, textX + descMaxW, y + 56*s + floatY);
        if (tooltipVisible(id + "_tip", descHovered) && hovering(textX, y + 34*s + floatY, textX + descMaxW, y + 56*s + floatY)) {
            ImDrawList fg = ImGui.getForegroundDrawList();
            float tipSize = 13f * s;
            float tipW = UiUtil.textWidth(desc, tipSize) + 16f * s;
            float tipH = 24f * s;
            float tipX = Math.min(ImGui.getIO().getMousePosX() + 14f * s,
                    ImGui.getIO().getDisplaySize().x - tipW - 8f * s);
            float tipY = ImGui.getIO().getMousePosY() + 18f * s;
            float tipRadius = Theme.r(6f, s);
            fg.addRectFilled(tipX, tipY, tipX + tipW, tipY + tipH, Theme.COLOR_PANEL_BG, tipRadius);
            fg.addRect(tipX, tipY, tipX + tipW, tipY + tipH, Theme.COLOR_CARD_BORDER, tipRadius, 0, 1f * s);
            fg.addText(font, tipSize, tipX + 8f * s, tipY + 5f * s, Theme.COLOR_TEXT, desc);
        }

        float swY = y + (cardH - swH) / 2f + floatY;
        float tgt = module.isEnabled() ? 1f : 0f;
        float tog = toggleAnims.getOrDefault(id, 0f);
        tog = lerp(tog, tgt, 20f);
        toggleAnims.put(id, tog);

        int swBg = module.isEnabled() ? Theme.COLOR_ACCENT : (themeIndex == 1 ? ImGui.getColorU32(1, 1, 1, 0.2f) : ImGui.getColorU32(0.2f, 0.2f, 0.25f, 1f));
        dl.addRectFilled(swX, swY, swX + swW, swY + swH, swBg, 14f*s);
        float knob = swX + 3*s + 11*s + (swW - swH) * tog;
        dl.addCircleFilled(knob, swY + swH / 2f, 11*s, module.isEnabled() ? ImGui.getColorU32(0.1f, 0.1f, 0.1f, 1f) : ImGui.getColorU32(0.8f, 0.8f, 0.8f, 1f));

        float gs = 30*s;
        float gx = swX - 12*s - gs;
        float gy = y + (cardH - gs) / 2f + floatY;
        float gearHit = Math.max(gs, 36f * s);
        float ghx = gx + gs / 2f - gearHit / 2f;
        float ghy = gy + gs / 2f - gearHit / 2f;
        boolean gearHovered = settingCount > 0 && hovering(ghx, ghy, ghx + gearHit, ghy + gearHit);
        if (gearHovered) mouseOverControl = true;

        if (settingCount > 0) {
            float gearRadius = Theme.r(7f, s);
            if (gearHovered || expanded) {
                dl.addRectFilled(gx, gy, gx + gs, gy + gs, Theme.withAlpha(accent, expanded ? 0.30f : 0.16f), gearRadius);
            }
            dl.addRect(gx, gy, gx + gs, gy + gs, expanded ? accent : Theme.COLOR_CARD_BORDER, gearRadius, 0, 1f*s);
            int iconColor = expanded ? accent : Theme.COLOR_TEXT_DIM;
            for (int i = 0; i < 3; i++) {
                float ly = gy + (11f + i * 4f) * s;
                dl.addLine(gx + 8f*s, ly, gx + gs - 8f*s, ly, iconColor, 1.5f*s);
            }

            ImGui.setCursorScreenPos(ghx, ghy);
            if (ImGui.invisibleButton("##gear_" + id, gearHit, gearHit)) {
                toggleExpanded(id);
            }
        }

        if (expanded) {
            float panelY = y + cardH;
            dl.addRectFilled(x + 6*s, panelY, x + width - 6*s, panelY + settingsH, ImGui.getColorU32(0, 0, 0, 0.16f), cardRadius);
            dl.addRect(x + 6*s, panelY, x + width - 6*s, panelY + settingsH, Theme.withAlpha(accent, 0.35f), cardRadius, 0, 1f*s);

            float rowY = panelY + 10*s;
            for (Setting<?> setting : module.getSettings()) {
                drawSetting(dl, font, setting, x, rowY, width, s, accent);
                rowY += ROW_H * s;
            }
            if (beModule != null) {
                rowY += 4f * s;
                drawBlockESPList(dl, font, beModule, x, rowY, width, s, accent);
                rowY += beModule.blocks.size() * BLOCK_ROW_H * s + 36f * s + 4f * s;
            }
            if (espModule != null) {
                rowY += 4f * s;
                drawESPEntityList(dl, font, espModule, x, rowY, width, s, accent);
            }
        }

        float swHitW = Math.max(swW + 8f * s, 44f * s);
        float swHitH = Math.max(swH + 16f * s, 44f * s);
        float shx = swX + swW / 2f - swHitW / 2f;
        float shy = swY + swH / 2f - swHitH / 2f;
        ImGui.setCursorScreenPos(shx, shy);
        ImGui.invisibleButton("##sw_" + id, swHitW, swHitH);
        if (ImGui.isItemHovered() || ImGui.isItemActive()) mouseOverControl = true;
        if (ImGui.isItemClicked()) {
            module.toggle();
            touchRecent(id);
            ConfigManager.markDirty();
            Notification.show(module.getName() + " " + Lang.get(module.isEnabled()
                    ? "cmeclient.notification.enabled" : "cmeclient.notification.disabled"));
        }

        return totalH + 15f*s;
    }

    // ===== BlockESP 方块列表 UI =====
    private static void drawBlockESPList(ImDrawList dl, ImFont font, BlockESP be, float x, float y, float width, float s, int accent) {
        float rowH = BLOCK_ROW_H * s;
        float left = x + 20f * s;
        float right = x + width - 20f * s;

        for (int i = 0; i < be.blocks.size(); i++) {
            BlockESP.BlockEntry entry = be.blocks.get(i);
            float rowY = y + i * rowH;
            float cy = rowY + rowH / 2f;

            float[] c = entry.color();
            int color = ImGui.getColorU32(c[0], c[1], c[2], 1f);
            float swSize = 18f * s;
            float swX = left;
            float swY = cy - swSize / 2f;
            boolean colorHovered = hovering(swX, swY, swX + swSize, swY + swSize);
            dl.addRectFilled(swX, swY, swX + swSize, swY + swSize, color, 4f * s);
            if (colorHovered) {
                dl.addRect(swX, swY, swX + swSize, swY + swSize, ImGui.getColorU32(1, 1, 1, 0.8f), 4f * s, 0, 1.5f * s);
                if (ImGui.isMouseClicked(0)) {
                    entry.nextColor();
                    ConfigManager.markDirty();
                }
            }

            float nameX = swX + swSize + 8f * s;
            float nameMaxW = Math.max(20f * s, right - 30f * s - nameX);
            dl.addText(font, 13f * s, nameX, cy - 7f * s, Theme.COLOR_TEXT,
                    UiUtil.fit(entry.id, nameMaxW, 13f * s));

            float delSize = 22f * s;
            float delX = right - delSize;
            float delY = cy - delSize / 2f;
            boolean delHovered = hovering(delX, delY, delX + delSize, delY + delSize);
            int delCol = delHovered ? ImGui.getColorU32(1f, 0.3f, 0.3f, 1f) : Theme.COLOR_TEXT_DIM;
            float pad = 6f * s;
            dl.addLine(delX + pad, delY + pad, delX + delSize - pad, delY + delSize - pad, delCol, 1.5f * s);
            dl.addLine(delX + delSize - pad, delY + pad, delX + pad, delY + delSize - pad, delCol, 1.5f * s);
            if (delHovered && ImGui.isMouseClicked(0)) {
                be.blocks.remove(i);
                ConfigManager.markDirty();
                break;
            }
        }

        float addY = y + be.blocks.size() * rowH + 4f * s;
        float addH = 32f * s;
        String addLabel = "+ " + Lang.get("cmeclient.blockesp.add");
        boolean addHovered = hovering(left, addY, right, addY + addH);
        int addBg = addHovered ? Theme.withAlpha(accent, 0.30f) : Theme.withAlpha(accent, 0.12f);
        dl.addRectFilled(left, addY, right, addY + addH, addBg, 6f * s);
        float textW = UiUtil.textWidth(addLabel, 14f * s);
        dl.addText(font, 14f * s, left + (right - left - textW) / 2f, addY + (addH - 14f * s) / 2f,
                addHovered ? accent : Theme.COLOR_TEXT_DIM, addLabel);

        if (addHovered && ImGui.isMouseClicked(0)) {
            addPopupRequested = true;
            addPopupTarget = be;
            addPopupSearch.set("");
            addPopupCachedQuery = "\0";
            addPopupResults.clear();
        }
    }


    // ===== ESP 实体列表 UI =====
    private static void drawESPEntityList(ImDrawList dl, ImFont font, ESP esp, float x, float y, float width, float s, int accent) {
        float rowH = BLOCK_ROW_H * s;
        float left = x + 20f * s;
        float right = x + width - 20f * s;

        for (int i = 0; i < esp.customEntities.size(); i++) {
            ESP.EntityEntry entry = esp.customEntities.get(i);
            float rowY = y + i * rowH;
            float cy = rowY + rowH / 2f;

            float[] c = entry.color();
            int color = ImGui.getColorU32(c[0], c[1], c[2], 1f);
            float swSize = 18f * s;
            float swX = left;
            float swY = cy - swSize / 2f;
            boolean colorHovered = hovering(swX, swY, swX + swSize, swY + swSize);
            dl.addRectFilled(swX, swY, swX + swSize, swY + swSize, color, 4f * s);
            if (colorHovered) {
                dl.addRect(swX, swY, swX + swSize, swY + swSize, ImGui.getColorU32(1, 1, 1, 0.8f), 4f * s, 0, 1.5f * s);
                if (ImGui.isMouseClicked(0)) {
                    entry.nextColor();
                    ConfigManager.markDirty();
                }
            }

            float nameX = swX + swSize + 8f * s;
            float nameMaxW = Math.max(20f * s, right - 30f * s - nameX);
            dl.addText(font, 13f * s, nameX, cy - 7f * s, Theme.COLOR_TEXT,
                    UiUtil.fit(entry.id, nameMaxW, 13f * s));

            float delSize = 22f * s;
            float delX = right - delSize;
            float delY = cy - delSize / 2f;
            boolean delHovered = hovering(delX, delY, delX + delSize, delY + delSize);
            int delCol = delHovered ? ImGui.getColorU32(1f, 0.3f, 0.3f, 1f) : Theme.COLOR_TEXT_DIM;
            float pad = 6f * s;
            dl.addLine(delX + pad, delY + pad, delX + delSize - pad, delY + delSize - pad, delCol, 1.5f * s);
            dl.addLine(delX + delSize - pad, delY + pad, delX + pad, delY + delSize - pad, delCol, 1.5f * s);
            if (delHovered && ImGui.isMouseClicked(0)) {
                esp.customEntities.remove(i);
                ConfigManager.markDirty();
                break;
            }
        }

        float addY = y + esp.customEntities.size() * rowH + 4f * s;
        float addH = 32f * s;
        String addLabel = "+ " + Lang.get("cmeclient.esp.add");
        boolean addHovered = hovering(left, addY, right, addY + addH);
        int addBg = addHovered ? Theme.withAlpha(accent, 0.30f) : Theme.withAlpha(accent, 0.12f);
        dl.addRectFilled(left, addY, right, addY + addH, addBg, 6f * s);
        float textW = UiUtil.textWidth(addLabel, 14f * s);
        dl.addText(font, 14f * s, left + (right - left - textW) / 2f, addY + (addH - 14f * s) / 2f,
                addHovered ? accent : Theme.COLOR_TEXT_DIM, addLabel);

        if (addHovered && ImGui.isMouseClicked(0)) {
            addPopupRequested = true;
            addEntityPopupTarget = esp;
            addPopupSearch.set("");
            addPopupCachedQuery = "\0";
            addPopupResults.clear();
        }
    }

    /** 在 ClickGUI.render 内调用一次，绘制方块/实体添加弹窗（共用）。 */
    public static void renderAddBlockPopup(float s) {
        if (addPopupRequested) {
            ImGui.openPopup("BlockESPAdd");
            addPopupRequested = false;
        }
        if (addPopupTarget == null && addEntityPopupTarget == null) return;

        ImBoolean open = new ImBoolean(true);
        if (!ImGui.beginPopupModal("BlockESPAdd", open, ImGuiWindowFlags.AlwaysAutoResize)) return;

        boolean entityMode = addEntityPopupTarget != null;
        ImGui.text(Lang.get(entityMode ? "cmeclient.esp.add_title" : "cmeclient.blockesp.add_title"));
        ImGui.setNextItemWidth(360f * s);
        if (ImGui.inputTextWithHint("##block_search",
                Lang.get(entityMode ? "cmeclient.esp.search_hint" : "cmeclient.blockesp.search_hint"),
                addPopupSearch)) {
            addPopupCachedQuery = "\0";
        }

        String q = addPopupSearch.get().trim().toLowerCase();
        if (!q.equals(addPopupCachedQuery)) {
            addPopupCachedQuery = q;
            addPopupResults.clear();
            List<String> universe = entityMode ? EntityIds.all() : BlockIds.all();
            for (String bid : universe) {
                if (!entityMode && bid.equals("minecraft:air")) continue;
                boolean already = false;
                if (entityMode) {
                    for (ESP.EntityEntry e : addEntityPopupTarget.customEntities) {
                        if (e.id.equals(bid)) { already = true; break; }
                    }
                } else {
                    for (BlockESP.BlockEntry e : addPopupTarget.blocks) {
                        if (e.id.equals(bid)) { already = true; break; }
                    }
                }
                if (already) continue;
                if (q.isEmpty() || bid.toLowerCase().contains(q)) {
                    addPopupResults.add(bid);
                    if (addPopupResults.size() >= 100) break;
                }
            }
        }

        ImGui.beginChild("##results", 360f * s, 220f * s, true);
        String toAdd = null;
        for (String bid : addPopupResults) {
            if (ImGui.selectable(bid)) { toAdd = bid; break; }
        }
        ImGui.endChild();

        if (toAdd != null) {
            if (entityMode) {
                int colorIdx = addEntityPopupTarget.customEntities.size() % ESP.PRESET_COLORS.length;
                addEntityPopupTarget.customEntities.add(new ESP.EntityEntry(toAdd, colorIdx));
            } else {
                int colorIdx = addPopupTarget.blocks.size() % BlockESP.PRESET_COLORS.length;
                addPopupTarget.blocks.add(new BlockESP.BlockEntry(toAdd, colorIdx));
            }
            ConfigManager.markDirty();
            ImGui.closeCurrentPopup();
            addPopupTarget = null;
            addEntityPopupTarget = null;
        }

        if (ImGui.button(Lang.get("cmeclient.blockesp.cancel"), 120f * s, 30f * s)) {
            ImGui.closeCurrentPopup();
            addPopupTarget = null;
            addEntityPopupTarget = null;
        }
        ImGui.endPopup();
    }

    public static ImVec2[] starPoints(float cx, float cy, float outer, float inner) {
        ImVec2[] pts = new ImVec2[10];
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2.0 + i * Math.PI / 5.0;
            float r = (i % 2 == 0) ? outer : inner;
            pts[i] = new ImVec2(cx + (float) Math.cos(angle) * r, cy + (float) Math.sin(angle) * r);
        }
        return pts;
    }

    private static void drawSetting(ImDrawList dl, ImFont font, Setting<?> setting, float x, float rowY, float width, float s, int accent) {
        String label = Lang.get(setting.getNameKey());
        float labelX = x + 24*s;
        float cy = rowY + ROW_H * s / 2f;

        float right = x + width - 20*s;
        float ctrlW = Math.min(width * 0.42f, 230f * s);
        float ctrlX = right - ctrlW;
        float labelEnd = setting instanceof FloatSetting ? Math.max(ctrlX, labelX + 90*s) : ctrlX;
        label = UiUtil.fit(label, Math.max(10f*s, labelEnd - labelX - 8f*s), 14f*s);
        dl.addText(font, 14f*s, labelX, cy - 8*s, Theme.COLOR_TEXT_DIM, label);

        if (setting instanceof FloatSetting fs) {
            drawFloat(dl, font, fs, labelX, ctrlX, ctrlW, cy, rowY, s, accent);
        } else if (setting instanceof BoolSetting bs) {
            drawBool(dl, font, bs, ctrlX, ctrlW, cy, s, accent);
        } else if (setting instanceof EnumSetting es) {
            drawEnum(dl, font, es, ctrlX, ctrlW, cy, s, accent);
        }
    }

    private static void drawFloat(ImDrawList dl, ImFont font, FloatSetting fs, float labelX, float ctrlX, float ctrlW, float cy, float rowY, float s, int accent) {
        float valueW = 44*s;
        float trackX = Math.max(ctrlX, labelX + 90*s);
        float trackW = ctrlX + ctrlW - valueW - trackX;
        if (trackW < 20*s) trackW = 20*s;
        float trackH = 6*s;
        float trackY = cy - trackH / 2f;
        float min = fs.getMin(), max = fs.getMax();
        float ratio = max > min ? (fs.get() - min) / (max - min) : 0f;
        ratio = Math.max(0f, Math.min(1f, ratio));

        int trackBg = ImGui.getColorU32(1f, 1f, 1f, 0.14f);
        dl.addRectFilled(trackX, trackY, trackX + trackW, trackY + trackH, trackBg, 3*s);
        dl.addRectFilled(trackX, trackY, trackX + trackW * ratio, trackY + trackH, accent, 3*s);
        dl.addCircleFilled(trackX + trackW * ratio, cy, 7*s, accent);

        String value = String.format("%.1f", fs.get());
        float vw = UiUtil.textWidth(value, 14f * s);
        dl.addText(font, 14*s, ctrlX + ctrlW - vw, cy - 8*s, Theme.COLOR_TEXT, value);

        float hitL = trackX - 8*s, hitR = trackX + trackW + 8*s;
        float hitT = rowY + 4*s, hitB = rowY + ROW_H * s - 4*s;
        boolean hovered = hovering(hitL, hitT, hitR, hitB);
        if (hovered) mouseOverControl = true;

        String sliderId = fs.getNameKey();
        if (hovered && ImGui.isMouseClicked(0)) {
            if (isDoubleClick("float:" + sliderId)) {
                fs.reset();
                activeSlider = null;
                mouseOverControl = true;
                ConfigManager.markDirty();
            } else {
                activeSlider = sliderId;
            }
        }
        if (sliderId.equals(activeSlider)) {
            if (ImGui.isMouseDown(0)) {
                float t = (ImGui.getMousePosX() - trackX) / trackW;
                t = Math.max(0f, Math.min(1f, t));
                fs.setClamped(min + (max - min) * t);
                mouseOverControl = true;
                ConfigManager.markDirty();
            } else {
                activeSlider = null;
            }
        }
    }

    private static void drawBool(ImDrawList dl, ImFont font, BoolSetting bs, float ctrlX, float ctrlW, float cy, float s, int accent) {
        float w = 42*s, h = 22*s;
        float tx = ctrlX + ctrlW - w, ty = cy - h / 2f;
        boolean hovered = hovering(ctrlX, cy - 15*s, ctrlX + ctrlW, cy + 15*s);
        if (hovered) mouseOverControl = true;

        int bg = bs.get() ? accent : ImGui.getColorU32(1f, 1f, 1f, 0.16f);
        dl.addRectFilled(tx, ty, tx + w, ty + h, bg, h / 2f);
        float knobX = bs.get() ? tx + w - h / 2f : tx + h / 2f;
        dl.addCircleFilled(knobX, cy, h / 2f - 2*s, bs.get() ? ImGui.getColorU32(0.1f, 0.1f, 0.1f, 1f) : ImGui.getColorU32(0.85f, 0.85f, 0.9f, 1f));

        if (hovered && ImGui.isMouseClicked(0)) {
            if (isDoubleClick("bool:" + bs.getNameKey())) bs.reset();
            else bs.toggle();
            ConfigManager.markDirty();
        }
    }

    private static void drawEnum(ImDrawList dl, ImFont font, EnumSetting es, float ctrlX, float ctrlW, float cy, float s, int accent) {
        float w = ctrlW, h = 24*s;
        float px = ctrlX, py = cy - h / 2f;
        boolean hovered = hovering(ctrlX, cy - 15*s, ctrlX + ctrlW, cy + 15*s);
        if (hovered) mouseOverControl = true;

        dl.addRectFilled(px, py, px + w, py + h, ImGui.getColorU32(1f, 1f, 1f, hovered ? 0.16f : 0.09f), 6*s);
        dl.addRect(px, py, px + w, py + h, Theme.withAlpha(accent, 0.65f), 6*s, 0, 1*s);

        String current = UiUtil.fit(
                Lang.option(es.getNameKey(), es.get(), es.getCurrent()), w - 34*s, 14f*s);
        float tw = UiUtil.textWidth(current, 14f * s);
        dl.addText(font, 14*s, px + (w - tw) / 2f, cy - 8*s, Theme.COLOR_TEXT, current);

        float ax = px + w - 12*s;
        dl.addTriangleFilled(ax - 4*s, cy - 4*s, ax + 4*s, cy - 4*s, ax, cy + 4*s, accent);
        float lx = px + 12*s;
        dl.addTriangleFilled(lx + 4*s, cy - 4*s, lx - 4*s, cy - 4*s, lx, cy + 4*s, accent);

        if (hovered && ImGui.isMouseClicked(0)) {
            if (isDoubleClick("enum:" + es.getNameKey())) {
                es.reset();
            } else if (ImGui.getMousePosX() < px + w * 0.32f) {
                es.prev();
            } else {
                es.next();
            }
            ConfigManager.markDirty();
        }
    }
}
