package com.chengmo.cmeclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector4f;

public final class ScreenProjector {
    private static final Vector4f TMP = new Vector4f();
    private static final Quaternionf INV_ROT = new Quaternionf();

    private ScreenProjector() {}

    public static float[] worldToScreen(double wx, double wy, double wz, float fovDeg, int fbW, int fbH) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer == null) return null;
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d camPos = cam.getPos();

        float x = (float) (wx - camPos.x);
        float y = (float) (wy - camPos.y);
        float z = (float) (wz - camPos.z);

        INV_ROT.set(cam.getRotation()).conjugate();
        INV_ROT.transform(TMP.set(x, y, z, 1.0f));
        float vx = TMP.x;
        float vy = TMP.y;
        float vz = TMP.z;

        if (vz >= -0.1f) return null;

        float fovRad = (float) Math.toRadians(fovDeg);
        float f = 1.0f / (float) Math.tan(fovRad / 2.0);
        float aspect = (float) fbW / (float) fbH;
        float ndcX = (f / aspect) * vx / -vz;
        float ndcY = f * vy / -vz;

        float sx = (ndcX + 1f) * 0.5f * fbW;
        float sy = (1f - ndcY) * 0.5f * fbH;
        return new float[] { sx, sy };
    }
}
