package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;
import net.minecraft.client.MinecraftClient;

public class GodMode extends Module {
    public GodMode() {
        super("GodMode", "全方位无敌", Category.MISC);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.setHealth(20.0f);
            mc.player.getHungerManager().setFoodLevel(20);
            mc.player.getHungerManager().setSaturationLevel(20.0f);
            mc.player.getActiveStatusEffects().keySet().removeIf(effect -> !effect.isBeneficial());
        }
    }
}
