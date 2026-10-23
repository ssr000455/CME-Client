package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.setting.BoolSetting;
import com.chengmo.cmeclient.module.setting.EnumSetting;
import com.chengmo.cmeclient.module.setting.FloatSetting;
import com.mojang.blaze3d.systems.RenderSystem;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class MiniMap extends Module {
    public final FloatSetting size = addSetting(new FloatSetting("minimap.size", 150.0f, 100.0f, 300.0f));
    public final FloatSetting range = addSetting(new FloatSetting("minimap.range", 64.0f, 32.0f, 128.0f));
    public final EnumSetting filter = addSetting(new EnumSetting("minimap.filter", 0, "All", "Hostile", "Neutral", "Passive"));
    public final BoolSetting rotate = addSetting(new BoolSetting("minimap.rotate", true));
    public final BoolSetting darkMode = addSetting(new BoolSetting("minimap.darkMode", true));
    public final BoolSetting bg = addSetting(new BoolSetting("minimap.bg", true));

    private static final int MAP_SIZE = 256; // 提高分辨率，贴图更清晰
    private static final Identifier MAP_TEXTURE_ID = new Identifier("cmeclient", "minimap_hd_texture");

    private NativeImage mapImage;
    private NativeImageBackedTexture mapTexture;
    private int lastCenterX = Integer.MIN_VALUE;
    private int lastCenterZ = Integer.MIN_VALUE;

    private static class MapEntity {
        float dx, dz, yaw;
        int type;
    }
    private final List<MapEntity> entities = new ArrayList<>();
    private long lastUpdate = 0;

    public MiniMap() {
        super("MiniMap", Category.RENDER);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // 50ms 刷新一次，配合玩家移动
        if (System.currentTimeMillis() - lastUpdate < 50) return;
        lastUpdate = System.currentTimeMillis();

        updateEntities(mc);
        
        // 只有跨区块时才重新生成地形（套用成熟 Mod 的按需更新逻辑）
        BlockPos pos = mc.player.getBlockPos();
        if (pos.getX() != lastCenterX || pos.getZ() != lastCenterZ) {
            lastCenterX = pos.getX();
            lastCenterZ = pos.getZ();
            updateTerrainTexture(mc);
        }
    }

    private void updateTerrainTexture(MinecraftClient mc) {
        if (mapImage == null) mapImage = new NativeImage(MAP_SIZE, MAP_SIZE, false);
        
        BlockPos center = mc.player.getBlockPos();
        float r = range.get();
        float scale = (r * 2) / MAP_SIZE;

        for (int px = 0; px < MAP_SIZE; px++) {
            for (int pz = 0; pz < MAP_SIZE; pz++) {
                int wx = center.getX() + (int) ((px * scale) - r);
                int wz = center.getZ() + (int) ((pz * scale) - r);

                // 简易地表搜索（最多向下探 10 格）
                BlockPos.Mutable pos = new BlockPos.Mutable(wx, center.getY(), wz);
                for (int i = 0; i < 10; i++) {
                    if (!mc.world.getBlockState(pos).isAir()) break;
                    pos.setY(pos.getY() - 1);
                }

                BlockState state = mc.world.getBlockState(pos);
                MapColor mapColor = state.getMapColor(mc.world, pos);
                
                int argb = 0xFF101010;
                if (mapColor != null) {
                    int rgb = mapColor.getRenderColor(MapColor.Brightness.NORMAL);
                    int rC = (rgb >> 16) & 0xFF;
                    int gC = (rgb >> 8) & 0xFF;
                    int bC = rgb & 0xFF;
                    argb = (0xFF << 24) | (rC << 16) | (gC << 8) | bC;
                }
                mapImage.setColor(px, pz, argb);
            }
        }

        if (mapTexture == null) {
            mapTexture = new NativeImageBackedTexture(mapImage);
            mc.getTextureManager().registerTexture(MAP_TEXTURE_ID, mapTexture);
        } else {
            mapTexture.upload();
        }
    }

    private void updateEntities(MinecraftClient mc) {
        entities.clear();
        float r = range.get();
        int mode = filter.get();

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity)) continue;
            double dx = e.getX() - mc.player.getX();
            double dz = e.getZ() - mc.player.getZ();
            if (dx * dx + dz * dz > r * r) continue;

            boolean isHostile = e instanceof HostileEntity;
            boolean isPassive = e instanceof AnimalEntity;
            boolean isNeutral = !isHostile && !isPassive && !(e instanceof PlayerEntity);

            if (mode == 1 && !isHostile) continue;
            if (mode == 2 && !isNeutral) continue;
            if (mode == 3 && !isPassive) continue;

            MapEntity me = new MapEntity();
            me.dx = (float) dx;
            me.dz = (float) dz;
            me.yaw = e.getYaw();
            if (e instanceof PlayerEntity) me.type = 3;
            else if (isHostile) me.type = 0;
            else if (isNeutral) me.type = 1;
            else me.type = 2;
            entities.add(me);
        }
    }

    public void render(ImDrawList dl, ImFont font, float centerX, float centerY) {
        if (!isEnabled() || mapTexture == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        float s = size.get();
        float r = range.get();
        float half = s / 2f;
        float x = centerX - half;
        float y = centerY - half;

        if (bg.get()) {
            dl.addRectFilled(x, y, x + s, y + s, ImGui.getColorU32(0.1f, 0.1f, 0.1f, 0.9f), 8f);
        }
        int texId = mapTexture.getGlId();

        int tint = ImGui.getColorU32(1f, 1f, 1f, 1f);
        if (darkMode.get() && mc.world != null) {
            boolean underground = mc.player != null && mc.player.getY() < 50;
            long timeOfDay = mc.world.getTimeOfDay() % 24000L;
            boolean night = timeOfDay >= 13000L && timeOfDay <= 23000L;
            if (underground || night) {
                tint = ImGui.getColorU32(0.35f, 0.35f, 0.45f, 1f);
            }
        }
        boolean rot = rotate.get();
        float yaw = mc.player.getYaw();

        if (rot) {
            // 旋转映射：玩家朝向永远朝上
            float rad = (float) Math.toRadians(-yaw);
            float cos = MathHelper.cos(rad);
            float sin = MathHelper.sin(rad);

            float x0 = centerX + (-half * cos - -half * sin);
            float y0 = centerY + (-half * sin + -half * cos);
            float x1 = centerX + (half * cos - -half * sin);
            float y1 = centerY + (half * sin + -half * cos);
            float x2 = centerX + (half * cos - half * sin);
            float y2 = centerY + (half * sin + half * cos);
            float x3 = centerX + (-half * cos - half * sin);
            float y3 = centerY + (-half * sin + half * cos);

            dl.addImageQuad(texId, x0, y0, x1, y1, x2, y2, x3, y3, 0, 0, 1, 0, 1, 1, 0, 1, tint);
        } else {
            dl.addImage(texId, x, y, x + s, y + s, 0, 0, 1, 1, tint);
        }

        // 边缘暗角裁剪，防止实体画出界
        dl.pushClipRect(x, y, x + s, y + s, true);

        // 实体渲染（真实图标投影）
        float scale = half / r;
        for (MapEntity e : entities) {
            float ex, ey;
            if (rot) {
                float rad = (float) Math.toRadians(-yaw);
                float cos = MathHelper.cos(rad);
                float sin = MathHelper.sin(rad);
                ex = centerX + (e.dx * cos - e.dz * sin) * scale;
                ey = centerY + (e.dx * sin + e.dz * cos) * scale;
            } else {
                ex = centerX + e.dx * scale;
                ey = centerY + e.dz * scale;
            }

            int color = switch (e.type) {
                case 0 -> ImGui.getColorU32(1f, 0.2f, 0.2f, 1f); // 敌对
                case 1 -> ImGui.getColorU32(1f, 0.8f, 0.2f, 1f); // 中立
                case 2 -> ImGui.getColorU32(0.2f, 1f, 0.3f, 1f); // 友好
                default -> ImGui.getColorU32(1f, 1f, 1f, 1f);    // 玩家
            };

            // 用带有方向的图标代替纯圆点
            if (e.type == 3) {
                dl.addCircleFilled(ex, ey, 3f, color);
                dl.addCircle(ex, ey, 5f, ImGui.getColorU32(0f, 0f, 0f, 0.6f), 12, 1f);
            } else if (e.type == 0) {
                dl.addNgonFilled(ex, ey, 4f, color, 3); // 敌对画三角
            } else {
                dl.addCircleFilled(ex, ey, 3f, color); // 其他画圆
            }
        }

        // 中心十字准星（仅旋转模式）
        if (rot) {
            dl.addLine(centerX - 6, centerY, centerX + 6, centerY, ImGui.getColorU32(0.3f, 0.7f, 1f, 1f), 1.5f);
            dl.addLine(centerX, centerY - 6, centerX, centerY + 6, ImGui.getColorU32(0.3f, 0.7f, 1f, 1f), 1.5f);
        } else {
            float ex = centerX - MathHelper.sin(yaw * 0.017453292f) * 8f;
            float ey = centerY + MathHelper.cos(yaw * 0.017453292f) * 8f;
            dl.addCircleFilled(centerX, centerY, 4f, ImGui.getColorU32(0.3f, 0.7f, 1f, 1f));
            dl.addLine(centerX, centerY, ex, ey, ImGui.getColorU32(0.3f, 0.7f, 1f, 1f), 2f);
        }

        dl.popClipRect();
        dl.addRect(x, y, x + s, y + s, ImGui.getColorU32(0.85f, 0.65f, 0.13f, 0.8f), 8f, 0, 1.5f);
    }
}
