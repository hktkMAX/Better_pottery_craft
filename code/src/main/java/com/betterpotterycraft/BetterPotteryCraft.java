package com.betterpotterycraft;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BetterPotteryCraft implements ModInitializer {
    public static final String MOD_ID = "better_pottery_craft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PotteryContent.register();
        LOGGER.info("Better Pottery Craft initialized");
    }
}
