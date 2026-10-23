package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.setting.BoolSetting;
import com.chengmo.cmeclient.module.setting.FloatSetting;

public class OreESP extends Module {
    public final FloatSetting range = addSetting(new FloatSetting("oresp.range", 32.0f, 8.0f, 64.0f));
    public final BoolSetting diamond = addSetting(new BoolSetting("oresp.diamond", true));
    public final BoolSetting emerald = addSetting(new BoolSetting("oresp.emerald", true));
    public final BoolSetting gold = addSetting(new BoolSetting("oresp.gold", true));
    public final BoolSetting iron = addSetting(new BoolSetting("oresp.iron", false));
    public final BoolSetting ancientDebris = addSetting(new BoolSetting("oresp.ancient_debris", false));

    public OreESP() {
        super("OreESP", Category.RENDER);
    }
}
