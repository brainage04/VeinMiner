package io.github.brainage04.vein_miner.gametest;

import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.ORIGIN;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.allowBlock;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.configureOreMining;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.createPlayer;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.destroyOrigin;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.moveEastBorderTo;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.prepareVeinMiner;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.resetPlayer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.brainage04.brainagelib.gametest.GameTestPlayer;
import io.github.brainage04.brainagelib.help.ServerModHelpRegistry;
import io.github.brainage04.vein_miner.VeinMiner;
import io.github.brainage04.vein_miner.config.AdjacencyMode;
import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.BreakInterference;
import io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.ConfigSnapshot;
import io.github.brainage04.vein_miner.leaf.LeafDecayRateHandler;
import io.github.brainage04.vein_miner.player.VeinMinerPlayerSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

/** Server selection, limits, costs and settings storage, registered by both loaders. */
public final class VeinMinerPolicyGameTests {
    private static final int FAR_CHUNK_OFFSET = 1000;

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

    public static void blocksBeyondTheWorldBorderAreNotMined(GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(helper, Items.DIAMOND_PICKAXE);
        Runnable restoreBorder = () -> {};
        try {
            configureOreMining(config, 4);
            helper.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            helper.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);
            helper.setBlock(ORIGIN.east(2), Blocks.DIAMOND_ORE);
            helper.setBlock(ORIGIN.east(3), Blocks.DIAMOND_ORE);
            restoreBorder = moveEastBorderTo(helper, ORIGIN.east(2));

            destroyOrigin(helper, player);

            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN.east());
            helper.assertBlockPresent(Blocks.DIAMOND_ORE, ORIGIN.east(2));
            helper.assertBlockPresent(Blocks.DIAMOND_ORE, ORIGIN.east(3));
        } finally {
            restoreBorder.run();
            snapshot.restore(config);
            resetPlayer(player);
        }
        helper.succeed();
    }

    public static void borderMovedDuringTheVeinStopsTheVeinThere(
            GameTestHelper helper, BreakInterference interference) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(helper, Items.DIAMOND_PICKAXE);
        Runnable[] restoreBorder = {() -> {}};
        try {
            configureOreMining(config, 3);
            helper.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            helper.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);
            helper.setBlock(ORIGIN.east(2), Blocks.DIAMOND_ORE);
            interference.runBefore(
                    helper.absolutePos(ORIGIN.east()),
                    () -> restoreBorder[0] = moveEastBorderTo(helper, ORIGIN.east(2)));

            destroyOrigin(helper, player);

            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN.east());
            helper.assertBlockPresent(Blocks.DIAMOND_ORE, ORIGIN.east(2));
        } finally {
            interference.stop();
            restoreBorder[0].run();
            snapshot.restore(config);
            resetPlayer(player);
        }
        helper.succeed();
    }

    public static void refusedExtraBlockStaysWhileTheRestIsMined(
            GameTestHelper helper, BreakInterference interference) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        // A joined player: Fabric answers a refused break with a block update to its connection.
        GameTestPlayer joined = GameTestPlayer.mock(helper, GameType.SURVIVAL);
        ServerPlayer player = joined.player();
        prepareVeinMiner(player, new ItemStack(Items.DIAMOND_PICKAXE));
        try {
            configureOreMining(config, 3);
            helper.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            helper.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);
            helper.setBlock(ORIGIN.west(), Blocks.DIAMOND_ORE);
            interference.refuseAt(helper.absolutePos(ORIGIN.east()));

            destroyOrigin(helper, player);

            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            helper.assertBlockPresent(Blocks.DIAMOND_ORE, ORIGIN.east());
            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN.west());
        } finally {
            interference.stop();
            snapshot.restore(config);
            resetPlayer(player);
            joined.close();
        }
        helper.succeed();
    }

    public static void veinSearchLeavesUnloadedChunksUnloaded(GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(helper, Items.DIAMOND_PICKAXE);
        ServerLevel level = helper.getLevel();
        // Far from every test structure. A forced chunk's ticket level grows by one per chunk
        // (ChunkMap.java:126, ChunkTracker.java:64-66), so the chunks one and two east of it are
        // loaded and the third is not (ChunkLevel.java:38-45, ServerChunkCache.java:258-261).
        ChunkPos testChunk = ChunkPos.containing(helper.absolutePos(ORIGIN));
        ChunkPos forced = new ChunkPos(testChunk.x() + FAR_CHUNK_OFFSET, testChunk.z());
        ChunkPos inner = new ChunkPos(forced.x() + 1, forced.z());
        ChunkPos outer = new ChunkPos(forced.x() + 2, forced.z());
        int y = helper.absolutePos(ORIGIN).getY();
        int z = inner.getMiddleBlockZ();
        BlockPos origin = new BlockPos(inner.getMaxBlockX() - 1, y, z);
        BlockPos innerEdge = origin.east();
        BlockPos outerWest = new BlockPos(outer.getMinBlockX(), y, z);
        BlockPos outerEast = new BlockPos(outer.getMaxBlockX(), y, z);
        BlockPos beyond = outerEast.east();
        int quietFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        try {
            level.setChunkForced(forced.x(), forced.z(), true);
            configureOreMining(config, 32);
            for (int x = origin.getX(); x <= outerEast.getX(); x++) {
                level.setBlock(
                        new BlockPos(x, y, z), Blocks.DIAMOND_ORE.defaultBlockState(), quietFlags);
            }
            helper.assertFalse(
                    level.isLoaded(beyond), "The chunk east of the vein must start unloaded.");

            helper.assertTrue(
                    player.gameMode.destroyBlock(origin), "Expected the origin to be destroyed.");

            helper.assertFalse(
                    level.isLoaded(beyond), "The vein loaded the chunk east of the vein.");
            helper.assertTrue(
                    level.getBlockState(innerEdge).isAir(),
                    "Expected the vein to mine a chunk whose neighbours are all loaded.");
            helper.assertTrue(
                    level.getBlockState(outerWest).is(Blocks.DIAMOND_ORE)
                            && level.getBlockState(outerEast).is(Blocks.DIAMOND_ORE),
                    "Expected the vein to stop at a chunk next to an unloaded chunk.");
        } finally {
            for (int x = origin.getX(); x <= outerEast.getX(); x++) {
                level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), quietFlags);
            }
            AABB vein = AABB.encapsulatingFullBlocks(origin, outerEast).inflate(3.0D);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, vein)) {
                item.discard();
            }
            level.setChunkForced(forced.x(), forced.z(), false);
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
            int previousToolUses =
                    player.getStats().getValue(Stats.ITEM_USED, Items.DIAMOND_PICKAXE);
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
            int toolUses =
                    player.getStats().getValue(Stats.ITEM_USED, Items.DIAMOND_PICKAXE)
                            - previousToolUses;
            helper.assertTrue(
                    toolUses == 2, "Expected two diamond-pickaxe uses, found " + toolUses);
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
