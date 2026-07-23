package io.github.brainage04.vein_miner;

import io.github.brainage04.brainagelib.help.ServerModHelpEntry;
import io.github.brainage04.brainagelib.help.ServerModHelpRegistry;
import io.github.brainage04.vein_miner.command.core.ModCommands;
import io.github.brainage04.vein_miner.command.VeinMinerCommand;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import io.github.brainage04.vein_miner.leaf.LeafDecayRateHandler;
import io.github.brainage04.vein_miner.player.VeinMinerPlayerSettings;
import io.github.brainage04.vein_miner.vein.VeinMiningHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VeinMiner implements ModInitializer {
    public static final String MOD_ID = "vein_miner";
    public static final String MOD_NAME = "Vein Miner";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onInitialize() {
        LOGGER.info("{} initialising...", MOD_NAME);

        VeinMinerConfigManager.initialize();
        VeinMinerPlayerSettings.initialize();
        ModCommands.initialize();
        VeinMiningHandler.initialize();
        LeafDecayRateHandler.initialize();
        ServerTickEvents.END_SERVER_TICK.register(VeinMinerCommand::tick);
        ServerModHelpRegistry.register(new ServerModHelpEntry(
                MOD_ID,
                MOD_NAME,
                "Mines connected ore and tree blocks using per-player activation and selection controls.",
                "/veinminer",
                "/veinminer admin"
        ));

        LOGGER.info("{} initialised.", MOD_NAME);
    }
}
