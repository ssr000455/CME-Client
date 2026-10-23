package com.chengmo.cmeclient;

import com.chengmo.cmeclient.gui.ClickGUI;
import com.chengmo.cmeclient.gui.ImGuiManager;
import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.AutoSprint;
import com.chengmo.cmeclient.module.impl.AutoTool;
import com.chengmo.cmeclient.module.impl.BlockESP;
import com.chengmo.cmeclient.module.impl.ESP;
import com.chengmo.cmeclient.module.impl.Flight;
import com.chengmo.cmeclient.module.impl.FullBright;
import com.chengmo.cmeclient.module.impl.KillAura;
import com.chengmo.cmeclient.module.impl.MiniMap;
import com.chengmo.cmeclient.module.impl.NoFall;
import com.chengmo.cmeclient.module.impl.AimAssist;
import com.chengmo.cmeclient.module.impl.ItemEditor;
import com.chengmo.cmeclient.module.impl.Criticals;
import com.chengmo.cmeclient.module.impl.Scaffold;
import com.chengmo.cmeclient.module.impl.Sprint;
import com.chengmo.cmeclient.module.impl.Velocity;
import com.chengmo.cmeclient.module.impl.OreESP;
import com.chengmo.cmeclient.util.ConfigManager;
import com.chengmo.cmeclient.util.Lang;
import imgui.ImGui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CMEClientClient implements ClientModInitializer {
    private static boolean hasSyncedWorld = false;
    public static final Logger LOGGER = LoggerFactory.getLogger("CME Client");
    private static boolean lastM = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("CME Client 客户端加载成功！");
        ModuleManager.INSTANCE.addModule(new FullBright());
        ModuleManager.INSTANCE.addModule(new AutoSprint());
        ModuleManager.INSTANCE.addModule(new Sprint());
        ModuleManager.INSTANCE.addModule(new Flight());
        ModuleManager.INSTANCE.addModule(new OreESP());
        ModuleManager.INSTANCE.addModule(new MiniMap());
        ModuleManager.INSTANCE.addModule(new AutoTool());
        ModuleManager.INSTANCE.addModule(new Scaffold());
        ModuleManager.INSTANCE.addModule(new NoFall());
        ModuleManager.INSTANCE.addModule(new ESP());
        ModuleManager.INSTANCE.addModule(new BlockESP());
        ModuleManager.INSTANCE.addModule(new KillAura());
        ModuleManager.INSTANCE.addModule(new Criticals());
        ModuleManager.INSTANCE.addModule(new AimAssist());
        ModuleManager.INSTANCE.addModule(new ItemEditor());
        ModuleManager.INSTANCE.addModule(new Velocity());
        ConfigManager.load();
        Lang.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getWindow() != null) {
                long handle = client.getWindow().getHandle();
                boolean m = InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_M);
                if (m && !lastM && canToggleGui(client)) {
                    ClickGUI.isOpen = !ClickGUI.isOpen;
                    LOGGER.info("[CME] M pressed, isOpen={}", ClickGUI.isOpen);
                }
                lastM = m;
            }
            if (!hasSyncedWorld && client.world != null && client.player != null) {
                hasSyncedWorld = true;
                for (com.chengmo.cmeclient.module.Module m : ModuleManager.INSTANCE.getModules()) {
                    if (m.isEnabled()) {
                        // 重新触发 onEnable，确保 FullBright / Flight 等模块的应用生效
                        m.setEnabled(false);
                        m.setEnabled(true);
                    }
                }
            }
            ModuleManager.INSTANCE.onTick();
            ConfigManager.tick();
        });

        
        // 每帧驱动 AimAssist（在渲染末帧，不影响当前帧相机，只影响下一帧朝向）
        WorldRenderEvents.END.register(ctx -> {
            com.chengmo.cmeclient.module.Module mod = ModuleManager.INSTANCE.findById("AimAssist");
            if (mod instanceof com.chengmo.cmeclient.module.impl.AimAssist aa && aa.isEnabled()) {
                aa.onFrame();
            }
        });

ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());
    }

    private static boolean canToggleGui(MinecraftClient client) {
        if (client.currentScreen != null) return false;
        if (ClickGUI.isOpen && ImGuiManager.isInitialized() && ImGui.getIO().getWantTextInput()) return false;
        return true;
    }
}
