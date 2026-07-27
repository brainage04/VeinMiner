package io.github.brainage04.vein_miner;

import io.github.brainage04.brainagelib.help.ServerModHelpEntry;
import io.github.brainage04.brainagelib.help.ServerModHelpRegistry;
import io.github.brainage04.vein_miner.command.VeinMinerCommand;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import io.github.brainage04.vein_miner.leaf.LeafDecayRateHandler;
import io.github.brainage04.vein_miner.platform.ServerPlatform;
import io.github.brainage04.vein_miner.player.VeinMinerPlayerSettings;
import io.github.brainage04.vein_miner.vein.VeinMiningHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VeinMiner {
    public static final String MOD_ID = "vein_miner";
    public static final String MOD_NAME = "Vein Miner";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private VeinMiner() {
    }

    public static void initialize(ServerPlatform platform) {
        VeinMinerConfigManager.initialize(platform.configDirectory());
        platform.registerCommands(VeinMinerCommand::initialize);
        platform.registerBlockBreak(VeinMiningHandler::beforeBlockBreak);
        platform.registerServerTick(VeinMinerCommand::tick);
        platform.registerServerTick(LeafDecayRateHandler::tick);
        platform.registerServerStarted(VeinMinerPlayerSettings::load);
        platform.registerServerStopping(VeinMinerPlayerSettings::shutdown);
        platform.registerServerStopping(LeafDecayRateHandler::shutdown);
        ServerModHelpRegistry.register(new ServerModHelpEntry(
                MOD_ID, MOD_NAME,
                "Mines connected ore and tree blocks using per-player activation and selection controls.",
                "/veinminer", "/veinminer admin"
        ));
        LOGGER.info("{} initialised.", MOD_NAME);
    }
}
