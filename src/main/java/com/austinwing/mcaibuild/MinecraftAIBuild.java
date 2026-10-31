package com.austinwing.mcaibuild;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MinecraftAIBuild implements ModInitializer {
    public static final String MOD_ID = "minecraft_ai_build";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Minecraft AI Build foundation loaded.");
    }
}
