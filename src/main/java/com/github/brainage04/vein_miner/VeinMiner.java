package com.github.brainage04.vein_miner;

import com.github.brainage04.vein_miner.command.core.ModCommands;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VeinMiner implements ModInitializer {
    public static final String MOD_ID = "vein_miner";
    public static final String MOD_NAME = "Vein Miner";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	@Override
	public void onInitialize() {
        LOGGER.info("{} initialising...", MOD_NAME);

        ModCommands.initialize();

        LOGGER.info("{} initialised.", MOD_NAME);
	}
}