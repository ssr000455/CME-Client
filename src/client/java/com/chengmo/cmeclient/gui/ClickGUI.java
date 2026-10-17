package com.chengmo.cmeclient.gui;

import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.ModuleManager;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;

import java.util.HashMap;
import java.util.Map;

public class ClickGUI {
    public static boolean isOpen = true;
    private static Module.Category currentCategory = Module.Category.COMBAT;
    private static final Map<String, Float> toggleAnimations = new HashMap<>();

    public static void render() {
        if (!isOpen || !ImGuiManager.isInitialized()) return;

        ImGui.setNextWindowSize(650, 450, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowPos(
            ImGui.getIO().getDisplaySizeX() / 2f, 
            ImGui.getIO().getDisplaySizeY() / 2f, 
            ImGuiCond.FirstUseEver, 
            0.5f, 0.5f
        );

        int windowFlags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse;
        
        if (ImGui.begin("CME Client", windowFlags)) {
            
            // 左侧导航
            ImGui.beginChild("NavBar", 140, 0, true);
            {
                ImGui.textColored(0.70f, 0.50f, 0.90f, 1.00f, "CME Client");
                ImGui.separator();
                ImGui.spacing();
                
                for (Module.Category cat : Module.Category.values()) {
                    boolean selected = (currentCategory == cat);
                    if (ImGui.selectable(cat.name, selected, 0, 0, 30)) {
                        currentCategory = cat;
                    }
                    ImGui.spacing();
                }
            }
            ImGui.endChild();

            ImGui.sameLine();

            // 右侧内容
            ImGui.beginChild("ContentArea", 0, 0, true);
            {
                for (Module module : ModuleManager.INSTANCE.getModules()) {
                    if (module.getCategory() == currentCategory) {
                        drawModuleCard(module);
                    }
                }
            }
            ImGui.endChild();
        }
        ImGui.end();
    }

    private static void drawModuleCard(Module module) {
        ImGui.pushID(module.getName());
        
        ImGui.beginChild(module.getName() + "_card", 0, 60, true);
        {
            ImGui.text(module.getName());
            ImGui.textDisabled(module.getDescription());

            ImGui.sameLine(ImGui.getWindowWidth() - 60);
            ImGui.setCursorPosY(ImGui.getCursorPosY() - 10);
            if (drawCustomSwitch(module.getName(), module.isEnabled())) {
                module.toggle();
            }
        }
        ImGui.endChild();
        ImGui.spacing();
        ImGui.popID();
    }

    private static boolean drawCustomSwitch(String id, boolean isOn) {
        float height = 24.0f;
        float width = 44.0f;
        
        float target = isOn ? 1.0f : 0.0f;
        float current = toggleAnimations.getOrDefault(id, 0.0f);
        current += (target - current) * 0.2f;
        toggleAnimations.put(id, current);

        var drawList = ImGui.getWindowDrawList();
        float pX = ImGui.getCursorScreenPosX();
        float pY = ImGui.getCursorScreenPosY();

        float r = (0.52f * current + 0.20f * (1 - current));
        float g = (0.34f * current + 0.20f * (1 - current));
        float b = (0.70f * current + 0.20f * (1 - current));
        int bgColor = ImGui.getColorU32(r, g, b, 1.0f);

        drawList.addRectFilled(pX, pY, pX + width, pY + height, bgColor, height / 2.0f);

        float padding = 2.0f;
        float knobSize = height - padding * 2;
        float knobX = pX + padding + (width - height) * current;
        float knobY = pY + padding;
        
        drawList.addCircleFilled(knobX + knobSize / 2, knobY + knobSize / 2, knobSize / 2, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f));

        ImGui.invisibleButton("##switch_btn", width, height);
        return ImGui.isItemClicked();
    }
}
