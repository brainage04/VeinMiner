package io.github.brainage04.vein_miner.gametest;

import io.github.brainage04.vein_miner.config.ActivationMode;
import io.github.brainage04.vein_miner.config.AdjacencyMode;
import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.player.VeinMinerPlayerSettings;
import java.util.LinkedHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Players, config set-up and counters shared by the GameTests of both loaders. */
public final class VeinMinerTestFixtures {
    public static final BlockPos ORIGIN = new BlockPos(1, 1, 1);

    private VeinMinerTestFixtures() {}

    /**
     * A survival player holding {@code tool}; see {@link #createPlayer(GameTestHelper, GameType,
     * ItemStack)}.
     */
    public static ServerPlayer createPlayer(GameTestHelper helper, Item tool) {
        return createPlayer(helper, GameType.SURVIVAL, new ItemStack(tool));
    }

    /** A player that vein mines with every break: enabled, always active, no personal whitelist. */
    public static ServerPlayer createPlayer(
            GameTestHelper helper, GameType gameType, ItemStack tool) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(gameType);
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        VeinMinerPlayerSettings.setEnabled(player, true);
        VeinMinerPlayerSettings.setActivationMode(player, ActivationMode.ALWAYS);
        VeinMinerPlayerSettings.setPersonalWhitelistEnabled(player, false);
        return player;
    }

    /** Puts the player's settings back to the defaults the other tests expect. */
    public static void resetPlayer(ServerPlayer player) {
        VeinMinerPlayerSettings.setEnabled(player, true);
        VeinMinerPlayerSettings.setActivationMode(player, ActivationMode.WHILE_SNEAKING);
        VeinMinerPlayerSettings.clearPersonalWhitelist(player);
        VeinMinerPlayerSettings.setPersonalWhitelistEnabled(player, false);
    }

    /**
     * Vein mining on for diamond ore alone, up to {@code limit} blocks, with every neighbour shape.
     */
    public static void configureOreMining(VeinMinerConfig config, int limit) {
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
        allowBlock(config, Blocks.DIAMOND_ORE);
    }

    /** Adds {@code block} to the server's explicitly allowed blocks. */
    public static void allowBlock(VeinMinerConfig config, Block block) {
        config.whitelist.add(BuiltInRegistries.BLOCK.getKey(block).toString());
    }

    public static void destroyOrigin(GameTestHelper helper, ServerPlayer player) {
        if (!player.gameMode.destroyBlock(helper.absolutePos(ORIGIN))) {
            throw new AssertionError("Expected the survival player to destroy the origin block.");
        }
    }

    public static int countInventoryItem(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Counts dropped items inside this test's own structure, so neighbouring tests cannot leak in.
     */
    public static int countWorldItem(GameTestHelper helper, Item item) {
        int count = 0;
        for (ItemEntity itemEntity : helper.getEntities(EntityTypes.ITEM)) {
            ItemStack stack = itemEntity.getItem();
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** The server settings the tests change, so each test can put them back. */
    public static final class ConfigSnapshot {
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

        public ConfigSnapshot(VeinMinerConfig config) {
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

        public void restore(VeinMinerConfig config) {
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
