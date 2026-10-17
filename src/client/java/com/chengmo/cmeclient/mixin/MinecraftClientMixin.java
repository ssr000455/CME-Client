package com.chengmo.cmeclient.mixin;

import com.chengmo.cmeclient.gui.ClickGUI;
import com.chengmo.cmeclient.gui.ImGuiManager;
import imgui.ImGui;
import imgui.gl3.ImGuiImplGl3;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    private static ImGuiImplGl3 imGuiGl3 = null;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(boolean tick, CallbackInfo ci) {
        if (imGuiGl3 == null) {
            ImGuiManager.init();
            imGuiGl3 = new ImGuiImplGl3();
            imGuiGl3.init("#version 150");
        }
        
        ImGui.newFrame();
        ClickGUI.render();
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }
}
