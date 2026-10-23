package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;

public class NoFall extends Module {
    public NoFall() { super("NoFall", Category.MOVEMENT); }
    // 逻辑在 PlayerMoveC2SPacketMixin 中发包拦截
}
