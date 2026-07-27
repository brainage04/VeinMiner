package io.github.brainage04.vein_miner;

import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import net.neoforged.fml.ModList;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashSet;

public class VeinMinerNeoForgeMiningGameTests {
    private static final BlockPos ORIGIN_POS = new BlockPos(1, 1, 1);
    private static final BlockPos CONNECTED_POS = ORIGIN_POS.east();

    public void connectedOreVeinDropsRespectTelekinesisCompatibility(GameTestHelper context) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        boolean previousEnabled = config.enableVeinMining;
        int previousMaxOreBlocks = config.maxOreBlocks;
        LinkedHashSet<String> previousWhitelist = new LinkedHashSet<>(config.whitelist);

        try {
            config.enableVeinMining = true;
            config.maxOreBlocks = 2;
            config.whitelist.clear();
            config.addBlockToWhitelist(Blocks.DIAMOND_ORE);

            ServerPlayer player = (ServerPlayer) context.makeMockServerPlayer(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            context.setBlock(ORIGIN_POS, Blocks.DIAMOND_ORE);
            context.setBlock(CONNECTED_POS, Blocks.DIAMOND_ORE);

            if (!player.gameMode.destroyBlock(context.absolutePos(ORIGIN_POS))) {
                throw new AssertionError("Expected the player to destroy the vein origin.");
            }

            context.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN_POS);
            context.assertBlockNotPresent(Blocks.DIAMOND_ORE, CONNECTED_POS);

            int inventoryDiamonds = countInventoryItem(player, Items.DIAMOND);
            int worldDiamonds = countWorldItem(context, Items.DIAMOND);
            if (ModList.get().isLoaded("telekinesis")) {
                assertCount("inventory diamonds with Telekinesis", inventoryDiamonds, 2);
                assertCount("world diamonds with Telekinesis", worldDiamonds, 0);
            } else {
                assertCount("inventory diamonds without Telekinesis", inventoryDiamonds, 0);
                assertCount("world diamonds without Telekinesis", worldDiamonds, 2);
            }
        } finally {
            config.enableVeinMining = previousEnabled;
            config.maxOreBlocks = previousMaxOreBlocks;
            config.whitelist.clear();
            config.whitelist.addAll(previousWhitelist);
        }

        context.succeed();
    }

    public void equivalentOreBlockTypesFormOneVein(GameTestHelper context) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        boolean previousEnabled = config.enableVeinMining;
        boolean previousBetterOreVeinMining = config.betterOreVeinMining;
        int previousMaxOreBlocks = config.maxOreBlocks;
        LinkedHashSet<String> previousWhitelist = new LinkedHashSet<>(config.whitelist);

        try {
            config.enableVeinMining = true;
            config.betterOreVeinMining = true;
            config.maxOreBlocks = 2;
            config.whitelist.clear();
            config.addBlockToWhitelist(Blocks.DIAMOND_ORE);
            config.addBlockToWhitelist(Blocks.DEEPSLATE_DIAMOND_ORE);

            ServerPlayer player = (ServerPlayer) context.makeMockServerPlayer(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            context.setBlock(ORIGIN_POS, Blocks.DIAMOND_ORE);
            context.setBlock(CONNECTED_POS, Blocks.DEEPSLATE_DIAMOND_ORE);

            if (!player.gameMode.destroyBlock(context.absolutePos(ORIGIN_POS))) {
                throw new AssertionError("Expected the player to destroy the mixed vein origin.");
            }

            context.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN_POS);
            context.assertBlockNotPresent(Blocks.DEEPSLATE_DIAMOND_ORE, CONNECTED_POS);

            int inventoryDiamonds = countInventoryItem(player, Items.DIAMOND);
            int worldDiamonds = countWorldItem(context, Items.DIAMOND);
            if (ModList.get().isLoaded("telekinesis")) {
                assertCount("inventory diamonds from a mixed vein with Telekinesis", inventoryDiamonds, 2);
                assertCount("world diamonds from a mixed vein with Telekinesis", worldDiamonds, 0);
            } else {
                assertCount("inventory diamonds from a mixed vein without Telekinesis", inventoryDiamonds, 0);
                assertCount("world diamonds from a mixed vein without Telekinesis", worldDiamonds, 2);
            }
        } finally {
            config.enableVeinMining = previousEnabled;
            config.betterOreVeinMining = previousBetterOreVeinMining;
            config.maxOreBlocks = previousMaxOreBlocks;
            config.whitelist.clear();
            config.whitelist.addAll(previousWhitelist);
        }

        context.succeed();
    }

    private static int countInventoryItem(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countWorldItem(GameTestHelper context, Item item) {
        BlockPos origin = context.absolutePos(ORIGIN_POS);
        AABB bounds = new AABB(origin).inflate(5.0D);
        int count = 0;
        for (ItemEntity itemEntity : context.getLevel().getEntitiesOfClass(ItemEntity.class, bounds)) {
            ItemStack stack = itemEntity.getItem();
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void assertCount(String description, int actual, int expected) {
        if (actual != expected) {
            throw new AssertionError("Expected " + expected + " " + description + ", found " + actual + ".");
        }
    }
}
