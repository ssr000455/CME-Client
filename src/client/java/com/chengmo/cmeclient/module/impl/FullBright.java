package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;
import net.minecraft.client.MinecraftClient;

/**
 * 全亮度：直接把伽马值拉到最大，不改任何光照逻辑，也不拦截原版夜视状态。
 * 关掉后恢复玩家原来的伽马值。
 */
public class FullBright extends Module {
    private double oldGamma;

    public FullBright() {
        super("FullBright", Category.RENDER);
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        oldGamma = mc.options.getGamma().getValue();
        mc.options.getGamma().setValue(1.0);
    }

    @Override
    public void onDisable() {
        MinecraftClient.getInstance().options.getGamma().setValue(oldGamma);
    }
}
