package io.github.brainage04.vein_miner.gametest;

import com.google.gson.Gson;
import io.github.brainage04.vein_miner.config.ActivationMode;
import io.github.brainage04.vein_miner.config.AdjacencyMode;
import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.player.VeinMinerPlayerSettings;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
import net.minecraft.world.level.border.WorldBorder;

/** Players, config set-up and counters shared by the GameTests of both loaders. */
public final class VeinMinerTestFixtures {
    public static final BlockPos ORIGIN = new BlockPos(1, 1, 1);
    private static final double BORDER_TEST_SIZE = 64.0D;

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
        prepareVeinMiner(player, tool);
        return player;
    }

    /** Makes {@code player} vein mine with every break while holding {@code tool}. */
    public static void prepareVeinMiner(ServerPlayer player, ItemStack tool) {
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        VeinMinerPlayerSettings.setEnabled(player, true);
        VeinMinerPlayerSettings.setActivationMode(player, ActivationMode.ALWAYS);
        VeinMinerPlayerSettings.setPersonalWhitelistEnabled(player, false);
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
        config.minimumRemainingDurability = VeinMinerConfig.DEFAULT_MINIMUM_REMAINING_DURABILITY;
        config.durabilityCostPerBlock = VeinMinerConfig.DEFAULT_DURABILITY_COST_PER_BLOCK;
        config.exhaustionCostPerBlock = VeinMinerConfig.DEFAULT_EXHAUSTION_COST_PER_BLOCK;
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

    /**
     * Moves the world border so that {@code firstOutside} is the first block column east of it, and
     * returns the action that puts the border back. Callers restore it before their test function
     * returns, so the border never moves while another test runs.
     */
    public static Runnable moveEastBorderTo(GameTestHelper helper, BlockPos firstOutside) {
        WorldBorder border = helper.getLevel().getWorldBorder();
        double centerX = border.getCenterX();
        double centerZ = border.getCenterZ();
        double size = border.getSize();
        BlockPos absolute = helper.absolutePos(firstOutside);
        border.setSize(BORDER_TEST_SIZE);
        border.setCenter(absolute.getX() - BORDER_TEST_SIZE / 2.0D, absolute.getZ() + 0.5D);
        return () -> {
            border.setCenter(centerX, centerZ);
            border.setSize(size);
        };
    }

    /** Stands in for another mod's break listener; positions are absolute. */
    public interface BreakInterference {
        /** Refuses the break at {@code pos}. */
        void refuseAt(BlockPos pos);

        /** Runs {@code action} when the break at {@code pos} is about to happen. */
        void runBefore(BlockPos pos, Runnable action);

        void stop();
    }

    /** Every server setting, so each test can put back what it changed. */
    public static final class ConfigSnapshot {
        private static final Gson GSON = new Gson();
        private final String json;

        public ConfigSnapshot(VeinMinerConfig config) {
            json = GSON.toJson(config);
        }

        public void restore(VeinMinerConfig config) {
            VeinMinerConfig saved = GSON.fromJson(json, VeinMinerConfig.class);
            for (Field field : VeinMinerConfig.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.set(config, field.get(saved));
                } catch (IllegalAccessException exception) {
                    throw new AssertionError("Cannot restore config field " + field, exception);
                }
            }
        }
    }
}
