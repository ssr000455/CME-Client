package com.chengmo.cmeclient.mixin;

import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.NoFall;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.20.1 里所有移动包（PositionAndOnGround / LookAndOnGround / Full / OnGroundOnly）
 * 都在 ClientPlayerEntity.sendMovementPackets() 内部构造。
 * 直接把这里对 isOnGround() 的调用重定向为 true，客户端本地状态保持不变，
 * 发出去的包 onGround 字段全部变成 true —— 服务端认为玩家一直在地面。
 */
@Mixin(ClientPlayerEntity.class)
public class NoFallMixin {

    @Redirect(
            method = "sendMovementPackets",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;isOnGround()Z"
            )
    )
    private boolean cmeclient$forceOnGround(ClientPlayerEntity self) {
        NoFall mod = (NoFall) ModuleManager.INSTANCE.findById("NoFall");
        if (mod != null && mod.isEnabled()) return true;
        return self.isOnGround();
    }
}
