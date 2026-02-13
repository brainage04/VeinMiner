package com.example;

import com.example.command.core.ModCommands;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "examplemod";
    public static final String MOD_NAME = "ExampleMod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	@Override
	public void onInitialize() {
        LOGGER.info("{} initialising...", MOD_NAME);

        ModCommands.initialize();

        LOGGER.info("{} initialised.", MOD_NAME);
	}
}