package com.chengmo.cmeclient.mixin;

import com.chengmo.cmeclient.gui.ClickGUI;
import com.chengmo.cmeclient.gui.HUD;
import com.chengmo.cmeclient.gui.ItemEditorScreen;
import com.chengmo.cmeclient.gui.ImGuiManager;
import com.chengmo.cmeclient.gui.Notification;
import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.ItemEditor;
import com.chengmo.cmeclient.util.ConfigManager;
import com.chengmo.cmeclient.util.InputQueue;
import com.chengmo.cmeclient.util.Lang;
import imgui.ImGui;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    
    @Inject(method = "onChar", at = @At("HEAD"), cancellable = true)
    private void onChar(long window, int codepoint, int mods, CallbackInfo ci) {
        if (ImGuiManager.isInitialized() && (ClickGUI.isOpen || ItemEditorScreen.isOpen) && ImGui.getIO().getWantTextInput()) {
            ImGui.getIO().addInputCharacter(codepoint);
            ci.cancel();
        }
    }

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int key, int scancode, int action, int mods, CallbackInfo ci) {
        // F10 打开/关闭物品编辑器（仅启用模块时）
        if (key == GLFW.GLFW_KEY_F10 && action == GLFW.GLFW_PRESS) {
            if (ItemEditorScreen.isOpen) {
                ItemEditorScreen.close();
            } else {
                ItemEditor module = (ItemEditor) ModuleManager.INSTANCE.findById("ItemEditor");
                if (module != null && module.isEnabled()) {
                    ClickGUI.isOpen = false;
                    ItemEditorScreen.open();
                }
            }
            ci.cancel();
            return;
        }
        // 编辑器打开时 ESC 关闭
        if (ItemEditorScreen.isOpen) {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                ItemEditorScreen.close();
                ci.cancel();
            }
            return;
        }
        // 界面关闭时，快捷键用于直接切换模块（仅在游戏中且没有原版界面打开时）
        if (!ClickGUI.isOpen) {
            if (action == GLFW.GLFW_PRESS) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.currentScreen == null && mc.world != null && mc.player != null) {
                    List<Module> toggled = ModuleManager.INSTANCE.toggleByKeybind(key);
                    for (Module module : toggled) {
                        ConfigManager.markDirty();
                        Notification.show(module.getName() + " " + Lang.get(module.isEnabled()
                                ? "cmeclient.notification.enabled" : "cmeclient.notification.disabled"));
                    }
                }
            }
            return;
        }

        if (!ImGuiManager.isInitialized()) return;

        // 如果正在等待绑定快捷键
        if (ClickGUI.listeningModule != null && action == GLFW.GLFW_PRESS) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                ClickGUI.listeningModule.keybind = GLFW.GLFW_KEY_UNKNOWN;
            } else {
                ClickGUI.listeningModule.keybind = key;
            }
            ClickGUI.listeningModule = null;
            ConfigManager.markDirty();
            ci.cancel();
            return;
        }

        // ESC: 先退出 HUD 布局模式，再关闭界面
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            if (HUD.layoutMode) {
                HUD.layoutMode = false;
            } else {
                ClickGUI.isOpen = false;
            }
            ConfigManager.markDirty();
            ci.cancel();
            return;
        }

        // 正常输入框拦截（ClickGUI 和 ItemEditorScreen 都生效）
        if (ImGui.getIO().getWantTextInput()) {
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_DELETE) {
                InputQueue.pushKey(key, action != GLFW.GLFW_RELEASE);
                ci.cancel();
            }
        }
    }
}
