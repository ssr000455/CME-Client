package com.chengmo.cmeclient.gui;

import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.ESP;
import com.chengmo.cmeclient.render.WorldRendererESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EntityESPManager {
    private static final List<WorldRendererESP.EntityBox> found = new ArrayList<>();
    private static long lastScan = 0L;

    private EntityESPManager() {}

    public static List<WorldRendererESP.EntityBox> getFound() { return found; }

    public static void tick() {
        ESP mod = (ESP) ModuleManager.INSTANCE.findById("ESP");
        if (mod == null || !mod.isEnabled()) { found.clear(); return; }
        if (System.currentTimeMillis() - lastScan < 100L) return;
        lastScan = System.currentTimeMillis();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) { found.clear(); return; }

        found.clear();
        double rangeSq = mod.range.get() * mod.range.get();
        boolean customMode = !mod.customEntities.isEmpty();

        Map<EntityType<?>, Integer> customLookup = new HashMap<>();
        if (customMode) {
            for (ESP.EntityEntry entry : mod.customEntities) {
                Identifier id = Identifier.tryParse(entry.id);
                if (id == null || !Registries.ENTITY_TYPE.containsId(id)) continue;
                EntityType<?> type = Registries.ENTITY_TYPE.get(id);
                float[] c = entry.color();
                int argb = 0xFF000000
                        | ((int) (c[0] * 255f) << 16)
                        | ((int) (c[1] * 255f) << 8)
                        | (int) (c[2] * 255f);
                customLookup.put(type, argb);
            }
            if (customLookup.isEmpty()) return;
        }

        boolean showTracer = mod.tracers.get();
        boolean showHealth = mod.health.get();
        boolean showNames = mod.names.get();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (entity.isInvisible() && !mod.showInvisible.get()) continue;
            if (mc.player.squaredDistanceTo(entity) > rangeSq) continue;

            int argb;
            if (customMode) {
                Integer c = customLookup.get(entity.getType());
                if (c == null) continue;
                argb = c;
            } else if (entity instanceof PlayerEntity) {
                if (!mod.players.get()) continue;
                argb = 0xFF3CB4FF;
            } else if (entity instanceof AnimalEntity) {
                if (!mod.animals.get()) continue;
                argb = 0xFF4CFF4C;
            } else if (entity instanceof MobEntity) {
                if (!mod.mobs.get()) continue;
                argb = 0xFFFF4C4C;
            } else if (entity instanceof LivingEntity) {
                // 其他 LivingEntity（如 Villager、IronGolem 之外的）默认归入怪物
                if (!mod.mobs.get()) continue;
                argb = 0xFFFF4C4C;
            } else if (entity instanceof ItemEntity) {
                if (!mod.items.get()) continue;
                argb = 0xFFFFE64C;
            } else {
                // 矿车 / 船 / 掉落方块 / 经验球等一律跳过，避免物品开关误伤
                continue;
            }

            float healthRatio = 0f;
            if (entity instanceof LivingEntity living && living.getMaxHealth() > 0f) {
                healthRatio = Math.max(0f, Math.min(1f, living.getHealth() / living.getMaxHealth()));
            }

            String name = entity.getDisplayName().getString();

            found.add(new WorldRendererESP.EntityBox(
                    (float) entity.getX(),
                    (float) entity.getY(),
                    (float) entity.getZ(),
                    entity.getWidth(),
                    entity.getHeight(),
                    argb,
                    name,
                    healthRatio,
                    showTracer,
                    showHealth,
                    showNames));
        }
    }
}
