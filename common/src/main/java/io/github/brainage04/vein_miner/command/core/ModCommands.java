package io.github.brainage04.vein_miner.command.core;

import io.github.brainage04.vein_miner.command.VeinMinerCommand;
import io.github.brainage04.vein_miner.platform.ServerPlatform;

public class ModCommands {
    public static void initialize(ServerPlatform platform) {
        platform.registerCommands(VeinMinerCommand::initialize);
    }
}
