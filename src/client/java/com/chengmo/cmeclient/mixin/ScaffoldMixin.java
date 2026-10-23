package com.chengmo.cmeclient.mixin;

import com.chengmo.cmeclient.module.impl.Scaffold;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Scaffold 开启且站在边缘时，强制 isSneaking() 返回 true，
 * 让原版移动逻辑自动减速防掉。
 */
@Mixin(ClientPlayerEntity.class)
public class ScaffoldMixin {

    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void cmeclient$scaffoldSneak(CallbackInfoReturnable<Boolean> cir) {
        if (Scaffold.shouldForceSneak()) {
            cir.setReturnValue(true);
        }
    }
}
