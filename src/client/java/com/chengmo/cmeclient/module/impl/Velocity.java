package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.setting.BoolSetting;
import com.chengmo.cmeclient.module.setting.FloatSetting;

/**
 * 防击退：
 *   horizontal       —— X/Z 方向保留比例（0 = 完全免疫横向）
 *   vertical         —— Y 方向普通击退保留比例
 *   keep_explosion_boost —— 开启时，Y 分量 > 0.3 视为爆炸/跳击，保留 100%
 */
public class Velocity extends Module {
    public final FloatSetting horizontal = addSetting(
            new FloatSetting("velocity.horizontal", 0.0f, 0.0f, 1.0f));
    public final FloatSetting vertical = addSetting(
            new FloatSetting("velocity.vertical", 1.0f, 0.0f, 1.0f));
    public final BoolSetting keepExplosionBoost = addSetting(
            new BoolSetting("velocity.keep_explosion_boost", true));

    public Velocity() {
        super("Velocity", Category.COMBAT);
    }
}
