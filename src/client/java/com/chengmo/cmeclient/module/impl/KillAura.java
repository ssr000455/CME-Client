package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.gui.ClickGUI;
import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.setting.BoolSetting;
import com.chengmo.cmeclient.module.setting.FloatSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.village.VillagerDataContainer;
import net.minecraft.util.math.MathHelper;

public class KillAura extends Module {
    public final FloatSetting range = addSetting(new FloatSetting("killaura.range", 3.0f, 2.0f, 4.5f));
    public final FloatSetting cps = addSetting(new FloatSetting("killaura.cps", 8.0f, 1.0f, 20.0f));
    public final FloatSetting targetHold = addSetting(new FloatSetting("killaura.target_hold", 1.5f, 0.0f, 5.0f));
    public final BoolSetting players = addSetting(new BoolSetting("killaura.players", true));
    public final BoolSetting mobs = addSetting(new BoolSetting("killaura.mobs", true));
    public final BoolSetting animals = addSetting(new BoolSetting("killaura.animals", false));
    public final BoolSetting ignoreFriends = addSetting(new BoolSetting("killaura.ignore_friends", true));
    public final BoolSetting rotate = addSetting(new BoolSetting("killaura.rotate", true));
    public final BoolSetting requireSwing = addSetting(new BoolSetting("killaura.require_swing", false));
    public final BoolSetting attackWhileUsing = addSetting(new BoolSetting("killaura.attack_while_using", true));

    private long lastAttack = 0L;
    private int currentTargetId = -1;
    private long lastTargetSwitch = 0L;

    public KillAura() {
        super("KillAura", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        // 与 AimAssist 互斥：开启杀戮光环时自动关闭瞄准辅助
        com.chengmo.cmeclient.module.Module aimAssist =
                com.chengmo.cmeclient.module.ModuleManager.INSTANCE.findById("AimAssist");
        if (aimAssist != null && aimAssist.isEnabled()) {
            aimAssist.setEnabled(false);
            com.chengmo.cmeclient.gui.Notification.show(
                    com.chengmo.cmeclient.util.Lang.get("cmeclient.notification.killaura_disabled_aimassist"));
        }
    }

    @Override
    public void onDisable() {
        lastAttack = 0L;
        currentTargetId = -1;
        lastTargetSwitch = 0L;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (ClickGUI.isOpen) return;
        if (mc.currentScreen != null) return;
        if (!mc.player.isAlive()) return;
        if (requireSwing.get() && !mc.options.attackKey.isPressed()) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.92f) return;

        long now = System.currentTimeMillis();
        long interval = (long) (1000.0 / Math.max(1f, cps.get()));
        if (now - lastAttack < interval) return;

        Entity target = findTarget(mc);
        if (target == null) return;

        if (mc.player.isUsingItem()) {
            if (!attackWhileUsing.get()) return;
            mc.interactionManager.stopUsingItem(mc.player);
        }

        if (rotate.get()) aimAt(mc, target);

        mc.interactionManager.attackEntity(mc.player, target);
        lastAttack = now;

        // 攻击后让目标保持一会儿，避免半帧内换敌人
        currentTargetId = target.getId();
        if (now - lastTargetSwitch >= (long) (targetHold.get() * 1000f)) {
            lastTargetSwitch = now;
        }
    }

    private Entity findTarget(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        long holdMs = (long) (targetHold.get() * 1000f);

        // 保持当前目标（如果仍然有效且还在保持窗口内）
        if (currentTargetId != -1 && now - lastTargetSwitch < holdMs) {
            Entity cur = mc.world.getEntityById(currentTargetId);
            if (cur != null && isValid(cur, mc)) return cur;
        }

        // 重新选最近的合法目标
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!isValid(e, mc)) continue;
            double d = mc.player.squaredDistanceTo(e);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }

        if (best != null && best.getId() != currentTargetId) {
            currentTargetId = best.getId();
            lastTargetSwitch = now;
        }
        return best;
    }

    private boolean isValid(Entity e, MinecraftClient mc) {
        if (e == mc.player) return false;
        if (!e.isAlive()) return false;
        if (e == mc.player.getVehicle()) return false;
        if (!(e instanceof LivingEntity)) return false;

        double maxDistSq = range.get() * range.get();
        if (mc.player.squaredDistanceTo(e) > maxDistSq) return false;

        if (e instanceof PlayerEntity) return players.get();
        if (e instanceof AnimalEntity) return animals.get();

        if (ignoreFriends.get() && isFriendly(e)) return false;

        return e instanceof MobEntity || e instanceof LivingEntity;
    }

    private boolean isFriendly(Entity e) {
        if (e instanceof IronGolemEntity) return true;
        if (e instanceof SnowGolemEntity) return true;
        if (e instanceof WanderingTraderEntity) return true;
        if (e instanceof VillagerDataContainer) return true; // 村民
        return false;
    }

    private void aimAt(MinecraftClient mc, Entity target) {
        double dx = target.getX() - mc.player.getX();
        double dy = target.getEyeY() - mc.player.getEyeY();
        double dz = target.getZ() - mc.player.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));
        mc.player.setYaw(yaw);
        mc.player.setPitch(MathHelper.clamp(pitch, -89f, 89f));
    }
}
