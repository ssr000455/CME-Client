package com.chengmo.cmeclient.mixin;

import com.chengmo.cmeclient.gui.ClickGUI;
import com.chengmo.cmeclient.gui.ImGuiManager;
import imgui.ImGui;
import imgui.ImGuiIO;
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
        if (ClickGUI.isOpen) {
            io.addMouseButtonEvent(button, action == 1);
            if (io.getWantCaptureMouse()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
    private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        if (!ImGuiManager.isInitialized()) return;
        
        ImGuiIO io = ImGui.getIO();
        if (ClickGUI.isOpen) {
            io.addMousePosEvent((float)x, (float)y);
            if (io.getWantCaptureMouse()) {
                ci.cancel();
            }
        }
    }
}
