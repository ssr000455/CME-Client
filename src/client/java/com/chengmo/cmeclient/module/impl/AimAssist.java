package com.chengmo.cmeclient.module.impl;

import com.chengmo.cmeclient.module.Module;
import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.setting.BoolSetting;
import com.chengmo.cmeclient.module.setting.EnumSetting;
import com.chengmo.cmeclient.module.setting.FloatSetting;
import com.chengmo.cmeclient.util.ManualInputTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * 瞄准辅助（每帧更新 + 手动让路）。
 * onTick：选目标。
 * onFrame：手动输入时减速 80%；手动角度过大时本帧暂停，手停后自动恢复。
 */
public class AimAssist extends Module {
    public final FloatSetting range = addSetting(new FloatSetting("aimassist.range", 4.5f, 2.0f, 6.0f));
    public final FloatSetting fov = addSetting(new FloatSetting("aimassist.fov", 60.0f, 10.0f, 180.0f));
    public final FloatSetting speed = addSetting(new FloatSetting("aimassist.speed", 400.0f, 50.0f, 1500.0f));
    public final FloatSetting smooth = addSetting(new FloatSetting("aimassist.smooth", 0.65f, 0.0f, 1.0f));
    public final EnumSetting targetPoint = addSetting(new EnumSetting("aimassist.target", 1, "Head", "Body", "Feet"));
    public final EnumSetting priority = addSetting(new EnumSetting("aimassist.priority", 0, "Distance", "Health"));
    public final BoolSetting gcdFix = addSetting(new BoolSetting("aimassist.gcd_fix", true));
    public final BoolSetting visibleOnly = addSetting(new BoolSetting("aimassist.visible_only", true));
    public final BoolSetting onlyOnAttack = addSetting(new BoolSetting("aimassist.only_on_attack", true));
    public final BoolSetting players = addSetting(new BoolSetting("aimassist.players", true));
    public final BoolSetting mobs = addSetting(new BoolSetting("aimassist.mobs", false));

    // 手动让路
    public final BoolSetting manualOverride = addSetting(new BoolSetting("aimassist.manual_override", true));
    public final BoolSetting manualPauseOnLarge = addSetting(new BoolSetting("aimassist.manual_pause_on_large", true));
    public final FloatSetting manualThreshold = addSetting(new FloatSetting("aimassist.manual_threshold", 30f, 5f, 200f));

    private Entity target = null;
    private long lastNanos = 0;

    public AimAssist() {
        super("AimAssist", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        Module killAura = ModuleManager.INSTANCE.findById("KillAura");
        if (killAura != null && killAura.isEnabled()) {
            killAura.setEnabled(false);
            com.chengmo.cmeclient.gui.Notification.show(
                    com.chengmo.cmeclient.util.Lang.get("cmeclient.notification.aimassist_disabled_killaura"));
        }
    }

    @Override
    public void onDisable() {
        target = null;
        lastNanos = 0;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.currentScreen != null
                || com.chengmo.cmeclient.gui.ClickGUI.isOpen) {
            target = null;
            return;
        }
        Module killAura = ModuleManager.INSTANCE.findById("KillAura");
        if (killAura != null && killAura.isEnabled()) {
            target = null;
            return;
        }
        if (onlyOnAttack.get() && !mc.options.attackKey.isPressed()) {
            target = null;
            return;
        }
        target = findTarget(mc);
    }

    public void onFrame() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.currentScreen != null
                || com.chengmo.cmeclient.gui.ClickGUI.isOpen) {
            return;
        }
        if (target == null || !target.isAlive()) return;

        // ===== 手动让路判断 =====
        float speedMul = 1f;
        if (manualOverride.get()) {
            long sinceManual = System.nanoTime() - ManualInputTracker.lastNanos();
            if (sinceManual < 250_000_000L) { // 250ms 内有手动输入
                float accum = ManualInputTracker.accum();
                if (manualPauseOnLarge.get() && accum > manualThreshold.get()) {
                    // 手动角度过大 → 本帧完全暂停，手停后自动恢复
                    return;
                }
                // 否则辅助降速 80%（保留一点跟手）
                speedMul = 0.2f;
            }
        }

        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0.016f : (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;
        dt = MathHelper.clamp(dt, 0.005f, 0.05f);

        Vec3d aimPoint = getAimPoint(target);
        double dx = aimPoint.x - mc.player.getX();
        double dy = aimPoint.y - mc.player.getEyeY();
        double dz = aimPoint.z - mc.player.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();
        float deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float deltaPitch = targetPitch - currentPitch;

        if (Math.abs(deltaYaw) > fov.get() / 2f) {
            target = null;
            return;
        }

        float distAbs = Math.max(Math.abs(deltaYaw), Math.abs(deltaPitch));
        float maxSpeedDeg = speed.get() * speedMul;
        float curveFactor = 5f + smooth.get() * 25f;
        float speedDeg = maxSpeedDeg * (1f - 0.75f * (float) Math.exp(-distAbs / curveFactor));

        float maxStep = speedDeg * dt;
        deltaYaw = MathHelper.clamp(deltaYaw, -maxStep, maxStep);
        deltaPitch = MathHelper.clamp(deltaPitch, -maxStep, maxStep);

        float jitter = 0.92f + 0.16f * (float) Math.random();
        deltaYaw *= jitter;
        deltaPitch *= jitter;

        if (gcdFix.get()) {
            float step = computeGcdStep(mc);
            if (step > 0f) {
                if (Math.abs(deltaYaw) < 3f) deltaYaw = Math.round(deltaYaw / step) * step;
                if (Math.abs(deltaPitch) < 3f) deltaPitch = Math.round(deltaPitch / step) * step;
            }
        }

        if (deltaYaw != 0f) mc.player.setYaw(currentYaw + deltaYaw);
        if (deltaPitch != 0f) mc.player.setPitch(MathHelper.clamp(currentPitch + deltaPitch, -90f, 90f));
    }

    private Entity findTarget(MinecraftClient mc) {
        Entity best = null;
        double bestScore = Double.MAX_VALUE;
        double maxDistSq = range.get() * range.get();
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !e.isAlive()) continue;
            if (mc.player.squaredDistanceTo(e) > maxDistSq) continue;
            if (!(e instanceof LivingEntity)) continue;
            if (e instanceof PlayerEntity && !players.get()) continue;
            if (!(e instanceof PlayerEntity) && !mobs.get()) continue;
            if (visibleOnly.get() && !mc.player.canSee(e)) continue;
            double score;
            if (priority.get() == 0) score = mc.player.squaredDistanceTo(e);
            else {
                LivingEntity le = (LivingEntity) e;
                score = le.getHealth() / le.getMaxHealth();
            }
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    private Vec3d getAimPoint(Entity e) {
        return switch (targetPoint.get()) {
            case 0 -> new Vec3d(e.getX(), e.getEyeY(), e.getZ());
            case 2 -> new Vec3d(e.getX(), e.getY(), e.getZ());
            default -> new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ());
        };
    }

    private float computeGcdStep(MinecraftClient mc) {
        float sensitivity = mc.options.getMouseSensitivity().getValue().floatValue();
        float f = sensitivity * 0.6f + 0.2f;
        float g = f * f * f * 8.0f;
        return g * 0.15f;
    }
}
