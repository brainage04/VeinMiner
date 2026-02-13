package com.github.brainage04.vein_miner;

import com.github.brainage04.vein_miner.command.core.ModCommands;
import com.github.brainage04.vein_miner.command.VeinMinerCommand;
import com.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import com.github.brainage04.vein_miner.vein.VeinMiningHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VeinMiner implements ModInitializer {
    public static final String MOD_NAME = "Vein Miner";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	@Override
	public void onInitialize() {
        LOGGER.info("{} initialising...", MOD_NAME);

        VeinMinerConfigManager.initialize();
        ModCommands.initialize();
        VeinMiningHandler.initialize();
        ServerTickEvents.END_SERVER_TICK.register(VeinMinerCommand::tick);

        LOGGER.info("{} initialised.", MOD_NAME);
	}
}
