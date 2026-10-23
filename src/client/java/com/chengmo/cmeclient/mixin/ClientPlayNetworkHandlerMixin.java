package com.chengmo.cmeclient.mixin;

import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.NoFall;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @ModifyVariable(method = "sendPacket", at = @At("HEAD"), argsOnly = true)
    private Packet<?> cmeclient$noFall(Packet<?> packet) {
        if (!(packet instanceof PlayerMoveC2SPacket move)) return packet;
        if (move.isOnGround()) return packet;
        // 跳过 Criticals 的伪造包（Y 偏移 ≈ 0.0625）
        if (packet instanceof PlayerMoveC2SPacket.PositionAndOnGround pos) {
            double dy = Math.abs(pos.getY(0.0) - net.minecraft.client.MinecraftClient.getInstance().player.getY());
            if (dy > 0.05 && dy < 0.08) return packet;
        }

        NoFall mod = (NoFall) ModuleManager.INSTANCE.findById("NoFall");
        if (mod == null || !mod.isEnabled()) return packet;

        // 1.20.1 Yarn: getX/getY/getZ 需要 fallback 参数；getYaw/getPitch 同理
        if (packet instanceof PlayerMoveC2SPacket.Full full) {
            return new PlayerMoveC2SPacket.Full(
                    full.getX(0.0), full.getY(0.0), full.getZ(0.0),
                    full.getYaw(0f), full.getPitch(0f),
                    true);
        }
        if (packet instanceof PlayerMoveC2SPacket.PositionAndOnGround pos) {
            return new PlayerMoveC2SPacket.PositionAndOnGround(
                    pos.getX(0.0), pos.getY(0.0), pos.getZ(0.0),
                    true);
        }
        if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround look) {
            return new PlayerMoveC2SPacket.LookAndOnGround(
                    look.getYaw(0f), look.getPitch(0f),
                    true);
        }
        if (packet instanceof PlayerMoveC2SPacket.OnGroundOnly) {
            return new PlayerMoveC2SPacket.OnGroundOnly(true);
        }
        return packet;
    }
}
