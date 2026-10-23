package com.chengmo.cmeclient.gui;

import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.BlockESP;
import com.chengmo.cmeclient.render.WorldRendererESP;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 扫描周围 BlockESP 配置的方块，输出给传统 GL 渲染器。 */
public final class BlockESPManager {
    private static final List<WorldRendererESP.BlockBox> found = new ArrayList<>();
    private static long lastScan = 0L;

    private BlockESPManager() {}

    public static List<WorldRendererESP.BlockBox> getFound() { return found; }

    public static void tick() {
        BlockESP mod = (BlockESP) ModuleManager.INSTANCE.findById("BlockESP");
        if (mod == null || !mod.isEnabled() || mod.blocks.isEmpty()) {
            found.clear();
            return;
        }
        if (System.currentTimeMillis() - lastScan < 600L) return;
        lastScan = System.currentTimeMillis();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        Map<Block, Integer> lookup = new HashMap<>();
        for (BlockESP.BlockEntry entry : mod.blocks) {
            Identifier id = Identifier.tryParse(entry.id);
            if (id == null || !Registries.BLOCK.containsId(id)) continue;
            Block block = Registries.BLOCK.get(id);
            float[] c = entry.color();
            int argb = 0xFF000000
                    | ((int) (c[0] * 255f) << 16)
                    | ((int) (c[1] * 255f) << 8)
                    | (int) (c[2] * 255f);
            lookup.put(block, argb);
        }
        if (lookup.isEmpty()) { found.clear(); return; }

        found.clear();
        int r = mod.range.get().intValue();
        BlockPos center = mc.player.getBlockPos();
        int minY = Math.max(mc.world.getBottomY(), center.getY() - r);
        int maxY = Math.min(mc.world.getTopY(), center.getY() + r);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = center.getX() - r; x <= center.getX() + r; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
                    pos.set(x, y, z);
                    Integer argb = lookup.get(mc.world.getBlockState(pos).getBlock());
                    if (argb != null) {
                        found.add(new WorldRendererESP.BlockBox(
                                x, y, z,
                                x + 1, y + 1, z + 1,
                                argb));
                    }
                }
            }
        }
    }
}
