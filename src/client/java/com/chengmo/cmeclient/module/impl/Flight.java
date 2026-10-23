package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.setting.EnumSetting;
import com.chengmo.cmeclient.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class Flight extends Module {
    public final EnumSetting mode = addSetting(new EnumSetting("flight.mode", 0, "Vanilla", "Glide"));
    public final FloatSetting speed = addSetting(new FloatSetting("flight.speed", 1.0f, 0.5f, 3.0f));

    public Flight() {
        super("Flight", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.getAbilities().allowFlying = true;
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && !mc.player.isCreative()) {
            mc.player.getAbilities().allowFlying = false;
            mc.player.getAbilities().flying = false;
        }
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (mode.get() == 0) {
            // 原版飞行
            mc.player.getAbilities().allowFlying = true;
            mc.player.getAbilities().setFlySpeed(speed.get() * 0.05f);
            mc.player.getAbilities().flying = true;
            return;
        }

        // Glide 模式：不走原版 abilities.flying，靠手动注入速度
        mc.player.getAbilities().allowFlying = true;
        mc.player.getAbilities().flying = false;

        if (mc.player.isOnGround()) {
            // 站在地上时不做任何处理，允许正常起跳
            return;
        }

        Vec3d vel = mc.player.getVelocity();
        float sp = speed.get();

        double yVel;
        if (mc.options.sneakKey.isPressed()) {
            yVel = -0.5;
        } else if (mc.options.jumpKey.isPressed()) {
            yVel = 0.4;
        } else {
            yVel = -0.05;
        }

        // 水平方向保持动量并按 speed 缩放，让玩家能持续滑翔
        double xVel = vel.x * sp;
        double zVel = vel.z * sp;

        mc.player.setVelocity(xVel, yVel, zVel);
        mc.player.fallDistance = 0f;
        mc.player.setOnGround(false);
    }
}
