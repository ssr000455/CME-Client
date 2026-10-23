package com.chengmo.cmeclient.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EventBus {
    public static class TickEvent {}
    public static class Render2DEvent {
        public float tickDelta;
        public Render2DEvent(float tickDelta) { this.tickDelta = tickDelta; }
    }

    private static final List<Consumer<TickEvent>> tickListeners = new ArrayList<>();
    private static final List<Consumer<Render2DEvent>> renderListeners = new ArrayList<>();

    public static void registerTick(Consumer<TickEvent> listener) {
        tickListeners.add(listener);
    }

    public static void registerRender2D(Consumer<Render2DEvent> listener) {
        renderListeners.add(listener);
    }

    public static void postTick() {
        TickEvent event = new TickEvent();
        for (Consumer<TickEvent> l : tickListeners) {
            l.accept(event);
        }
    }

    public static void postRender2D(float tickDelta) {
        Render2DEvent event = new Render2DEvent(tickDelta);
        for (Consumer<Render2DEvent> l : renderListeners) {
            l.accept(event);
        }
    }
}
