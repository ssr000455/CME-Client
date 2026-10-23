package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.setting.BoolSetting;

/**
 * 刀刀暴击 - Packet 模式（参考 Meteor/OyVey）。
 * 攻击包发出时，由 CriticalsMixin 插入伪造位移包欺骗服务端物理判定。
 * stop_sprint: 攻击瞬间自动停疾跑，防止疾跑攻击覆盖暴击。
 */
public class Criticals extends Module {
    public final BoolSetting stopSprint = addSetting(new BoolSetting("criticals.stop_sprint", true));
    public final BoolSetting onlyOnGround = addSetting(new BoolSetting("criticals.only_on_ground", true));

    public Criticals() {
        super("Criticals", Category.COMBAT);
    }

    /** 供 Mixin 调用：判断当前是否允许注入伪造包。 */
    public boolean canInject() {
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return false;
        if (mc.player.isTouchingWater() || mc.player.isInLava()) return false;
        if (mc.player.isClimbing() || mc.player.hasVehicle()) return false;
        if (onlyOnGround.get() && !mc.player.isOnGround()) return false;
        return true;
    }
}
