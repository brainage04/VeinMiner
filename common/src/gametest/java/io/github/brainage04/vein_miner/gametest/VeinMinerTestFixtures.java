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
