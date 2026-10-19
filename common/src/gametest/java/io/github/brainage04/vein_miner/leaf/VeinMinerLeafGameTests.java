package io.github.brainage04.vein_miner.leaf;

import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gamerules.GameRules;

public final class VeinMinerLeafGameTests {
    private static final BlockPos LEAF = new BlockPos(1, 2, 1);

    private VeinMinerLeafGameTests() {}

    public static void queuedDecayHonoursDisableAndShutdown(GameTestHelper helper) {
        try (Scenario scenario = new Scenario(helper)) {
            scenario.queue();
            scenario.config.fastLeafDecayEnabled = false;
            scenario.drain();
            helper.assertBlockPresent(Blocks.OAK_LEAVES, LEAF);
            scenario.config.fastLeafDecayEnabled = true;
            scenario.drain();
            helper.assertBlockPresent(Blocks.OAK_LEAVES, LEAF);

            scenario.queue();
            scenario.config.leafDecaySpeedMultiplier = 1;
            scenario.drain();
            helper.assertBlockPresent(Blocks.OAK_LEAVES, LEAF);
            scenario.config.leafDecaySpeedMultiplier = 1000;
            scenario.queue();
            LeafDecayRateHandler.shutdown(scenario.server);
            scenario.drain();
            helper.assertBlockPresent(Blocks.OAK_LEAVES, LEAF);
        }
        helper.succeed();
    }

    public static void queuedDecayHonoursRandomTickRuleAndLeafState(GameTestHelper helper) {
        try (Scenario scenario = new Scenario(helper)) {
            scenario.queue();
            scenario.level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, 0, scenario.server);
            scenario.drain();
            helper.assertBlockPresent(Blocks.OAK_LEAVES, LEAF);

            scenario.level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, 4096, scenario.server);
            scenario.queue();
            scenario.level.setBlock(helper.absolutePos(LEAF), scenario.decaying.setValue(BlockStateProperties.PERSISTENT, true), 2);
            scenario.drain();
            helper.assertBlockPresent(Blocks.OAK_LEAVES, LEAF);

            scenario.queue();
            scenario.drain();
            helper.assertBlockNotPresent(Blocks.OAK_LEAVES, LEAF);
        }
        helper.succeed();
    }

    private static final class Scenario implements AutoCloseable {
        private final GameTestHelper helper;
        private final ServerLevel level;
        private final MinecraftServer server;
        private final VeinMinerConfig config;
        private final boolean enabled;
        private final int multiplier;
        private final int randomTickSpeed;
        private final BlockState decaying = Blocks.OAK_LEAVES.defaultBlockState()
                .setValue(BlockStateProperties.PERSISTENT, false)
                .setValue(BlockStateProperties.DISTANCE, LeavesBlock.DECAY_DISTANCE);

        private Scenario(GameTestHelper helper) {
            this.helper = helper;
            level = helper.getLevel();
            server = level.getServer();
            config = VeinMinerConfigManager.getConfig();
            enabled = config.fastLeafDecayEnabled;
            multiplier = config.leafDecaySpeedMultiplier;
            randomTickSpeed = level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
            LeafDecayRateHandler.shutdown(server);
            config.fastLeafDecayEnabled = true;
            config.leafDecaySpeedMultiplier = 1000;
            level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, 4096, server);
        }

        private void queue() {
            level.setBlock(helper.absolutePos(LEAF), decaying, 2);
            LeafDecayRateHandler.queueIfDecaying(level, helper.absolutePos(LEAF), decaying);
        }

        private void drain() {
            // The selected multiplier and rule guarantee a one-tick delay, without waiting or RNG.
            LeafDecayRateHandler.tick(server, server.getTickCount() + 1);
        }

        @Override
        public void close() {
            LeafDecayRateHandler.shutdown(server);
            config.fastLeafDecayEnabled = enabled;
            config.leafDecaySpeedMultiplier = multiplier;
            level.getGameRules().set(GameRules.RANDOM_TICK_SPEED, randomTickSpeed, server);
        }
    }
}
