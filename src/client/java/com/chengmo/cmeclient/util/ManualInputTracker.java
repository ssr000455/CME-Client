package com.chengmo.cmeclient.util;

/**
 * 记录最近手动鼠标/触屏输入强度，供 AimAssist 让路。
 * 只记 cursor 像素 delta，不做角度换算（不需要精确）。
 */
public final class ManualInputTracker {
    private static volatile long lastNanos = 0L;
    private static volatile float accumPixels = 0f;
    private static volatile float frameDelta = 0f;

    private static final long RESET_GAP_NS = 250_000_000L; // 250ms 无输入视为一段结束

    private ManualInputTracker() {}

    public static void recordCursor(double dx, double dy) {
        long now = System.nanoTime();
        if (now - lastNanos > RESET_GAP_NS) accumPixels = 0f;
        float mag = (float) Math.sqrt(dx * dx + dy * dy);
        accumPixels += mag;
        frameDelta = mag;
        lastNanos = now;
    }

    public static long lastNanos() { return lastNanos; }
    public static float accum() { return accumPixels; }
    public static float frameDelta() { return frameDelta; }

    public static void reset() {
        accumPixels = 0f;
        frameDelta = 0f;
        lastNanos = 0L;
    }
}
