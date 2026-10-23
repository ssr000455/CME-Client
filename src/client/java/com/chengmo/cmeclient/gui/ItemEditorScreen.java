package com.chengmo.cmeclient.gui;

import com.chengmo.cmeclient.util.Lang;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ItemEditorScreen {
    public static boolean isOpen = false;
    private static ItemStack editing = ItemStack.EMPTY;
    private static final ImString nameInput = new ImString(256);
    private static final ImString damageInput = new ImString(16);
    private static final ImString searchInput = new ImString(64);
    private static int currentTab = 0;
    private static final Map<Enchantment, Integer> editingEnchants = new LinkedHashMap<>();
    private static final Map<EntityAttribute, EntityAttributeModifier> editingAttrs = new LinkedHashMap<>();

    private ItemEditorScreen() {}

    public static boolean isAvailable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.isInSingleplayer() && mc.player != null;
    }

    public static void open() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !isAvailable()) return;
        ItemStack held = mc.player.getMainHandStack();
        if (held.isEmpty()) {
            Notification.warn(Lang.get("cmeclient.itemeditor.empty_hand"));
            return;
        }

        // ===== 进度保存核心逻辑 =====
        // 如果当前主手物品与上次编辑的物品一致，直接打开，不重置数据
        if (!editing.isEmpty() && ItemStack.areEqual(held, editing)) {
            isOpen = true;
            return;
        }

        // 换了物品，重新读取并初始化
        editing = held.copy();
        editingEnchants.clear();
        editingAttrs.clear();
        nameInput.set(editing.hasCustomName() ? editing.getName().getString() : "");
        damageInput.set(String.valueOf(editing.getDamage()));
        searchInput.set("");
        currentTab = 0;

        for (NbtElement el : editing.getEnchantments()) {
            if (el instanceof NbtCompound c) {
                Identifier id = Identifier.tryParse(c.getString("id"));
                if (id != null && Registries.ENCHANTMENT.containsId(id)) {
                    editingEnchants.put(Registries.ENCHANTMENT.get(id), (int) c.getShort("lvl"));
                }
            }
        }
        var mods = editing.getAttributeModifiers(EquipmentSlot.MAINHAND);
        for (var entry : mods.entries()) {
            editingAttrs.put(entry.getKey(), entry.getValue());
        }
        isOpen = true;
    }

    public static void close() {
        // 关闭时不清理 editing 数据，以实现再次打开时的进度保留
        isOpen = false;
    }

    public static void prepareIcon() {}

    public static void render() {
        if (!isOpen) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) { close(); return; }

        ImGuiIO io = ImGui.getIO();
        float screenW = io.getDisplaySizeX();
        float screenH = io.getDisplaySizeY();
        float s = Math.max(0.6f, Math.min(1.6f, screenH / 720f));

        float w = Math.min(860f * s, screenW - 40f * s);
        float h = Math.min(580f * s, screenH - 40f * s);
        ImGui.setNextWindowPos((screenW - w) / 2f, (screenH - h) / 2f, ImGuiCond.Always);
        ImGui.setNextWindowSize(w, h, ImGuiCond.Always);

        int flags = ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoSavedSettings | ImGuiWindowFlags.NoTitleBar;
        
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f);
        if (!ImGui.begin("##ItemEditorWindow", flags)) {
            ImGui.end();
            ImGui.popStyleVar();
            return;
        }

        // ===== 顶部：物品预览与信息 =====
        float topH = 100f * s;
        ImGui.beginChild("##TopBar", w, topH, true);
        
        ImGui.setCursorPos(20f * s, 16f * s);
        ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, 1.0f);
        ImGui.text(editing.getName().getString());
        ImGui.popStyleColor();
        
        ImGui.setCursorPos(20f * s, 44f * s);
        String itemId = Registries.ITEM.getId(editing.getItem()).toString();
        ImGui.pushStyleColor(ImGuiCol.Text, 0.6f, 0.6f, 0.6f, 1.0f);
        ImGui.text(itemId);
        ImGui.popStyleColor();
        
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(Lang.get("cmeclient.itemeditor.click_to_copy"));
        }
        if (ImGui.isItemClicked()) {
            mc.keyboard.setClipboard(itemId);
            Notification.success(Lang.get("cmeclient.itemeditor.copied", itemId));
        }
        
        ImGui.setCursorPos(20f * s, 68f * s);
        ImGui.pushStyleColor(ImGuiCol.Text, 0.45f, 0.45f, 0.45f, 1.0f);
        if (editing.isDamageable()) {
            ImGui.text(String.format("Durability: %d / %d", editing.getMaxDamage() - editing.getDamage(), editing.getMaxDamage()));
        } else {
            ImGui.text(Lang.get("cmeclient.itemeditor.not_damageable"));
        }
        ImGui.popStyleColor();
        
        ImGui.setCursorPos(w - 56f * s, 16f * s);
        if (ImGui.button("X##close", 36f * s, 36f * s)) close();
        
        ImGui.endChild();
        ImGui.separator();

        // ===== 下方主体区 =====
        float bodyH = h - topH - 60f * s - 10f * s;
        float navW = 140f * s;

        // 左侧导航
        ImGui.beginChild("##Nav", navW, bodyH, true);
        ImGui.setCursorPos(10f * s, 10f * s);
        String[] tabs = {
                Lang.get("cmeclient.itemeditor.tab.basic"),
                Lang.get("cmeclient.itemeditor.tab.enchant"),
                Lang.get("cmeclient.itemeditor.tab.attribute")
        };
        for (int i = 0; i < tabs.length; i++) {
            boolean selected = (currentTab == i);
            if (selected) {
                ImGui.pushStyleColor(ImGuiCol.Header, 0.85f, 0.65f, 0.13f, 0.8f);
                ImGui.pushStyleColor(ImGuiCol.Text, 0.1f, 0.1f, 0.1f, 1.0f);
            }
            if (ImGui.selectable(tabs[i], selected, 0, 0, 34f * s)) {
                currentTab = i;
                searchInput.set("");
            }
            if (selected) ImGui.popStyleColor(2);
        }
        ImGui.endChild();

        ImGui.sameLine();

        // 右侧内容
        ImGui.beginChild("##Content", w - navW - 16f * s, bodyH, true);
        ImGui.setCursorPos(16f * s, 16f * s);
        switch (currentTab) {
            case 0 -> renderBasic(s);
            case 1 -> renderEnchant(s);
            case 2 -> renderAttribute(s);
        }
        ImGui.endChild();

        // ===== 底部操作栏 =====
        ImGui.setCursorPos(0f, h - 60f * s);
        ImGui.beginChild("##Bottom", w, 60f * s, true);
        ImGui.setCursorPos(20f * s, 14f * s);
        if (ImGui.button(Lang.get("cmeclient.itemeditor.apply"), 100f * s, 32f * s)) {
            apply();
            // 应用成功后可以保留界面或者关闭，这里不强制关闭，但通过 ItemStack.areEqual 会刷新状态
        }
        ImGui.sameLine();
        if (ImGui.button(Lang.get("cmeclient.itemeditor.reset"), 100f * s, 32f * s)) {
            // 强制重置读取手上的物品
            editing = ItemStack.EMPTY;
            open();
        }
        ImGui.sameLine();
        if (ImGui.button(Lang.get("cmeclient.itemeditor.close"), 100f * s, 32f * s)) close();
        ImGui.endChild();

        ImGui.end();
        ImGui.popStyleVar();
    }

    private static void renderBasic(float s) {
        ImGui.text(Lang.get("cmeclient.itemeditor.name"));
        ImGui.setNextItemWidth(400f * s);
        ImGui.inputText("##name", nameInput);

        ImGui.dummy(0, 20f * s);
        ImGui.text(Lang.get("cmeclient.itemeditor.damage"));
        ImGui.setNextItemWidth(200f * s);
        if (editing.isDamageable()) {
            int[] dmg = {editing.getDamage()};
            try { dmg[0] = Integer.parseInt(damageInput.get().trim()); } catch (Exception ignored) {}
            if (ImGui.sliderInt("##damage", dmg, 0, editing.getMaxDamage())) {
                damageInput.set(String.valueOf(dmg[0]));
            }
            ImGui.textDisabled(Lang.get("cmeclient.itemeditor.damage_hint"));
        } else {
            ImGui.textDisabled(Lang.get("cmeclient.itemeditor.not_damageable"));
        }
    }

    private static void renderEnchant(float s) {
        ImGui.text(Lang.get("cmeclient.itemeditor.current_enchant"));
        ImGui.beginChild("##ench_list", 0, 180f * s, true);
        List<Enchantment> toRemove = new ArrayList<>();
        for (var e : editingEnchants.entrySet()) {
            String name = e.getKey().getName(2).getString();
            ImGui.text(name);
            ImGui.sameLine(250f * s);
            ImGui.text("Lv." + e.getValue());
            ImGui.sameLine(300f * s);
            if (ImGui.button("-##rm_" + name.hashCode())) toRemove.add(e.getKey());
        }
        for (Enchantment e : toRemove) editingEnchants.remove(e);
        ImGui.endChild();

        ImGui.dummy(0, 10f * s);
        ImGui.text(Lang.get("cmeclient.itemeditor.add_enchant"));
        ImGui.setNextItemWidth(300f * s);
        ImGui.inputTextWithHint("##ench_search", Lang.get("cmeclient.itemeditor.search_hint"), searchInput);
        ImGui.sameLine();
        ImGui.textDisabled(Lang.get("cmeclient.itemeditor.level_hint"));

        String q = searchInput.get().trim().toLowerCase();
        if (!q.isEmpty()) {
            ImGui.beginChild("##ench_results", 0, 150f * s, true);
            int count = 0;
            for (Enchantment ench : Registries.ENCHANTMENT) {
                String id = Registries.ENCHANTMENT.getId(ench).toString();
                if (!id.toLowerCase().contains(q)) continue;
                String label = ench.getName(2).getString() + " (" + id + ")";
                if (ImGui.selectable(label)) {
                    int level = editingEnchants.getOrDefault(ench, 0) + 1;
                    editingEnchants.put(ench, Math.min(level, 255));
                    searchInput.set("");
                    break;
                }
                if (++count >= 20) break;
            }
            ImGui.endChild();
        }
    }

    private static void renderAttribute(float s) {
        ImGui.text(Lang.get("cmeclient.itemeditor.current_attr"));
        ImGui.beginChild("##attr_list", 0, 180f * s, true);
        List<EntityAttribute> toRemove = new ArrayList<>();
        for (var e : editingAttrs.entrySet()) {
            String key = e.getKey().getTranslationKey().replace("attribute.name.", "");
            EntityAttributeModifier mod = e.getValue();
            ImGui.text(key);
            ImGui.sameLine(250f * s);
            ImGui.text(String.format("%.3f", mod.getValue()));
            ImGui.sameLine(350f * s);
            ImGui.text(mod.getOperation().name());
            ImGui.sameLine(500f * s);
            if (ImGui.button("-##rm_attr_" + key.hashCode())) toRemove.add(e.getKey());
        }
        for (EntityAttribute e : toRemove) editingAttrs.remove(e);
        ImGui.endChild();

        ImGui.dummy(0, 10f * s);
        ImGui.text(Lang.get("cmeclient.itemeditor.add_attr"));
        ImGui.beginChild("##attr_results", 0, 150f * s, true);
        for (EntityAttribute attr : Registries.ATTRIBUTE) {
            String key = attr.getTranslationKey().replace("attribute.name.", "");
            if (editingAttrs.containsKey(attr)) continue;
            if (ImGui.selectable(key)) {
                editingAttrs.put(attr, new EntityAttributeModifier(
                        UUID.randomUUID(), "cme_editor", 1.0, EntityAttributeModifier.Operation.ADDITION));
                break;
            }
        }
        ImGui.endChild();
    }

    private static void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) return;

        String nameStr = nameInput.get().trim();
        if (nameStr.isEmpty()) stack.removeCustomName();
        else stack.setCustomName(parseName(nameStr));

        if (stack.isDamageable()) {
            try { stack.setDamage(Integer.parseInt(damageInput.get().trim())); } catch (Exception ignored) {}
        }

        NbtCompound nbt = stack.getOrCreateNbt();
        NbtList enchList = new NbtList();
        for (var e : editingEnchants.entrySet()) {
            NbtCompound c = new NbtCompound();
            c.putString("id", Registries.ENCHANTMENT.getId(e.getKey()).toString());
            c.putShort("lvl", e.getValue().shortValue());
            enchList.add(c);
        }
        if (!enchList.isEmpty()) nbt.put("Enchantments", enchList);
        else nbt.remove("Enchantments");

        NbtList attrList = new NbtList();
        for (var e : editingAttrs.entrySet()) {
            NbtCompound c = e.getValue().toNbt();
            c.putString("Slot", "mainhand");
            c.putString("AttributeName", Registries.ATTRIBUTE.getId(e.getKey()).toString());
            attrList.add(c);
        }
        if (!attrList.isEmpty()) nbt.put("AttributeModifiers", attrList);
        else nbt.remove("AttributeModifiers");

        if (mc.getServer() != null) {
            var serverPlayer = mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());
            if (serverPlayer != null) {
                serverPlayer.getInventory().setStack(serverPlayer.getInventory().selectedSlot, stack.copy());
                serverPlayer.playerScreenHandler.syncState();
            }
        }

        // 将 editing 更新为应用后的最新物品，防止下次 open() 判断失效
        editing = stack.copy();
        Notification.success(Lang.get("cmeclient.itemeditor.applied"));
    }

    private static Text parseName(String input) {
        if (input.startsWith("{") || input.startsWith("[")) {
            try { return Text.Serializer.fromJson(input); } catch (Exception ignored) {}
        }
        MutableText result = Text.literal("");
        int i = 0;
        while (i < input.length()) {
            if (input.charAt(i) == '&' && i + 1 < input.length()) {
                Formatting fmt = Formatting.byCode(input.charAt(i + 1));
                if (fmt != null) {
                    result.append(Text.literal("").formatted(fmt));
                    i += 2; continue;
                }
            }
            result.append(Text.literal(String.valueOf(input.charAt(i))));
            i++;
        }
        return result;
    }
}
