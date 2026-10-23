package com.chengmo.cmeclient.util;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** 缓存所有已注册实体 ID（含模组实体），供搜索弹窗使用。 */
public final class EntityIds {
    private static List<String> cache;

    private EntityIds() {}

    public static List<String> all() {
        if (cache == null) {
            List<String> list = new ArrayList<>();
            for (EntityType<?> type : Registries.ENTITY_TYPE) {
                Identifier id = Registries.ENTITY_TYPE.getId(type);
                if (id != null) list.add(id.toString());
            }
            list.sort(String::compareTo);
            cache = list;
        }
        return cache;
    }
}
