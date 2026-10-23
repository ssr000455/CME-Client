package com.chengmo.cmeclient.mixin;

import com.chengmo.cmeclient.gui.ClickGUI;
import com.chengmo.cmeclient.gui.ItemEditorScreen;
import com.chengmo.cmeclient.gui.ImGuiManager;
import com.chengmo.cmeclient.util.InputQueue;
import imgui.ImGui;
import imgui.ImGuiIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (!ImGuiManager.isInitialized()) return;
        ImGuiIO io = ImGui.getIO();

        if (button >= 0 && button <= 2) {
            boolean down = action != 0; // GLFW_PRESS / GLFW_REPEAT
            // 界面关闭时不注入按下，只放行抬起，避免状态卡死。
            // 允许 ItemEditorScreen 接管鼠标事件
            if (ClickGUI.isOpen || ItemEditorScreen.isOpen || !down) {
                InputQueue.pushMouseButton(button, down,
                        io.getMousePosX(), io.getMousePosY(), down);
            }
        }

        if ((ClickGUI.isOpen || ItemEditorScreen.isOpen) && io.getWantCaptureMouse()) {
            // 拦截底层点击，防止穿透到游戏
            ci.cancel();
        }
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        if (!ImGuiManager.isInitialized() || (!ClickGUI.isOpen && !ItemEditorScreen.isOpen)) return;
        ImGuiIO io = ImGui.getIO();
        MinecraftClient mc = MinecraftClient.getInstance();
        
        if (!mc.mouse.isCursorLocked()) {
            double scaleX = (double) mc.getWindow().getFramebufferWidth() / mc.getWindow().getWidth();
            double scaleY = (double) mc.getWindow().getFramebufferHeight() / mc.getWindow().getHeight();
            io.setMousePos((float)(x * scaleX), (float)(y * scaleY));
        }
        
        if (io.getWantCaptureMouse()) {
            ci.cancel();
        }
    }
}
