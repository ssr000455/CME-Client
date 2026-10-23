package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;
import net.minecraft.client.MinecraftClient;

public class AutoSprint extends Module {
    public AutoSprint() { super("AutoSprint", Category.MOVEMENT); }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.forwardSpeed > 0) {
            if (!mc.player.isSneaking() && !mc.player.horizontalCollision) {
                mc.player.setSprinting(true);
            }
        }
    }
}
