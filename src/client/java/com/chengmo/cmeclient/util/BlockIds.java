package com.chengmo.cmeclient.util;

import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** 缓存所有已注册方块 ID（含模组方块），供搜索弹窗使用。 */
public final class BlockIds {
    private static List<String> cache;

    private BlockIds() {}

    public static List<String> all() {
        if (cache == null) {
            List<String> list = new ArrayList<>();
            for (Block block : Registries.BLOCK) {
                Identifier id = Registries.BLOCK.getId(block);
                if (id != null) list.add(id.toString());
            }
            list.sort(String::compareTo);
            cache = list;
        }
        return cache;
    }
}
