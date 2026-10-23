package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.setting.BoolSetting;
import com.chengmo.cmeclient.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Scaffold C 方案：
 *  - 脚下 Y-1 放方块
 *  - 前进方向预测 1 格放方块
 *  - 站在边缘时强制 sneak（由 ScaffoldMixin 拦截 isSneaking）
 *
 * 放置流程：找方块 → 切槽 → 发包 → 4 tick 节流。
 * 不修改玩家移动速度，全靠原版 sneak 减速。
 */
public class Scaffold extends Module {
    public final FloatSetting delay = addSetting(new FloatSetting("scaffold.delay", 4.0f, 1.0f, 10.0f));
    public final BoolSetting forward = addSetting(new BoolSetting("scaffold.forward", true));
    public final BoolSetting autoSneak = addSetting(new BoolSetting("scaffold.auto_sneak", true));
    public final BoolSetting swing = addSetting(new BoolSetting("scaffold.swing", true));

    private long lastPlace = 0L;

    public Scaffold() {
        super("Scaffold", Category.MOVEMENT);
    }

    /** ScaffoldMixin 调用：是否应强制 sneak。 */
    public static boolean shouldForceSneak() {
        var mod = com.chengmo.cmeclient.module.ModuleManager.INSTANCE.findById("Scaffold");
        if (!(mod instanceof Scaffold sc) || !sc.isEnabled() || !sc.autoSneak.get()) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return false;
        if (!mc.player.isOnGround()) return false;
        // 脚下有方块，但预测位置下方是空气 → 边缘
        BlockPos below = mc.player.getBlockPos().down();
        if (!mc.world.getBlockState(below).isAir()) return false;
        return true;
    }

    @Override
    public void onDisable() {
        lastPlace = 0L;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.currentScreen != null) return;
        if (!mc.player.isOnGround()) return;

        long now = System.currentTimeMillis();
        if (now - lastPlace < (long) (delay.get() * 50f)) return;

        BlockPos below = mc.player.getBlockPos().down();
        boolean placed = false;

        // 1. 脚下
        if (mc.world.getBlockState(below).isAir()) {
            placed = tryPlace(mc, below);
        }

        // 2. 前进方向预测 1 格
        if (!placed && forward.get()) {
            Vec3d vel = mc.player.getVelocity();
            double vx = vel.x, vz = vel.z;
            if (vx * vx + vz * vz > 0.003) {
                int dx = (int) Math.signum(vx);
                int dz = (int) Math.signum(vz);
                BlockPos predict = new BlockPos(
                        mc.player.getBlockPos().getX() + dx,
                        mc.player.getBlockPos().getY() - 1,
                        mc.player.getBlockPos().getZ() + dz);
                if (mc.world.getBlockState(predict).isAir()) {
                    placed = tryPlace(mc, predict);
                }
            }
        }

        if (placed) {
            lastPlace = now;
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    private boolean tryPlace(MinecraftClient mc, BlockPos target) {
        int slot = findBlockSlot(mc);
        if (slot < 0) return false;

        if (slot != mc.player.getInventory().selectedSlot) {
            mc.player.getInventory().selectedSlot = slot;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }

        // 选择目标位置相邻的实心面作为点击面
        Direction face = Direction.UP;
        BlockPos neighbor = target.down();
        if (mc.world.getBlockState(neighbor).isAir()) {
            // 下方也是空气，换相邻方向尝试
            BlockPos[] candidates = {target.north(), target.south(), target.east(), target.west()};
            Direction[] faces = {Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST};
            boolean found = false;
            for (int i = 0; i < candidates.length; i++) {
                if (!mc.world.getBlockState(candidates[i]).isAir()) {
                    neighbor = candidates[i];
                    face = faces[i];
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        Vec3d hitVec = Vec3d.ofCenter(neighbor).add(
                face.getOffsetX() * 0.5,
                face.getOffsetY() * 0.5,
                face.getOffsetZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(hitVec, face, neighbor, false);

        int sequence = 0; // 1.20.1 ClientWorld.getPendingUpdateManager() 非 public，Scaffold 场景用 0 足够
        mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hit, sequence));

        // 本地预放，避免连续 tick 重复发包（服务端确认会覆盖）
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        return true;
    }

    private int findBlockSlot(MinecraftClient mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }
}
