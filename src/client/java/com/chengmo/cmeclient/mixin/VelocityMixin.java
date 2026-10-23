package com.chengmo.cmeclient.mixin;

import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.Velocity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class VelocityMixin {

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
    private void onVelocityUpdate(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (packet.getId() != mc.player.getId()) return;

        Velocity mod = (Velocity) ModuleManager.INSTANCE.findById("Velocity");
        if (mod == null || !mod.isEnabled()) return;

        Entity entity = mc.world.getEntityById(packet.getId());
        if (entity == null) return;

        // S2C 包速度单位 1/8000 m/tick
        double vx = packet.getVelocityX() / 8000.0;
        double vy = packet.getVelocityY() / 8000.0;
        double vz = packet.getVelocityZ() / 8000.0;

        float h = mod.horizontal.get();
        float v = mod.vertical.get();
        boolean keepBoost = mod.keepExplosionBoost.get();

        // 普通击退的 Y 分量通常 < 0.3，爆炸/跳击会 ≥ 0.3
        boolean strongUpward = vy > 0.3;
        double newVy = (strongUpward && keepBoost) ? vy : vy * v;

        entity.setVelocity(vx * h, newVy, vz * h);
        ci.cancel();
    }
}
