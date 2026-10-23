package com.chengmo.cmeclient.gui;

import com.chengmo.cmeclient.gui.component.ModuleCard;
import com.chengmo.cmeclient.gui.component.SettingPanel;
import com.chengmo.cmeclient.gui.component.Theme;
import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.setting.Setting;
import com.chengmo.cmeclient.util.ConfigManager;
import com.chengmo.cmeclient.util.Lang;
import com.chengmo.cmeclient.util.UiUtil;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.flag.ImDrawFlags;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import java.util.HashMap;
import java.util.Map;

public class ClickGUI {
    public static boolean isOpen = true;
    public static Module listeningModule = null;
    public static int bgTexId = -1;

    public static float uiScale = 1.0f;
    public static int themeIndex = 0;
    public static final String[] THEME_KEYS = {
            "cmeclient.theme.royal", "cmeclient.theme.glass", "cmeclient.theme.ink",
            "cmeclient.theme.sky", "cmeclient.theme.contrast"
    };
    public static final float[] customTextColor = {0.95f, 0.95f, 0.95f};

    public static Module.Category currentCategory = Module.Category.MISC;
    /** True while the sidebar shows the pinned modules instead of a category. */
    public static boolean favoritesView = false;
    /** True while the sidebar shows the most recently toggled modules. */
    public static boolean recentView = false;
    /** Optional mobile convenience: tapping the dimmed backdrop closes the GUI. */
    public static boolean closeOnOutsideClick = false;
    private static final Map<String, Float> hoverAnims = new HashMap<>();
    private static float menuAlpha = 0.0f;
    private static final ImString search = new ImString();

    public static float panelOffsetX = 0, panelOffsetY = 0;
    private static boolean dragging = false;
    private static float dragStartX, dragStartY, dragOrigX, dragOrigY;

    /** Card rectangles captured each frame, used for long-press favourite reordering. */
    private record CardRect(String id, float x, float y, float w, float h) {}
    private static final java.util.List<CardRect> cardRects = new java.util.ArrayList<>();
    private static String reorderId = null;
    private static long reorderStart = 0L;
    private static boolean reorderActive = false;

    // 触摸拖动滚动（手机端没有滚轮）
    private static boolean scrollDragging = false;
    private static float scrollDragMouseY, scrollDragStart;
    private static String lastContentKey = "";

    private static int rgba(float r, float g, float b, float a) { return ImGui.getColorU32(r, g, b, a); }

    private static float textWidth(String text, float fontSize) {
        return UiUtil.textWidth(text, fontSize);
    }

    // 拼音/别名检索（只影响搜索，不涉及任何模块逻辑）
    private static final String[][] SEARCH_ALIASES = {
            {"godmode", "wudi", "shangdi", "busi", "god", "modes"},
            {"KillAura", "ka", "aura", "kill", "shalu"},
            {"BlockESP", "block", "fangkuai", "espblock"},
    };

    private static boolean matches(Module module, String query) {
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) return true;
        if (module.getName().toLowerCase().contains(q)) return true;
        if (module.getDescription().toLowerCase().contains(q)) return true;
        if (module.getCategory().displayName().toLowerCase().contains(q)) return true;
        for (Setting<?> setting : module.getSettings()) {
            if (Lang.get(setting.getNameKey()).toLowerCase().contains(q)) return true;
        }
        for (String[] alias : SEARCH_ALIASES) {
            if (!alias[0].equals(module.getId())) continue;
            for (int i = 1; i < alias.length; i++) {
                if (alias[i].contains(q)) return true;
            }
        }
        return false;
    }

    private static Module findModule(String id) {
        for (Module module : ModuleManager.INSTANCE.getModules()) {
            if (module.getId().equals(id)) return module;
        }
        return null;
    }

    public static void render() {
        if (!ImGuiManager.isInitialized()) return;
        Theme.apply(themeIndex, customTextColor);

        ImGuiIO io = ImGui.getIO();
        float screenW = io.getDisplaySizeX();
        float screenH = io.getDisplaySizeY();

        menuAlpha = ModuleCard.lerp(menuAlpha, isOpen ? 1f : 0f, 15f);
        if (!isOpen && menuAlpha < 0.01f) {
            dragging = false;
            scrollDragging = false;
            return;
        }

        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(screenW, screenH, ImGuiCond.Always);
        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, menuAlpha);

        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoBackground;

        if (ImGui.begin("CME Overlay", new ImBoolean(true), flags)) {
            ImDrawList dl = ImGui.getWindowDrawList();
            ImFont font = ImGui.getFont();
            // 以 720p 为基准随分辨率缩放，兼顾高分屏手机与小窗口
            float autoScale = Math.max(0.6f, Math.min(1.6f, screenH / 720f));
            float s = uiScale * autoScale;
            UiUtil.applyStyle(s);

            dl.addRectFilled(0, 0, screenW, screenH, rgba(0.0f, 0.0f, 0.0f, 0.3f * menuAlpha));

            // 自适应尺寸：小屏自动收缩，避免超出屏幕
            float margin = 16f * s;
            float panelW = Math.min(950f * s, screenW - margin * 2f);
            float panelH = Math.min(600f * s, screenH - margin * 2f);
            panelW = Math.max(panelW, Math.min(screenW, 320f * s));
            panelH = Math.max(panelH, Math.min(screenH, 340f * s));

            // 开关面板时轻微缩放，配合透明度形成淡入淡出
            float zoom = 0.94f + 0.06f * menuAlpha;
            panelW *= zoom;
            panelH *= zoom;

            float baseX = (screenW - panelW) / 2f;
            float baseY = (screenH - panelH) / 2f;
            float maxOffX = Math.max(0f, (screenW - panelW) / 2f - 8f * s);
            float maxOffY = Math.max(0f, (screenH - panelH) / 2f - 8f * s);
            panelOffsetX = Math.max(-maxOffX, Math.min(maxOffX, panelOffsetX));
            panelOffsetY = Math.max(-maxOffY, Math.min(maxOffY, panelOffsetY));

            float intro = 1f - menuAlpha;
            float startX = baseX + panelOffsetX;
            float startY = baseY + panelOffsetY + intro * 16f * s;

            // 手机端便捷：点击面板外的遮罩关闭界面
            if (closeOnOutsideClick && ImGui.isMouseClicked(0)) {
                float mx = io.getMousePosX();
                float my = io.getMousePosY();
                if (mx < startX || mx > startX + panelW || my < startY || my > startY + panelH) {
                    isOpen = false;
                }
            }

            float radius = 16f * s;
            dl.addRectFilled(startX + 10f * s, startY + 15f * s, startX + panelW + 15f * s, startY + panelH + 15f * s,
                    rgba(0f, 0f, 0f, 0.5f), radius);
            dl.addRectFilled(startX, startY, startX + panelW, startY + panelH, Theme.COLOR_PANEL_BG, radius);

            float navW = Math.min(180f * s, panelW * 0.32f);
            dl.addRectFilled(startX, startY, startX + navW, startY + panelH, Theme.COLOR_NAV_BG,
                    radius, ImDrawFlags.RoundCornersLeft);
            dl.addRect(startX, startY, startX + panelW, startY + panelH, Theme.COLOR_ACCENT, radius, 0, 1.5f * s);

            // 拖动区域只保留顶部细条与左侧标题栏，避开设置标签/搜索框/整类开关
            ImGui.setCursorScreenPos(startX, startY);
            ImGui.invisibleButton("##drag", Math.max(1f, panelW), 18f * s);
            boolean dragActive = ImGui.isItemActive();
            ImGui.setCursorScreenPos(startX, startY + 18f * s);
            ImGui.invisibleButton("##drag_nav", Math.max(1f, navW), 72f * s);
            dragActive = dragActive || ImGui.isItemActive();
            if (dragActive) {
                if (!dragging) {
                    dragging = true;
                    dragStartX = io.getMousePosX();
                    dragStartY = io.getMousePosY();
                    dragOrigX = panelOffsetX;
                    dragOrigY = panelOffsetY;
                }
                panelOffsetX = dragOrigX + (io.getMousePosX() - dragStartX);
                panelOffsetY = dragOrigY + (io.getMousePosY() - dragStartY);
                panelOffsetX = Math.max(-maxOffX, Math.min(maxOffX, panelOffsetX));
                panelOffsetY = Math.max(-maxOffY, Math.min(maxOffY, panelOffsetY));
                ConfigManager.markDirty();
            } else {
                dragging = false;
            }

            // 关闭按钮
            float closeSize = 30f * s;
            float closeX = startX + panelW - closeSize - 20f * s;
            float closeY = startY + 20f * s;
            boolean closeHovered = ImGui.isMouseHoveringRect(closeX, closeY, closeX + closeSize, closeY + closeSize);
            dl.addRectFilled(closeX, closeY, closeX + closeSize, closeY + closeSize,
                    closeHovered ? rgba(0.9f, 0.2f, 0.2f, 0.9f) : rgba(0.2f, 0.2f, 0.2f, 0.5f), 8f * s);
            int closeColor = closeHovered ? rgba(1f, 1f, 1f, 1f) : Theme.COLOR_TEXT_DIM;
            float cx1 = closeX + 10f * s, cy1 = closeY + 10f * s;
            float cx2 = closeX + closeSize - 10f * s, cy2 = closeY + closeSize - 10f * s;
            dl.addLine(cx1, cy1, cx2, cy2, closeColor, Math.max(1f, 2f * s));
            dl.addLine(cx1, cy2, cx2, cy1, closeColor, Math.max(1f, 2f * s));
            ImGui.setCursorScreenPos(closeX, closeY);
            if (ImGui.invisibleButton("##close", closeSize, closeSize)) isOpen = false;

            // 标题
            float titleSize = 24f * s;
            float titleMaxW = Math.max(20f * s, closeX - 8f * s - (startX + 26f * s));
            dl.addText(font, titleSize, startX + 26f * s, startY + 22f * s, Theme.COLOR_TEXT,
                    UiUtil.fit(Lang.get("cmeclient.gui.title"), titleMaxW, titleSize));
            dl.addText(font, 13f * s, startX + 27f * s, startY + 54f * s, Theme.COLOR_TEXT_DIM,
                    UiUtil.fit(Lang.get("cmeclient.gui.subtitle"), titleMaxW + 2f * s, 13f * s));

            // 导航
            float navItemX = startX + 16f * s;
            float navItemW = Math.max(1f, navW - 32f * s);
            float navTop = startY + 96f * s;
            float navBottom = startY + panelH - 44f * s;
            float navSlot = Math.max(18f * s, (navBottom - navTop) / 8f);
            float navItemH = Math.min(40f * s, Math.max(16f * s, navSlot - 4f * s));
            float navItemY = navTop;

            // 收藏夹入口（置于分类之上）
            {
                String favLabel = Lang.get("cmeclient.gui.favorites");
                boolean selected = favoritesView;
                boolean isHovered = ImGui.isMouseHoveringRect(navItemX, navItemY, navItemX + navItemW, navItemY + navItemH);
                float cur = hoverAnims.getOrDefault("FAVORITES", 0f);
                cur = ModuleCard.lerp(cur, isHovered ? 1f : 0f, 20f);
                hoverAnims.put("FAVORITES", cur);

                if (selected || cur > 0.01f) {
                    int bg = selected ? Theme.COLOR_ACCENT : Theme.withAlpha(Theme.COLOR_ACCENT, cur * 0.18f);
                    float shiftX = -3f * cur * s;
                    dl.addRectFilled(navItemX + shiftX, navItemY, navItemX + navItemW + shiftX, navItemY + navItemH,
                            bg, 8f * s);
                }

                float starCX = navItemX + 14f * s;
                float starCY = navItemY + navItemH / 2f;
                int starColor = selected ? rgba(0.1f, 0.1f, 0.1f, 1f) : Theme.COLOR_ACCENT;
                ImVec2[] star = ModuleCard.starPoints(starCX, starCY, 5.5f * s, 2.5f * s);
                dl.addPolyline(star, star.length, starColor, 0, Math.max(1f, 1.3f * s));

                dl.addText(font, 17f * s, navItemX + 26f * s, navItemY + (navItemH - 17f * s) / 2f,
                        selected ? rgba(0.1f, 0.1f, 0.1f, 1f) : Theme.COLOR_TEXT_DIM,
                        UiUtil.fit(favLabel, Math.max(10f * s, navItemW - 60f * s), 17f * s));

                String favCount = String.valueOf(ModuleCard.favorites.size());
                float countW = textWidth(favCount, 12f * s);
                dl.addText(font, 12f * s, navItemX + navItemW - 12f * s - countW,
                        navItemY + (navItemH - 12f * s) / 2f,
                        selected ? rgba(0.1f, 0.1f, 0.1f, 0.85f) : Theme.COLOR_TEXT_DIM, favCount);

                ImGui.setCursorScreenPos(navItemX, navItemY);
                if (ImGui.invisibleButton("##nav_favorites", navItemW, navItemH)) {
                    favoritesView = true;
                    recentView = false;
                    ConfigManager.markDirty();
                }
                navItemY += navSlot;
            }

            // 最近使用入口
            {
                String recentLabel = Lang.get("cmeclient.gui.recent");
                boolean selected = recentView;
                boolean isHovered = ImGui.isMouseHoveringRect(navItemX, navItemY, navItemX + navItemW, navItemY + navItemH);
                float cur = hoverAnims.getOrDefault("RECENT", 0f);
                cur = ModuleCard.lerp(cur, isHovered ? 1f : 0f, 20f);
                hoverAnims.put("RECENT", cur);

                if (selected || cur > 0.01f) {
                    int bg = selected ? Theme.COLOR_ACCENT : Theme.withAlpha(Theme.COLOR_ACCENT, cur * 0.18f);
                    float shiftX = -3f * cur * s;
                    dl.addRectFilled(navItemX + shiftX, navItemY, navItemX + navItemW + shiftX, navItemY + navItemH,
                            bg, 8f * s);
                }

                float clockCX = navItemX + 14f * s;
                float clockCY = navItemY + navItemH / 2f;
                int clockColor = selected ? rgba(0.1f, 0.1f, 0.1f, 1f) : Theme.COLOR_ACCENT;
                dl.addCircle(clockCX, clockCY, 5.5f * s, clockColor, 12, Math.max(1f, 1.3f * s));
                dl.addLine(clockCX, clockCY, clockCX, clockCY - 3f * s, clockColor, Math.max(1f, 1.3f * s));
                dl.addLine(clockCX, clockCY, clockCX + 2.5f * s, clockCY + 1.5f * s, clockColor, Math.max(1f, 1.3f * s));

                dl.addText(font, 17f * s, navItemX + 26f * s, navItemY + (navItemH - 17f * s) / 2f,
                        selected ? rgba(0.1f, 0.1f, 0.1f, 1f) : Theme.COLOR_TEXT_DIM,
                        UiUtil.fit(recentLabel, Math.max(10f * s, navItemW - 60f * s), 17f * s));

                String recentCount = String.valueOf(ModuleCard.recents.size());
                float countW = textWidth(recentCount, 12f * s);
                dl.addText(font, 12f * s, navItemX + navItemW - 12f * s - countW,
                        navItemY + (navItemH - 12f * s) / 2f,
                        selected ? rgba(0.1f, 0.1f, 0.1f, 0.85f) : Theme.COLOR_TEXT_DIM, recentCount);

                ImGui.setCursorScreenPos(navItemX, navItemY);
                if (ImGui.invisibleButton("##nav_recent", navItemW, navItemH)) {
                    recentView = true;
                    favoritesView = false;
                    ConfigManager.markDirty();
                }
                navItemY += navSlot;
            }

            for (Module.Category cat : Module.Category.values()) {
                boolean selected = !favoritesView && !recentView && currentCategory == cat;
                boolean isHovered = ImGui.isMouseHoveringRect(navItemX, navItemY, navItemX + navItemW, navItemY + navItemH);
                float cur = hoverAnims.getOrDefault(cat.name(), 0f);
                cur = ModuleCard.lerp(cur, isHovered ? 1f : 0f, 20f);
                hoverAnims.put(cat.name(), cur);

                int catColor = cat == Module.Category.SETTINGS ? Theme.COLOR_ACCENT : Theme.categoryColor(cat);
                if (selected || cur > 0.01f) {
                    int bg = selected ? catColor : Theme.withAlpha(catColor, cur * 0.18f);
                    float shiftX = -3f * cur * s;
                    dl.addRectFilled(navItemX + shiftX, navItemY, navItemX + navItemW + shiftX, navItemY + navItemH,
                            bg, 8f * s);
                }

                float dotX = navItemX + 14f * s;
                float dotY = navItemY + navItemH / 2f;
                dl.addCircleFilled(dotX, dotY, 4f * s, selected ? rgba(0.1f, 0.1f, 0.1f, 1f) : catColor);
                dl.addText(font, 17f * s, navItemX + 26f * s,
                        navItemY + (navItemH - 17f * s) / 2f,
                        selected ? rgba(0.1f, 0.1f, 0.1f, 1f) : Theme.COLOR_TEXT_DIM,
                        UiUtil.fit(cat.displayName(), Math.max(10f * s, navItemW - 38f * s), 17f * s));

                ImGui.setCursorScreenPos(navItemX, navItemY);
                if (ImGui.invisibleButton("##nav_" + cat.name(), navItemW, navItemH)) {
                    currentCategory = cat;
                    favoritesView = false;
                    recentView = false;
                    ConfigManager.markDirty();
                }
                navItemY += navSlot;
            }

            // 底部版本信息（限制在导航列内，避免窄屏溢出到内容区/滚动条）
            String versionText = UiUtil.fit(Lang.get("cmeclient.gui.version"),
                    Math.max(10f * s, navW - 38f * s), 11f * s);
            dl.addText(font, 11f * s, startX + 26f * s, startY + panelH - 26f * s,
                    Theme.withAlpha(Theme.COLOR_TEXT_DIM, 0.7f), versionText);

            float contentX = startX + navW + 26f * s;
            float contentRight = startX + panelW - 24f * s;
            float contentW = Math.max(1f, contentRight - contentX);

            if (!favoritesView && !recentView && currentCategory == Module.Category.SETTINGS) {
                SettingPanel.render(dl, font, contentX, startY + 24f * s, contentW, panelH - 48f * s, s,
                        customTextColor);
            } else {
                String header = favoritesView
                        ? Lang.get("cmeclient.gui.favorites")
                        : recentView
                        ? Lang.get("cmeclient.gui.recent")
                        : Lang.get("cmeclient.gui.category_header", currentCategory.displayName());
                float headerY = startY + 30f * s;
                float reservedRight = textWidth("999", 12f * s) + 16f * s;
                if (!favoritesView && !recentView) {
                    float bulkReserve = textWidth(Lang.get("cmeclient.gui.enable_all"), 12f * s) + 20f * s + 8f * s;
                    reservedRight += Math.max(bulkReserve,
                            textWidth(Lang.get("cmeclient.gui.disable_all"), 12f * s) + 20f * s + 8f * s);
                }
                float headerMaxW = Math.max(30f * s, contentW - reservedRight - 12f * s);
                dl.addText(font, 20f * s, contentX, headerY, Theme.COLOR_TEXT,
                        UiUtil.fit(header, headerMaxW, 20f * s));

                ImGui.setCursorScreenPos(contentX, startY + 68f * s);
                ImGui.setNextItemWidth(contentW);
                ImGui.inputTextWithHint("##search", Lang.get("cmeclient.gui.search_hint"), search);

                // 搜索框清空按钮（触摸热区 30px）
                if (!search.get().isEmpty()) {
                    float itemMinX = ImGui.getItemRectMinX();
                    float itemMinY = ImGui.getItemRectMinY();
                    float itemMaxX = ImGui.getItemRectMaxX();
                    float itemMaxY = ImGui.getItemRectMaxY();
                    float clearSize = Math.max(30f * s, 26f * s);
                    float clearX = itemMaxX - clearSize - 2f * s;
                    float clearY = (itemMinY + itemMaxY - clearSize) / 2f;
                    float ccx = clearX + clearSize / 2f, ccy = clearY + clearSize / 2f;
                    boolean clearHovered = ImGui.isMouseHoveringRect(clearX, clearY, clearX + clearSize, clearY + clearSize);
                    dl.addCircleFilled(ccx, ccy, 8f * s,
                            clearHovered ? Theme.withAlpha(Theme.COLOR_ACCENT, 0.35f) : rgba(1f, 1f, 1f, 0.10f));
                    int xCol = clearHovered ? Theme.COLOR_ACCENT : Theme.COLOR_TEXT_DIM;
                    dl.addLine(ccx - 3f * s, ccy - 3f * s, ccx + 3f * s, ccy + 3f * s, xCol, Math.max(1f, 1.4f * s));
                    dl.addLine(ccx - 3f * s, ccy + 3f * s, ccx + 3f * s, ccy - 3f * s, xCol, Math.max(1f, 1.4f * s));
                    ImGui.setCursorScreenPos(clearX, clearY);
                    if (ImGui.invisibleButton("##clear_search", clearSize, clearSize)) search.set("");
                }

                // 搜索结果统计徽标（在输入框之后计算，避免慢一帧）
                boolean hasQuery = !search.get().trim().isEmpty();
                int badgeColor = (favoritesView || recentView) ? Theme.COLOR_ACCENT : Theme.categoryColor(currentCategory);
                java.util.List<Module> visible = new java.util.ArrayList<>();
                if (favoritesView) {
                    for (String favId : ModuleCard.favorites) {
                        Module module = findModule(favId);
                        if (module != null && matches(module, search.get())) visible.add(module);
                    }
                } else if (recentView) {
                    for (String recentId : ModuleCard.recents) {
                        Module module = findModule(recentId);
                        if (module != null && matches(module, search.get())) visible.add(module);
                    }
                } else {
                    for (Module module : ModuleManager.INSTANCE.getModules()) {
                        if (!hasQuery && module.getCategory() != currentCategory) continue;
                        if (!matches(module, search.get())) continue;
                        visible.add(module);
                    }
                }
                if (hasQuery) {
                    visible.sort(java.util.Comparator
                            .comparingInt((Module m) -> m.getCategory().ordinal())
                            .thenComparing(Module::getName));
                }
                String countText = String.valueOf(visible.size());
                float badgeW = textWidth(countText, 12f * s) + 16f * s;
                float badgeH = 20f * s;
                float badgeX = contentRight - badgeW;
                float badgeY = headerY + 4f * s;
                dl.addRectFilled(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH,
                        Theme.withAlpha(badgeColor, 0.22f), badgeH / 2f);
                dl.addText(font, 12f * s, badgeX + 8f * s, badgeY + 3f * s, badgeColor, countText);

                // 分类整类开关（收藏/最近视图不显示）
                if (!favoritesView && !recentView) {
                    boolean allEnabled = true;
                    java.util.List<Module> categoryModules = new java.util.ArrayList<>();
                    for (Module module : ModuleManager.INSTANCE.getModules()) {
                        if (module.getCategory() != currentCategory) continue;
                        categoryModules.add(module);
                        if (!module.isEnabled()) allEnabled = false;
                    }
                    String bulkLabel = Lang.get(allEnabled ? "cmeclient.gui.disable_all" : "cmeclient.gui.enable_all");
                    float bulkW = textWidth(bulkLabel, 12f * s) + 20f * s;
                    float bulkH = 22f * s;
                    float bulkX = badgeX - 8f * s - bulkW;
                    float bulkY = headerY + 3f * s;
                    boolean bulkHovered = ImGui.isMouseHoveringRect(bulkX, bulkY, bulkX + bulkW, bulkY + bulkH);
                    dl.addRectFilled(bulkX, bulkY, bulkX + bulkW, bulkY + bulkH,
                            bulkHovered ? Theme.withAlpha(badgeColor, 0.30f) : Theme.withAlpha(badgeColor, 0.12f),
                            bulkH / 2f);
                    dl.addText(font, 12f * s, bulkX + 10f * s, bulkY + 3f * s,
                            bulkHovered ? badgeColor : Theme.COLOR_TEXT_DIM, bulkLabel);
                    ImGui.setCursorScreenPos(bulkX, bulkY);
                    if (ImGui.invisibleButton("##bulk_toggle", bulkW, bulkH)) {
                        boolean target = !allEnabled;
                        for (Module module : categoryModules) {
                            if (module.isEnabled() != target) module.toggle();
                        }
                        ConfigManager.markDirty();
                        Notification.show(Lang.get(target
                                ? "cmeclient.gui.all_enabled" : "cmeclient.gui.all_disabled", currentCategory.displayName()));
                    }
                }

                float contentTop = startY + 116f * s;
                float contentH = Math.max(40f * s, startY + panelH - 24f * s - contentTop);
                String contentKey = (favoritesView ? "FAVORITES"
                        : recentView ? "RECENT" : currentCategory.name()) + "|" + search.get();

                if (favoritesView && !visible.isEmpty()) {
                    dl.addText(font, 11f * s, contentX + 2f * s, contentTop - 15f * s,
                            Theme.withAlpha(Theme.COLOR_TEXT_DIM, 0.8f),
                            UiUtil.fit(Lang.get("cmeclient.gui.favorite_order_hint"),
                                    Math.max(10f * s, contentW - 4f * s), 11f * s));
                }

                ImGui.setCursorScreenPos(contentX, contentTop);
                ImGui.beginChild("##content", contentW, contentH, false, ImGuiWindowFlags.NoScrollbar);

                float scrollY = ImGui.getScrollY();
                if (!contentKey.equals(lastContentKey)) {
                    lastContentKey = contentKey;
                    ImGui.setScrollY(0f);
                    scrollY = 0f;
                    scrollDragging = false;
                }

                // 触摸/按住拖动滚动
                boolean overControl = ModuleCard.mouseOverControl;
                ModuleCard.mouseOverControl = false;
                boolean inContent = ImGui.isMouseHoveringRect(contentX, contentTop,
                        contentX + contentW, contentTop + contentH);
                if (inContent && !overControl && !scrollDragging && ImGui.isMouseClicked(0)) {
                    scrollDragging = true;
                    scrollDragMouseY = io.getMousePosY();
                    scrollDragStart = scrollY;
                }
                if (scrollDragging) {
                    if (ImGui.isMouseDown(0)) {
                        float target = scrollDragStart - (io.getMousePosY() - scrollDragMouseY);
                        target = Math.max(0f, Math.min(ImGui.getScrollMaxY(), target));
                        ImGui.setScrollY(target);
                        scrollY = target;
                    } else {
                        scrollDragging = false;
                    }
                }

                ModuleCard.hoveredModule = null;
                ModuleCard.clipMinX = contentX;
                ModuleCard.clipMinY = contentTop;
                ModuleCard.clipMaxX = contentX + contentW;
                ModuleCard.clipMaxY = contentTop + contentH;
                dl.pushClipRect(contentX, contentTop, contentX + contentW, contentTop + contentH, true);
                int columns = (contentW >= 620f * s && !hasQuery) ? 2 : 1;
                float columnGap = 12f * s;
                float cardW = (contentW - columnGap * (columns - 1)) / columns;
                float y = 0f;
                cardRects.clear();
                int cardIndex = 0;
                Module.Category lastSearchCat = null;
                while (cardIndex < visible.size()) {
                    Module firstModule = visible.get(cardIndex);
                    if (hasQuery && firstModule.getCategory() != lastSearchCat) {
                        lastSearchCat = firstModule.getCategory();
                        int catCol = Theme.categoryColor(lastSearchCat);
                        float headerScreenY = contentTop + y - scrollY;
                        dl.addCircleFilled(contentX + 4f * s, headerScreenY + 9f * s, 3f * s, catCol);
                        String groupLabel = UiUtil.fit(lastSearchCat.displayName(), contentW - 34f * s, 12f * s);
                        dl.addText(font, 12f * s, contentX + 13f * s, headerScreenY, catCol,
                                groupLabel);
                        float groupLabelW = textWidth(groupLabel, 12f * s);
                        dl.addLine(contentX + 21f * s + groupLabelW, headerScreenY + 8f * s,
                                contentX + contentW, headerScreenY + 8f * s,
                                Theme.withAlpha(catCol, 0.22f), 1f * s);
                        y += 24f * s;
                        continue;
                    }
                    // 展开的设置面板占满整行
                    if (columns > 1 && ModuleCard.isExpanded(firstModule.getId())) {
                        float cardScreenY = contentTop + y - scrollY;
                        float h = ModuleCard.draw(dl, font, firstModule, contentX, cardScreenY,
                                contentW, s, themeIndex);
                        cardRects.add(new CardRect(firstModule.getId(), contentX, cardScreenY, contentW, h));
                        y += h;
                        cardIndex++;
                        continue;
                    }
                    float rowH = 0f;
                    for (int c = 0; c < columns && cardIndex < visible.size(); c++) {
                        Module module = visible.get(cardIndex);
                        if (columns > 1 && ModuleCard.isExpanded(module.getId())) break;
                        float cardX = contentX + c * (cardW + columnGap);
                        float cardScreenY = contentTop + y - scrollY;
                        float h = ModuleCard.draw(dl, font, module, cardX, cardScreenY, cardW, s, themeIndex);
                        cardRects.add(new CardRect(module.getId(), cardX, cardScreenY, cardW, h));
                        rowH = Math.max(rowH, h);
                        cardIndex++;
                    }
                    y += rowH;
                }

                // 收藏视图：长按卡片拖动排序
                boolean overControlNow = ModuleCard.mouseOverControl;
                if (favoritesView) {
                    if (!ImGui.isMouseDown(0)) {
                        if (reorderActive && reorderId != null) {
                            int target = -1;
                            for (int r = 0; r < cardRects.size(); r++) {
                                CardRect rect = cardRects.get(r);
                                if (io.getMousePosX() >= rect.x() && io.getMousePosX() <= rect.x() + rect.w()
                                        && io.getMousePosY() >= rect.y() && io.getMousePosY() <= rect.y() + rect.h()) {
                                    target = r;
                                    break;
                                }
                            }
                            if (target >= 0) {
                                java.util.List<String> order = new java.util.ArrayList<>(ModuleCard.favorites);
                                String draggedId = reorderId;
                                int from = order.indexOf(draggedId);
                                String targetId = cardRects.get(target).id();
                                int to = order.indexOf(targetId);
                                if (from >= 0 && to >= 0 && from != to) {
                                    order.remove(from);
                                    order.add(to, draggedId);
                                    ModuleCard.favorites.clear();
                                    ModuleCard.favorites.addAll(order);
                                    ConfigManager.markDirty();
                                }
                            }
                        }
                        reorderActive = false;
                        reorderId = null;
                        reorderStart = 0L;
                    } else if (reorderId == null) {
                        if (!overControlNow && inContent) {
                            for (CardRect rect : cardRects) {
                                if (io.getMousePosX() >= rect.x() && io.getMousePosX() <= rect.x() + rect.w()
                                        && io.getMousePosY() >= rect.y() && io.getMousePosY() <= rect.y() + rect.h()) {
                                    reorderId = rect.id();
                                    reorderStart = System.currentTimeMillis();
                                    break;
                                }
                            }
                        }
                    } else if (reorderActive) {
                        scrollDragging = false;
                        // 目标位置提示线
                        for (CardRect rect : cardRects) {
                            if (rect.id().equals(reorderId)) continue;
                            float mid = rect.y() + rect.h() / 2f;
                            if (io.getMousePosY() < mid) {
                                dl.addLine(rect.x(), rect.y() - 3f * s, rect.x() + rect.w(), rect.y() - 3f * s,
                                        Theme.COLOR_ACCENT, Math.max(2f, 3f * s));
                                break;
                            }
                        }
                        // 跟随手指的拖动幽灵
                        Module dragged = findModule(reorderId);
                        if (dragged != null) {
                            float gx = io.getMousePosX();
                            float gy = io.getMousePosY();
                            String name = UiUtil.fit(dragged.getName(),
                                    Math.max(20f * s, Math.min(220f * s, screenW * 0.6f)), 14f * s);
                            float nw = textWidth(name, 14f * s);
                            float gw = Math.min(screenW - 8f * s, nw + 32f * s);
                            float gh = 36f * s;
                            float ghostX = UiUtil.clamp(gx, gw / 2f + 4f * s, screenW - gw / 2f - 4f * s);
                            dl.addRectFilled(ghostX - gw / 2f, gy - gh / 2f, ghostX + gw / 2f, gy + gh / 2f,
                                    Theme.withAlpha(Theme.COLOR_ACCENT, 0.35f), Theme.r(10f, s));
                            dl.addRect(ghostX - gw / 2f, gy - gh / 2f, ghostX + gw / 2f, gy + gh / 2f,
                                    Theme.COLOR_ACCENT, Theme.r(10f, s), 0, 1.5f * s);
                            dl.addText(font, 14f * s, ghostX - nw / 2f, gy - 8f * s, Theme.COLOR_TEXT, name);
                        }
                    } else if (System.currentTimeMillis() - reorderStart >= 450L) {
                        reorderActive = true;
                        scrollDragging = false;
                    }
                }
                ModuleCard.clipMinX = -Float.MAX_VALUE;
                ModuleCard.clipMinY = -Float.MAX_VALUE;
                ModuleCard.clipMaxX = Float.MAX_VALUE;
                ModuleCard.clipMaxY = Float.MAX_VALUE;
                if (visible.isEmpty()) {
                    String empty = Lang.get(favoritesView ? "cmeclient.gui.favorites_empty"
                            : recentView ? "cmeclient.gui.recent_empty" : "cmeclient.gui.no_results");
                    float emptySize = 14f * s;
                    String emptyText = UiUtil.fit(empty, Math.max(10f * s, contentW - 12f * s), emptySize);
                    float emptyW = textWidth(emptyText, emptySize);
                    dl.addText(font, emptySize, contentX + (contentW - emptyW) / 2f, contentTop + 60f * s,
                            Theme.COLOR_TEXT_DIM, emptyText);
                }
                dl.popClipRect();

                float scrollMax = ImGui.getScrollMaxY();
                if (scrollMax > 1f) {
                    float trackX = contentX + contentW + 8f * s;
                    float trackW = Math.max(2f, 3f * s);
                    dl.addRectFilled(trackX, contentTop, trackX + trackW, contentTop + contentH,
                            Theme.withAlpha(Theme.COLOR_TEXT_DIM, 0.12f), trackW / 2f);
                    float ratio = contentH / (contentH + scrollMax);
                    float thumbH = Math.max(24f * s, contentH * ratio);
                    float travel = Math.max(0f, contentH - thumbH);
                    float thumbY = contentTop + travel * (scrollY / scrollMax);
                    dl.addRectFilled(trackX, thumbY, trackX + trackW, thumbY + thumbH,
                            Theme.withAlpha(Theme.COLOR_ACCENT, 0.75f), trackW / 2f);
                }

                // 撑开内容高度，保证滚动范围正确
                ImGui.setCursorScreenPos(contentX, contentTop + y - scrollY);
                ImGui.dummy(contentW, 8f * s);

                ImGui.endChild();
            }
            ModuleCard.renderAddBlockPopup(s);
        }
        ImGui.end();
        ImGui.popStyleVar();
    }
}