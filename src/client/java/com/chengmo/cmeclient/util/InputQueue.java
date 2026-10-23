package com.chengmo.cmeclient.util;

import imgui.ImGuiIO;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * imgui-java 1.86 还没有 ImGui 1.87 的事件队列（addMouseButtonEvent / addKeyEvent），
 * 而 NewFrame() 只是把「本帧采样到的按下状态」和「上一帧的状态」做差来推导 MouseClicked
 * 和按键按下边沿。
 *
 * 手机端一次点击的 press 和 release 两个回调经常落在同一帧的间隔里（帧耗时越长越容易），
 * 等到 NewFrame 采样时状态早已回到抬起，边沿根本不存在 —— 所有依赖 isMouseClicked /
 * Button / checkbox / slider 的控件全部无响应，而只依赖「按住」状态的拖动却正常。
 *
 * 这里自己做一层缓冲：每帧对每个按键/鼠标键最多回放一次状态变化，保证 press 一定先被
 * NewFrame 看到，release 才会在之后的帧生效。
 */
public final class InputQueue {

    private record MouseEdge(boolean down, float x, float y, boolean hasPos) { }
    private record KeyEdge(boolean down) { }

    private static final Map<Integer, ArrayDeque<MouseEdge>> MOUSE_BUTTONS = new HashMap<>();
    private static final Map<Integer, ArrayDeque<KeyEdge>> KEYS = new HashMap<>();

    private InputQueue() { }

    public static synchronized void pushMouseButton(int button, boolean down, float x, float y, boolean hasPos) {
        MOUSE_BUTTONS.computeIfAbsent(button, k -> new ArrayDeque<>())
                .addLast(new MouseEdge(down, x, y, hasPos));
    }

    public static synchronized void pushKey(int key, boolean down) {
        KEYS.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(new KeyEdge(down));
    }

    /**
     * 必须在 ImGui.newFrame() 之前调用。
     * 每个按键/鼠标键每帧只消费一个事件，多余的留到后续帧。
     */
    public static synchronized void pump(ImGuiIO io) {
        for (Map.Entry<Integer, ArrayDeque<MouseEdge>> entry : MOUSE_BUTTONS.entrySet()) {
            int button = entry.getKey();
            ArrayDeque<MouseEdge> queue = entry.getValue();

            // 丢弃不会改变当前状态的事件（例如连续两次按下）。
            while (!queue.isEmpty() && queue.peekFirst().down() == io.getMouseDown(button)) {
                queue.pollFirst();
            }
            if (queue.isEmpty()) continue;

            MouseEdge edge = queue.pollFirst();
            if (edge.hasPos()) {
                io.setMousePos(edge.x(), edge.y());
            }
            io.setMouseDown(button, edge.down());
        }

        for (Map.Entry<Integer, ArrayDeque<KeyEdge>> entry : KEYS.entrySet()) {
            int key = entry.getKey();
            ArrayDeque<KeyEdge> queue = entry.getValue();

            while (!queue.isEmpty() && queue.peekFirst().down() == io.getKeysDown(key)) {
                queue.pollFirst();
            }
            if (queue.isEmpty()) continue;

            io.setKeysDown(key, queue.pollFirst().down());
        }
    }

    public static synchronized void clear() {
        MOUSE_BUTTONS.clear();
        KEYS.clear();
    }
}
