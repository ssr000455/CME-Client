package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.setting.BoolSetting;
import net.minecraft.client.MinecraftClient;

/**
 * 仅客户端强制疾跑：每 tick 持续 setSprinting(true)。
 * 不依赖原版 forwardSpeed 判断，站着也会保持疾跑状态（但没速度就没意义，仅保证状态位）。
 * keep_on_sneak 开启时，潜行也不解除疾跑。
 */
public class Sprint extends Module {
    public final BoolSetting keepOnSneak = addSetting(new BoolSetting("sprint.keep_on_sneak", false));

    public Sprint() {
        super("Sprint", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.player.horizontalCollision) return;
        if (mc.player.isUsingItem()) return;
        if (mc.player.isSneaking() && !keepOnSneak.get()) return;
        mc.player.setSprinting(true);
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.setSprinting(false);
    }
}
