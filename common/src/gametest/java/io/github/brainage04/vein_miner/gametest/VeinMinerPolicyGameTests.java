package io.github.brainage04.vein_miner.gametest;

import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.ORIGIN;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.allowBlock;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.configureOreMining;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.createPlayer;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.destroyOrigin;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.resetPlayer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.brainage04.brainagelib.help.ServerModHelpRegistry;
import io.github.brainage04.vein_miner.VeinMiner;
import io.github.brainage04.vein_miner.config.AdjacencyMode;
import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.ConfigSnapshot;
import io.github.brainage04.vein_miner.leaf.LeafDecayRateHandler;
import io.github.brainage04.vein_miner.player.VeinMinerPlayerSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.LevelResource;

/** Server selection, limits, costs and settings storage, registered by both loaders. */
public final class VeinMinerPolicyGameTests {
    private VeinMinerPolicyGameTests() {}

    public static void faceAdjacencyLeavesDiagonalOreIntact(GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(helper, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, 2);
            config.adjacencyMode = AdjacencyMode.FACES;
            BlockPos diagonal = ORIGIN.east().above();
            helper.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            helper.setBlock(diagonal, Blocks.DIAMOND_ORE);

            destroyOrigin(helper, player);

            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            helper.assertBlockPresent(Blocks.DIAMOND_ORE, diagonal);
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        helper.succeed();
    }

    public static void categoryLimitCapsConnectedOreCount(GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(helper, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, 2);
            helper.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            helper.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);
            helper.setBlock(ORIGIN.east(2), Blocks.DIAMOND_ORE);

            destroyOrigin(helper, player);

            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN.east());
            helper.assertBlockPresent(Blocks.DIAMOND_ORE, ORIGIN.east(2));
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        helper.succeed();
    }

    public static void additionalBlocksUseConfiguredCostsAndAwardMiningStats(
            GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(helper, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, 2);
            config.stopBeforeBreakingTool = false;
            config.durabilityCostPerBlock = 3;
            config.exhaustionCostPerBlock = 4.0F;
            helper.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            helper.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);
            int previousStat = player.getStats().getValue(Stats.BLOCK_MINED, Blocks.DIAMOND_ORE);
            float previousSaturation = player.getFoodData().getSaturationLevel();

            destroyOrigin(helper, player);
            player.getFoodData().tick(player);

            int minedBlocks =
                    player.getStats().getValue(Stats.BLOCK_MINED, Blocks.DIAMOND_ORE)
                            - previousStat;
            if (minedBlocks != 2) {
                throw new AssertionError(
                        "Expected two diamond-ore mining statistics, found " + minedBlocks + ".");
            }
            int toolDamage = player.getMainHandItem().getDamageValue();
            if (toolDamage != 4) {
                throw new AssertionError(
                        "Expected one vanilla and three configured durability damage, found "
                                + toolDamage
                                + ".");
            }
            float saturationLoss = previousSaturation - player.getFoodData().getSaturationLevel();
            if (Math.abs(saturationLoss - 1.0F) > 0.0001F) {
                throw new AssertionError(
                        "Expected configured exhaustion to consume one saturation point, found "
                                + saturationLoss
                                + ".");
            }
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        helper.succeed();
    }

    public static void toolProtectionStopsBeforeLastDurabilityPoint(GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(helper, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, 2);
            config.stopBeforeBreakingTool = true;
            config.minimumRemainingDurability = 1;
            config.durabilityCostPerBlock = 1;
            ItemStack tool = player.getMainHandItem();
            tool.setDamageValue(tool.getMaxDamage() - 2);
            helper.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            helper.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);

            destroyOrigin(helper, player);

            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            if (helper.getBlockState(ORIGIN.east()).isAir()) {
                ItemStack currentTool = player.getMainHandItem();
                throw new AssertionError(
                        "Tool protection mined the extra block; captured damage="
                                + tool.getDamageValue()
                                + ", current damage="
                                + currentTool.getDamageValue()
                                + ", max="
                                + currentTool.getMaxDamage()
                                + ", minimum="
                                + config.minimumRemainingDurability
                                + ", cost="
                                + config.durabilityCostPerBlock
                                + ".");
            }
            if (tool.isEmpty() || tool.getMaxDamage() - tool.getDamageValue() != 1) {
                throw new AssertionError(
                        "Expected the pickaxe to survive with exactly one durability point.");
            }
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        helper.succeed();
    }

    public static void betterTreeMiningConnectsStrippedWoodFamily(GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(helper, Items.DIAMOND_AXE);
        try {
            config.enableVeinMining = true;
            config.maxTreeBlocks = 2;
            config.betterTreeVeinMining = true;
            config.whitelist.clear();
            config.deniedBlocks.clear();
            config.deniedTags.clear();
            config.allowedTags.clear();
            config.allowedTags.add(VeinMinerConfig.TREE_CATEGORY_TAG.location().toString());
            helper.setBlock(ORIGIN, Blocks.OAK_LOG);
            helper.setBlock(ORIGIN.above(), Blocks.STRIPPED_OAK_LOG);

            destroyOrigin(helper, player);

            helper.assertBlockNotPresent(Blocks.OAK_LOG, ORIGIN);
            helper.assertBlockNotPresent(Blocks.STRIPPED_OAK_LOG, ORIGIN.above());
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        helper.succeed();
    }

    public static void denyTagOverridesAllowedBlock(GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(helper, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, 2);
            config.deniedTags.add(VeinMinerConfig.ORE_CATEGORY_TAG.location().toString());
            helper.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            helper.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);

            destroyOrigin(helper, player);

            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            helper.assertBlockPresent(Blocks.DIAMOND_ORE, ORIGIN.east());
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        helper.succeed();
    }

    public static void personalWhitelistFurtherNarrowsServerSelection(GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(helper, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, 2);
            config.betterOreVeinMining = true;
            allowBlock(config, Blocks.DEEPSLATE_DIAMOND_ORE);
            VeinMinerPlayerSettings.clearPersonalWhitelist(player);
            if (VeinMinerPlayerSettings.addPersonalBlock(player, Blocks.DIAMOND_ORE)
                    != VeinMinerPlayerSettings.WhitelistAddResult.ADDED) {
                throw new AssertionError("Expected diamond ore to enter the personal whitelist.");
            }
            helper.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            helper.setBlock(ORIGIN.east(), Blocks.DEEPSLATE_DIAMOND_ORE);

            destroyOrigin(helper, player);

            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            helper.assertBlockPresent(Blocks.DEEPSLATE_DIAMOND_ORE, ORIGIN.east());
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        helper.succeed();
    }

    public static void fastLeafDecayHasAnIndependentMasterSwitch(GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        try {
            BlockState decayingLeaves =
                    Blocks.OAK_LEAVES
                            .defaultBlockState()
                            .setValue(BlockStateProperties.PERSISTENT, false)
                            .setValue(BlockStateProperties.DISTANCE, LeavesBlock.DECAY_DISTANCE);
            config.betterTreeVeinMining = false;
            config.fastLeafDecayEnabled = true;
            config.leafDecaySpeedMultiplier = 100;
            if (!LeafDecayRateHandler.shouldCancelVanillaDecay(decayingLeaves)) {
                throw new AssertionError(
                        "Fast leaf decay should remain active independently of tree-family mining.");
            }
            config.fastLeafDecayEnabled = false;
            if (LeafDecayRateHandler.shouldCancelVanillaDecay(decayingLeaves)) {
                throw new AssertionError("Disabling fast leaf decay should restore vanilla decay.");
            }
        } finally {
            snapshot.restore(config);
        }
        helper.succeed();
    }

    public static void playerToggleIsPersistedInWorldStorage(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper, Items.DIAMOND_PICKAXE);
        try {
            if (!VeinMinerPlayerSettings.setEnabled(player, false)) {
                throw new AssertionError("Expected the player toggle to save.");
            }
            Path settingsPath =
                    helper.getLevel()
                            .getServer()
                            .getWorldPath(LevelResource.ROOT)
                            .resolve("vein-miner-players.json");
            JsonObject players =
                    JsonParser.parseString(Files.readString(settingsPath))
                            .getAsJsonObject()
                            .getAsJsonObject("players");
            JsonObject playerSettings = players.getAsJsonObject(player.getUUID().toString());
            if (playerSettings == null || playerSettings.get("enabled").getAsBoolean()) {
                throw new AssertionError("Expected the disabled player toggle in world storage.");
            }
        } catch (IOException exception) {
            throw new AssertionError("Failed to inspect persisted player settings.", exception);
        } finally {
            resetPlayer(player);
        }
        helper.succeed();
    }

    public static void combinedServerHelpIncludesVeinMiner(GameTestHelper helper) {
        boolean registered =
                ServerModHelpRegistry.entries().stream()
                        .anyMatch(
                                entry ->
                                        entry.modId().equals(VeinMiner.MOD_ID)
                                                && entry.helpCommand().equals("/veinminer")
                                                && entry.adminConfigCommand()
                                                        .equals("/veinminer admin"));
        if (!registered) {
            throw new AssertionError("Expected Vein Miner in the shared server help registry.");
        }
        helper.succeed();
    }
}
