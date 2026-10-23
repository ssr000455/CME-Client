package com.chengmo.cmeclient.mixin;

import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.Criticals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class CriticalsMixin {

    @Inject(method = "sendPacket", at = @At("HEAD"))
    private void cmeclient$criticals(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof PlayerInteractEntityC2SPacket interact)) return;

        // Access Widener 已开放 typeHandler 字段 + InteractType 枚举 + getType() 方法
        if (interact.type.getType() != PlayerInteractEntityC2SPacket.InteractType.ATTACK) return;

        Criticals mod = (Criticals) ModuleManager.INSTANCE.findById("Criticals");
        if (mod == null || !mod.isEnabled() || !mod.canInject()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // 攻击瞬间停疾跑（防止疾跑攻击覆盖暴击判定）
        if (mod.stopSprint.get() && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }

        // 插入伪造位移包：先微升，后复位，均标记离地
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false));
        mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false));
    }
}
