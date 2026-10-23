package com.chengmo.cmeclient.mixin;

import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.AutoTool;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class PlayerActionMixin {

    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        switchToBestTool(pos);
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void onUpdateBlockBreakingProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        switchToBestTool(pos);
    }

    private static void switchToBestTool(BlockPos pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        AutoTool mod = (AutoTool) ModuleManager.INSTANCE.findById("AutoTool");
        if (mod == null || !mod.isEnabled()) return;

        // 正在吃东西/喝药/拉弓时不切换，避免打断使用状态
        if (mc.player.isUsingItem()) return;

        var state = mc.world.getBlockState(pos);
        ItemStack current = mc.player.getInventory().getMainHandStack();
        float currentSpeed = current.getMiningSpeedMultiplier(state);

        int bestSlot = mc.player.getInventory().selectedSlot;
        float bestSpeed = currentSpeed;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            float speed = stack.getMiningSpeedMultiplier(state);
            // 严格大于，避免当前槽位和备用槽位速度相同时无意义切换
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        if (bestSlot != mc.player.getInventory().selectedSlot) {
            mc.player.getInventory().selectedSlot = bestSlot;
            // 通知服务端，保证挖速判定一致
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
        }
    }
}
