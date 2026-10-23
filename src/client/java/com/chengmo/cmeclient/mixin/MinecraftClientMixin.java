package com.chengmo.cmeclient.mixin;

import com.chengmo.cmeclient.gui.BlockESPManager;
import com.chengmo.cmeclient.gui.ClickGUI;
import com.chengmo.cmeclient.gui.EntityESPManager;
import com.chengmo.cmeclient.gui.HUD;
import com.chengmo.cmeclient.gui.ItemEditorScreen;
import com.chengmo.cmeclient.gui.ImGuiManager;
import com.chengmo.cmeclient.gui.Notification;
import com.chengmo.cmeclient.gui.OreESPManager;
import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.BlockESP;
import com.chengmo.cmeclient.module.impl.ESP;
import com.chengmo.cmeclient.module.impl.OreESP;
import com.chengmo.cmeclient.render.EntityNameRenderer;
import com.chengmo.cmeclient.render.WorldRendererESP;
import com.chengmo.cmeclient.util.InputQueue;
import com.mojang.blaze3d.systems.RenderSystem;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MinecraftClientMixin {
    private static ImGuiImplGl3 imGuiGl3 = null;

    @Inject(method = "render", at = @At("TAIL"))
    private void onWorldRender(MatrixStack matrices, float tickDelta, long limitTime,
                               boolean renderBlockOutline, Camera camera,
                               GameRenderer gameRenderer,
                               LightmapTextureManager lightmapTextureManager,
                               Matrix4f projectionMatrix, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // ===== 1. 传统 GL 3D 线框 =====
        OreESP oreESP = (OreESP) ModuleManager.INSTANCE.findById("OreESP");
        if (oreESP != null && oreESP.isEnabled()) {
            OreESPManager.tick();
            WorldRendererESP.drawBlocks(matrices, OreESPManager.getFound());
        }

        BlockESP blockESP = (BlockESP) ModuleManager.INSTANCE.findById("BlockESP");
        if (blockESP != null && blockESP.isEnabled()) {
            BlockESPManager.tick();
            WorldRendererESP.drawBlocks(matrices, BlockESPManager.getFound(), blockESP.tracers.get());
        }

        ESP esp = (ESP) ModuleManager.INSTANCE.findById("ESP");
        if (esp != null && esp.isEnabled()) {
            EntityESPManager.tick();
            WorldRendererESP.drawEntities(matrices, EntityESPManager.getFound());
            WorldRendererESP.drawHealthBars(matrices, EntityESPManager.getFound());
        }

        // ===== 2. ImGui 2D 渲染 =====
        if (imGuiGl3 == null) {
            ImGuiManager.init();
            if (!ImGuiManager.isInitialized()) return;
            imGuiGl3 = new ImGuiImplGl3();
            imGuiGl3.init("#version 150");
        }

        if (mc.currentScreen == null && mc.getWindow() != null) {
            if (ClickGUI.isOpen || ItemEditorScreen.isOpen) { if (mc.mouse.isCursorLocked()) mc.mouse.unlockCursor(); }
            else { if (!mc.mouse.isCursorLocked() && mc.isWindowFocused()) mc.mouse.lockCursor(); }
        }

        int physWidth = mc.getWindow().getFramebufferWidth();
        int physHeight = mc.getWindow().getFramebufferHeight();

        ImGuiIO io = ImGui.getIO();
        io.setDisplaySize(physWidth, physHeight);
        io.setDeltaTime(1.0f / 60.0f);

        InputQueue.pump(io);

        // 关键：渲染前先让 MC 把自身状态与 GL 同步
        RenderSystem.assertOnRenderThread();
        RenderSystem.backupProjectionMatrix();

        ImGui.newFrame();

        ClickGUI.bgTexId = mc.getFramebuffer().getColorAttachment();

        if (!HUD.layoutMode) ClickGUI.render();
        if (!ClickGUI.isOpen || HUD.layoutMode) HUD.render();
        if (ItemEditorScreen.isOpen) ItemEditorScreen.render();
        EntityNameRenderer.render();
        Notification.render(io.getDisplaySizeX(), io.getDisplaySizeY());
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        // 关键：渲染后用 RenderSystem 全量恢复，并还原投影矩阵
        restoreGlState(mc);
        RenderSystem.restoreProjectionMatrix();
    }

    private static void restoreGlState(MinecraftClient mc) {
        // 1. 深度
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);

        // 2. 面剔除
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glFrontFace(GL11.GL_CCW);

        // 3. 混合
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);

        // 4. 裁剪
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // 5. 颜色写入
        GL11.glColorMask(true, true, true, true);

        // 6. 视口恢复为 MC 的 framebuffer 尺寸
        GL11.glViewport(0, 0, mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());

        // 7. 解绑纹理，让 MC 自己重新绑定
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        // 8. 清零 shader program，让 RenderSystem 后续自己 setShader
        GL20.glUseProgram(0);

        // 9. 还原 MC 默认的裁剪与混合状态
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0f);

        // 10. 重新绑定 MC 的主 framebuffer
        mc.getFramebuffer().beginWrite(true);

        // 11. 重新设置 MC 需要的 shader 与纹理
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        RenderSystem.bindTexture(0);
    }
}
