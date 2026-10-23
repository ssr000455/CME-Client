package com.chengmo.cmeclient.gui;

import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.OreESP;
import com.chengmo.cmeclient.render.WorldRendererESP;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 时间预算式分片扫描：
 *  - 每 tick 用约 6ms 尽量多地扫方块（不是固定 512 块/tick）
 *  - 玩家移动时只跟随范围边界，不中断扫描游标
 *  - 用 HashSet 去重，循环扫描不会重复添加
 *  - 每 500ms 清理一次超出范围的结果
 *
 * range=32 时一轮约 0.5~1.5 秒扫完，玩家基本感觉不到延迟。
 */
public final class OreESPManager {
    private static final List<WorldRendererESP.BlockBox> found = new ArrayList<>();
    private static final Set<Long> foundPositions = new HashSet<>();

    private static final int CHUNK = 16;                   // 每次扫 16x16x16 = 4096 块
    private static final long TIME_BUDGET_NS = 6_000_000L; // 每 tick 6ms 预算
    private static long lastTick = 0;
    private static long lastPrune = 0;

    private static int curX, curY, curZ;
    private static int minX, minY, minZ, maxX, maxY, maxZ;
    private static boolean initialized = false;

    private OreESPManager() {}

    public static List<WorldRendererESP.BlockBox> getFound() { return found; }

    /** 把 (x,y,z) 编码成 long，用于去重。y 范围 0~4095，x/z 范围 ±3355 万，足够。 */
    private static long posKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (y & 0xFFF) << 26)
                | (z & 0x3FFFFFF);
    }

    public static void tick() {
        OreESP mod = (OreESP) ModuleManager.INSTANCE.findById("OreESP");
        if (mod == null || !mod.isEnabled()) {
            found.clear();
            foundPositions.clear();
            initialized = false;
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        if (now - lastTick < 16) return;
        lastTick = now;

        int range = mod.range.get().intValue();
        BlockPos center = mc.player.getBlockPos();

        // 每 tick 都跟随玩家更新边界
        int newMinX = center.getX() - range;
        int newMaxX = center.getX() + range;
        int newMinY = Math.max(mc.world.getBottomY(), center.getY() - range);
        int newMaxY = Math.min(mc.world.getTopY(), center.getY() + range);
        int newMinZ = center.getZ() - range;
        int newMaxZ = center.getZ() + range;

        boolean boundsChanged = !initialized
                || newMinX != minX || newMaxX != maxX
                || newMinY != minY || newMaxY != maxY
                || newMinZ != minZ || newMaxZ != maxZ;

        if (boundsChanged) {
            minX = newMinX; maxX = newMaxX;
            minY = newMinY; maxY = newMaxY;
            minZ = newMinZ; maxZ = newMaxZ;

            if (!initialized) {
                curX = minX; curY = minY; curZ = minZ;
                initialized = true;
            } else {
                // 玩家移动：游标跟着平移，不重置
                if (curX < minX) curX = minX;
                if (curX > maxX) curX = maxX;
                if (curY < minY) curY = minY;
                if (curY > maxY) curY = maxY;
                if (curZ < minZ) curZ = minZ;
                if (curZ > maxZ) curZ = maxZ;
            }
        }

        // 时间预算内尽量多扫
        long deadline = System.nanoTime() + TIME_BUDGET_NS;
        while (System.nanoTime() < deadline) {
            scanChunk(mc, mod);
        }

        // 每 500ms 清理一次超范围结果
        if (now - lastPrune > 500) {
            lastPrune = now;
            pruneOutOfRange(center, range);
        }
    }

    private static void scanChunk(MinecraftClient mc, OreESP mod) {
        int endX = Math.min(curX + CHUNK, maxX + 1);
        int endY = Math.min(curY + CHUNK, maxY + 1);
        int endZ = Math.min(curZ + CHUNK, maxZ + 1);
        BlockPos.Mutable pos = new BlockPos.Mutable();

        boolean diamond = mod.diamond.get();
        boolean emerald = mod.emerald.get();
        boolean gold = mod.gold.get();
        boolean iron = mod.iron.get();
        boolean ancient = mod.ancientDebris.get();

        for (int x = curX; x < endX; x++) {
            for (int y = curY; y < endY; y++) {
                for (int z = curZ; z < endZ; z++) {
                    pos.set(x, y, z);
                    Block b = mc.world.getBlockState(pos).getBlock();
                    int argb = 0;
                    if (diamond && (b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE))
                        argb = 0xFF00FFFF;
                    else if (emerald && (b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE))
                        argb = 0xFF00FF00;
                    else if (gold && (b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE))
                        argb = 0xFFFFD700;
                    else if (iron && (b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE))
                        argb = 0xFFC0C0C0;
                    else if (ancient && b == Blocks.ANCIENT_DEBRIS)
                        argb = 0xFF8B4513;

                    if (argb != 0) {
                        long key = posKey(x, y, z);
                        if (foundPositions.add(key)) {
                            found.add(new WorldRendererESP.BlockBox(
                                    x, y, z, x + 1, y + 1, z + 1, argb));
                        }
                    }
                }
            }
        }

        // 推进游标，越界则循环回起点
        curZ = endZ;
        if (curZ > maxZ) {
            curZ = minZ;
            curY = endY;
            if (curY > maxY) {
                curY = minY;
                curX = endX;
                if (curX > maxX) {
                    curX = minX; // 扫完一圈，重新开始
                }
            }
        }
    }

    private static void pruneOutOfRange(BlockPos center, int range) {
        int maxDistSq = (range + 4) * (range + 4);
        found.removeIf(box -> {
            int dx = box.minX - center.getX();
            int dy = box.minY - center.getY();
            int dz = box.minZ - center.getZ();
            if (dx * dx + dy * dy + dz * dz > maxDistSq) {
                foundPositions.remove(posKey(box.minX, box.minY, box.minZ));
                return true;
            }
            return false;
        });
    }
}
