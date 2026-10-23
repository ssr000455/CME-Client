package com.chengmo.cmeclient.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class WorldRendererESP {
    private WorldRendererESP() {}

    // ===== 方块线框 =====
    public static void drawBlocks(MatrixStack matrices, Iterable<BlockBox> boxes) {
        drawBlocks(matrices, boxes, false);
    }

    public static void drawBlocks(MatrixStack matrices, Iterable<BlockBox> boxes, boolean tracers) {
        boolean any = false;
        for (BlockBox ignored : boxes) { any = true; break; }
        if (!any) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(1.5f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        Vec3d eyePos = mc.player != null ? mc.player.getEyePos() : camPos;

        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (BlockBox box : boxes) {
            drawBoxEdges(buffer, matrix, box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ, box.argb);

            if (tracers) {
                float r = ((box.argb >> 16) & 0xFF) / 255f;
                float g = ((box.argb >> 8) & 0xFF) / 255f;
                float b = (box.argb & 0xFF) / 255f;
                float a = ((box.argb >>> 24) & 0xFF) / 255f;
                float cx = (box.minX + box.maxX) * 0.5f;
                float cy = (box.minY + box.maxY) * 0.5f;
                float cz = (box.minZ + box.maxZ) * 0.5f;
                edge(buffer, matrix,
                        (float) eyePos.x, (float) eyePos.y, (float) eyePos.z,
                        cx, cy, cz, r, g, b, a);
            }
        }

        tessellator.draw();
        matrices.pop();

        RenderSystem.lineWidth(1f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // ===== 实体线框 + 追踪线 =====
    public static void drawEntities(MatrixStack matrices, Iterable<EntityBox> boxes) {
        boolean any = false;
        for (EntityBox ignored : boxes) { any = true; break; }
        if (!any) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(1.5f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // 视线中心（屏幕底部）作为追踪线起点
        Vec3d eyePos = mc.player != null ? mc.player.getEyePos() : camPos;
        float startX = (float) eyePos.x;
        float startY = (float) (eyePos.y - 0.3);
        float startZ = (float) eyePos.z;

        for (EntityBox box : boxes) {
            float half = box.width * 0.5f;
            drawBoxEdges(buffer, matrix,
                    box.cx - half, box.cy, box.cz - half,
                    box.cx + half, box.cy + box.height, box.cz + half,
                    box.argb);

            if (box.tracer) {
                // 从玩家眼睛到实体底部中心画追踪线
                edge(buffer, matrix,
                        startX, startY, startZ,
                        box.cx, box.cy + box.height * 0.5f, box.cz,
                        (box.argb >> 16 & 0xFF) / 255f,
                        (box.argb >> 8 & 0xFF) / 255f,
                        (box.argb & 0xFF) / 255f,
                        (box.argb >>> 24 & 0xFF) / 255f);
            }
        }

        tessellator.draw();
        matrices.pop();

        RenderSystem.lineWidth(1f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // ===== 血量条（GL 全实心矩形，不依赖 ImGui）=====
    public static void drawHealthBars(MatrixStack matrices, Iterable<EntityBox> boxes) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        matrices.push();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        for (EntityBox box : boxes) {
            if (!box.health) continue;
            float barW = Math.max(0.08f, box.width * 0.15f);
            float barH = box.height * 0.85f;
            float offsetX = -(box.width * 0.5f) - barW - 0.06f;
            float y0 = box.cy;
            float y1 = box.cy + barH;

            float ratio = Math.max(0f, Math.min(1f, box.healthRatio));
            float filledTop = y1 - (y1 - y0) * ratio;

            // 背景
            addQuad(buffer, matrix,
                    box.cx + offsetX, y0, box.cz - 0.01f,
                    box.cx + offsetX + barW, y0, box.cz - 0.01f,
                    box.cx + offsetX + barW, y1, box.cz - 0.01f,
                    box.cx + offsetX, y1, box.cz - 0.01f,
                    0f, 0f, 0f, 0.6f);

            // 前景（红→绿渐变的单色近似）
            float r = 1f - ratio;
            float g = ratio;
            addQuad(buffer, matrix,
                    box.cx + offsetX, filledTop, box.cz - 0.005f,
                    box.cx + offsetX + barW, filledTop, box.cz - 0.005f,
                    box.cx + offsetX + barW, y1, box.cz - 0.005f,
                    box.cx + offsetX, y1, box.cz - 0.005f,
                    r, g, 0.1f, 0.9f);
        }

        tessellator.draw();
        matrices.pop();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void addQuad(BufferBuilder buffer, Matrix4f matrix,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float x4, float y4, float z4,
                                float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, a).next();
        buffer.vertex(matrix, x4, y4, z4).color(r, g, b, a).next();
    }

    private static void drawBoxEdges(BufferBuilder buffer, Matrix4f matrix,
                                      float x1, float y1, float z1,
                                      float x2, float y2, float z2,
                                      int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float a = ((argb >>> 24) & 0xFF) / 255f;

        edge(buffer, matrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
        edge(buffer, matrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
        edge(buffer, matrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
        edge(buffer, matrix, x1, y1, z2, x1, y1, z1, r, g, b, a);
        edge(buffer, matrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
        edge(buffer, matrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
        edge(buffer, matrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
        edge(buffer, matrix, x1, y2, z2, x1, y2, z1, r, g, b, a);
        edge(buffer, matrix, x1, y1, z1, x1, y2, z1, r, g, b, a);
        edge(buffer, matrix, x2, y1, z1, x2, y2, z1, r, g, b, a);
        edge(buffer, matrix, x2, y1, z2, x2, y2, z2, r, g, b, a);
        edge(buffer, matrix, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void edge(BufferBuilder buffer, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
    }

    // ===== 数据载体 =====
    public static final class BlockBox {
        public final int minX, minY, minZ, maxX, maxY, maxZ;
        public final int argb;
        public BlockBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int argb) {
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
            this.argb = argb;
        }
    }

    public static final class EntityBox {
        public final float cx, cy, cz, width, height;
        public final int argb;
        public final String name;
        public final float healthRatio;
        public final boolean tracer;
        public final boolean health;
        public final boolean nameVisible;
        public EntityBox(float cx, float cy, float cz, float width, float height, int argb,
                         String name, float healthRatio,
                         boolean tracer, boolean health, boolean nameVisible) {
            this.cx = cx; this.cy = cy; this.cz = cz;
            this.width = width; this.height = height;
            this.argb = argb;
            this.name = name;
            this.healthRatio = healthRatio;
            this.tracer = tracer;
            this.health = health;
            this.nameVisible = nameVisible;
        }
    }
}
