package io.github.brainage04.vein_miner;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.brainage04.brainagelib.help.ServerModHelpRegistry;
import io.github.brainage04.vein_miner.config.ActivationMode;
import io.github.brainage04.vein_miner.config.AdjacencyMode;
import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import io.github.brainage04.vein_miner.leaf.LeafDecayRateHandler;
import io.github.brainage04.vein_miner.player.VeinMinerPlayerSettings;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.LinkedHashSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VeinMinerPolicyGameTest {
    private static final BlockPos ORIGIN = new BlockPos(1, 1, 1);

    @GameTest
    public void faceAdjacencyLeavesDiagonalOreIntact(GameTestHelper context) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(context, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, 2);
            config.adjacencyMode = AdjacencyMode.FACES;
            BlockPos diagonal = ORIGIN.east().above();
            context.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            context.setBlock(diagonal, Blocks.DIAMOND_ORE);

            destroyOrigin(context, player);

            context.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            context.assertBlockPresent(Blocks.DIAMOND_ORE, diagonal);
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        context.succeed();
    }

    @GameTest
    public void categoryLimitCapsConnectedOreCount(GameTestHelper context) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(context, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, 2);
            context.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            context.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);
            context.setBlock(ORIGIN.east(2), Blocks.DIAMOND_ORE);

            destroyOrigin(context, player);

            context.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            context.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN.east());
            context.assertBlockPresent(Blocks.DIAMOND_ORE, ORIGIN.east(2));
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        context.succeed();
    }

    @GameTest
    public void additionalBlocksUseConfiguredCostsAndAwardMiningStats(GameTestHelper context) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(context, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, 2);
            config.stopBeforeBreakingTool = false;
            config.durabilityCostPerBlock = 3;
            config.exhaustionCostPerBlock = 4.0F;
            context.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            context.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);
            int previousStat = player.getStats().getValue(Stats.BLOCK_MINED, Blocks.DIAMOND_ORE);
            float previousSaturation = player.getFoodData().getSaturationLevel();

            destroyOrigin(context, player);
            player.getFoodData().tick(player);

            int minedBlocks = player.getStats().getValue(Stats.BLOCK_MINED, Blocks.DIAMOND_ORE) - previousStat;
            if (minedBlocks != 2) {
                throw new AssertionError("Expected two diamond-ore mining statistics, found " + minedBlocks + ".");
            }
            int toolDamage = player.getMainHandItem().getDamageValue();
            if (toolDamage != 4) {
                throw new AssertionError("Expected one vanilla and three configured durability damage, found " + toolDamage + ".");
            }
            float saturationLoss = previousSaturation - player.getFoodData().getSaturationLevel();
            if (Math.abs(saturationLoss - 1.0F) > 0.0001F) {
                throw new AssertionError("Expected configured exhaustion to consume one saturation point, found " + saturationLoss + ".");
            }
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        context.succeed();
    }

    @GameTest
    public void toolProtectionStopsBeforeLastDurabilityPoint(GameTestHelper context) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(context, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, 2);
            config.stopBeforeBreakingTool = true;
            config.minimumRemainingDurability = 1;
            config.durabilityCostPerBlock = 1;
            ItemStack tool = player.getMainHandItem();
            tool.setDamageValue(tool.getMaxDamage() - 2);
            context.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            context.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);

            destroyOrigin(context, player);

            context.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            if (context.getBlockState(ORIGIN.east()).isAir()) {
                ItemStack currentTool = player.getMainHandItem();
                throw new AssertionError("Tool protection mined the extra block; captured damage="
                        + tool.getDamageValue() + ", current damage=" + currentTool.getDamageValue()
                        + ", max=" + currentTool.getMaxDamage() + ", minimum="
                        + config.minimumRemainingDurability + ", cost=" + config.durabilityCostPerBlock + ".");
            }
            if (tool.isEmpty() || tool.getMaxDamage() - tool.getDamageValue() != 1) {
                throw new AssertionError("Expected the pickaxe to survive with exactly one durability point.");
            }
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        context.succeed();
    }

    @GameTest
    public void betterTreeMiningConnectsStrippedWoodFamily(GameTestHelper context) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(context, Items.DIAMOND_AXE);
        try {
            config.enableVeinMining = true;
            config.maxTreeBlocks = 2;
            config.betterTreeVeinMining = true;
            config.whitelist.clear();
            config.deniedBlocks.clear();
            config.deniedTags.clear();
            config.allowedTags.clear();
            config.allowedTags.add(VeinMinerConfig.TREE_CATEGORY_TAG.location().toString());
            context.setBlock(ORIGIN, Blocks.OAK_LOG);
            context.setBlock(ORIGIN.above(), Blocks.STRIPPED_OAK_LOG);

            destroyOrigin(context, player);

            context.assertBlockNotPresent(Blocks.OAK_LOG, ORIGIN);
            context.assertBlockNotPresent(Blocks.STRIPPED_OAK_LOG, ORIGIN.above());
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        context.succeed();
    }

    @GameTest
    public void denyTagOverridesAllowedBlock(GameTestHelper context) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(context, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, 2);
            config.deniedTags.add(VeinMinerConfig.ORE_CATEGORY_TAG.location().toString());
            context.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            context.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);

            destroyOrigin(context, player);

            context.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            context.assertBlockPresent(Blocks.DIAMOND_ORE, ORIGIN.east());
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        context.succeed();
    }

    @GameTest
    public void personalWhitelistFurtherNarrowsServerSelection(GameTestHelper context) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(context, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, 2);
            config.betterOreVeinMining = true;
            config.addBlockToWhitelist(Blocks.DEEPSLATE_DIAMOND_ORE);
            VeinMinerPlayerSettings.clearPersonalWhitelist(player);
            if (VeinMinerPlayerSettings.addPersonalBlock(player, Blocks.DIAMOND_ORE)
                    != VeinMinerPlayerSettings.WhitelistAddResult.ADDED) {
                throw new AssertionError("Expected diamond ore to enter the personal whitelist.");
            }
            context.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            context.setBlock(ORIGIN.east(), Blocks.DEEPSLATE_DIAMOND_ORE);

            destroyOrigin(context, player);

            context.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            context.assertBlockPresent(Blocks.DEEPSLATE_DIAMOND_ORE, ORIGIN.east());
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
        context.succeed();
    }

    @GameTest
    public void fastLeafDecayHasAnIndependentMasterSwitch(GameTestHelper context) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        try {
            BlockState decayingLeaves = Blocks.OAK_LEAVES.defaultBlockState()
                    .setValue(BlockStateProperties.PERSISTENT, false)
                    .setValue(BlockStateProperties.DISTANCE, LeavesBlock.DECAY_DISTANCE);
            config.betterTreeVeinMining = false;
            config.fastLeafDecayEnabled = true;
            config.leafDecaySpeedMultiplier = 100;
            if (!LeafDecayRateHandler.shouldCancelVanillaDecay(decayingLeaves)) {
                throw new AssertionError("Fast leaf decay should remain active independently of tree-family mining.");
            }
            config.fastLeafDecayEnabled = false;
            if (LeafDecayRateHandler.shouldCancelVanillaDecay(decayingLeaves)) {
                throw new AssertionError("Disabling fast leaf decay should restore vanilla decay.");
            }
        } finally {
            snapshot.restore(config);
        }
        context.succeed();
    }

    @GameTest
    public void playerToggleIsPersistedInWorldStorage(GameTestHelper context) {
        ServerPlayer player = createPlayer(context, Items.DIAMOND_PICKAXE);
        try {
            if (!VeinMinerPlayerSettings.setEnabled(player, false)) {
                throw new AssertionError("Expected the player toggle to save.");
            }
            Path settingsPath = context.getLevel().getServer().getWorldPath(LevelResource.ROOT)
                    .resolve("vein-miner-players.json");
            JsonObject players = JsonParser.parseString(Files.readString(settingsPath))
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
        context.succeed();
    }

    @GameTest
    public void combinedServerHelpIncludesVeinMiner(GameTestHelper context) {
        boolean registered = ServerModHelpRegistry.entries().stream()
                .anyMatch(entry -> entry.modId().equals(VeinMiner.MOD_ID)
                        && entry.helpCommand().equals("/veinminer")
                        && entry.adminConfigCommand().equals("/veinminer admin"));
        if (!registered) {
            throw new AssertionError("Expected Vein Miner in the shared server help registry.");
        }
        context.succeed();
    }

    private static ServerPlayer createPlayer(GameTestHelper context, net.minecraft.world.item.Item tool) {
        ServerPlayer player = (ServerPlayer) context.makeMockServerPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));
        VeinMinerPlayerSettings.setEnabled(player, true);
        VeinMinerPlayerSettings.setActivationMode(player, ActivationMode.ALWAYS);
        VeinMinerPlayerSettings.setPersonalWhitelistEnabled(player, false);
        return player;
    }

    private static void configureOreMining(VeinMinerConfig config, int limit) {
        config.enableVeinMining = true;
        config.maxOreBlocks = limit;
        config.adjacencyMode = AdjacencyMode.FACES_EDGES_CORNERS;
        config.betterOreVeinMining = true;
        config.stopBeforeBreakingTool = true;
        config.minimumRemainingDurability = 1;
        config.durabilityCostPerBlock = 1;
        config.exhaustionCostPerBlock = 0.005F;
        config.whitelist.clear();
        config.allowedTags.clear();
        config.deniedBlocks.clear();
        config.deniedTags.clear();
        config.addBlockToWhitelist(Blocks.DIAMOND_ORE);
    }

    private static void destroyOrigin(GameTestHelper context, ServerPlayer player) {
        if (!player.gameMode.destroyBlock(context.absolutePos(ORIGIN))) {
            throw new AssertionError("Expected the survival player to destroy the origin block.");
        }
    }

    private static void resetPlayer(ServerPlayer player) {
        VeinMinerPlayerSettings.setEnabled(player, true);
        VeinMinerPlayerSettings.setActivationMode(player, ActivationMode.WHILE_SNEAKING);
        VeinMinerPlayerSettings.clearPersonalWhitelist(player);
        VeinMinerPlayerSettings.setPersonalWhitelistEnabled(player, false);
    }

    private static final class ConfigSnapshot {
        private final boolean enableVeinMining;
        private final ActivationMode defaultActivationMode;
        private final AdjacencyMode adjacencyMode;
        private final int maxOreBlocks;
        private final int maxTreeBlocks;
        private final int maxOtherBlocks;
        private final boolean betterOreVeinMining;
        private final boolean betterTreeVeinMining;
        private final boolean stopBeforeBreakingTool;
        private final int minimumRemainingDurability;
        private final int durabilityCostPerBlock;
        private final float exhaustionCostPerBlock;
        private final boolean fastLeafDecayEnabled;
        private final int leafDecaySpeedMultiplier;
        private final LinkedHashSet<String> whitelist;
        private final LinkedHashSet<String> allowedTags;
        private final LinkedHashSet<String> deniedBlocks;
        private final LinkedHashSet<String> deniedTags;

        private ConfigSnapshot(VeinMinerConfig config) {
            enableVeinMining = config.enableVeinMining;
            defaultActivationMode = config.defaultActivationMode;
            adjacencyMode = config.adjacencyMode;
            maxOreBlocks = config.maxOreBlocks;
            maxTreeBlocks = config.maxTreeBlocks;
            maxOtherBlocks = config.maxOtherBlocks;
            betterOreVeinMining = config.betterOreVeinMining;
            betterTreeVeinMining = config.betterTreeVeinMining;
            stopBeforeBreakingTool = config.stopBeforeBreakingTool;
            minimumRemainingDurability = config.minimumRemainingDurability;
            durabilityCostPerBlock = config.durabilityCostPerBlock;
            exhaustionCostPerBlock = config.exhaustionCostPerBlock;
            fastLeafDecayEnabled = config.fastLeafDecayEnabled;
            leafDecaySpeedMultiplier = config.leafDecaySpeedMultiplier;
            whitelist = new LinkedHashSet<>(config.whitelist);
            allowedTags = new LinkedHashSet<>(config.allowedTags);
            deniedBlocks = new LinkedHashSet<>(config.deniedBlocks);
            deniedTags = new LinkedHashSet<>(config.deniedTags);
        }

        private void restore(VeinMinerConfig config) {
            config.enableVeinMining = enableVeinMining;
            config.defaultActivationMode = defaultActivationMode;
            config.adjacencyMode = adjacencyMode;
            config.maxOreBlocks = maxOreBlocks;
            config.maxTreeBlocks = maxTreeBlocks;
            config.maxOtherBlocks = maxOtherBlocks;
            config.betterOreVeinMining = betterOreVeinMining;
            config.betterTreeVeinMining = betterTreeVeinMining;
            config.stopBeforeBreakingTool = stopBeforeBreakingTool;
            config.minimumRemainingDurability = minimumRemainingDurability;
            config.durabilityCostPerBlock = durabilityCostPerBlock;
            config.exhaustionCostPerBlock = exhaustionCostPerBlock;
            config.fastLeafDecayEnabled = fastLeafDecayEnabled;
            config.leafDecaySpeedMultiplier = leafDecaySpeedMultiplier;
            config.whitelist = new LinkedHashSet<>(whitelist);
            config.allowedTags = new LinkedHashSet<>(allowedTags);
            config.deniedBlocks = new LinkedHashSet<>(deniedBlocks);
            config.deniedTags = new LinkedHashSet<>(deniedTags);
        }
    }
}
