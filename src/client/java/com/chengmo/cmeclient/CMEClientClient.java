package com.chengmo.cmeclient;

import com.chengmo.cmeclient.module.ModuleManager;
import com.chengmo.cmeclient.module.impl.GodMode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CMEClientClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("CME Client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("CME Client Initialized!");
        ModuleManager.INSTANCE.addModule(new GodMode());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ModuleManager.INSTANCE.onTick();
        });
    }
}
