package io.github.brainage04.vein_miner.gametest;

import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.ORIGIN;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.allowBlock;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.countInventoryItem;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.countWorldItem;

import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import java.util.LinkedHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * Where a vein's drops land, registered by both loaders; each loader says whether Telekinesis is
 * installed, which moves the drops into the miner's inventory.
 */
public final class VeinMinerMiningGameTests {
    private static final BlockPos CONNECTED = ORIGIN.east();

    private VeinMinerMiningGameTests() {}

    public static void connectedOreVeinDropsRespectTelekinesisCompatibility(
            GameTestHelper helper, boolean telekinesis) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        boolean previousEnabled = config.enableVeinMining;
        int previousMaxOreBlocks = config.maxOreBlocks;
        LinkedHashSet<String> previousWhitelist = new LinkedHashSet<>(config.whitelist);

        try {
            config.enableVeinMining = true;
            config.maxOreBlocks = 2;
            config.whitelist.clear();
            allowBlock(config, Blocks.DIAMOND_ORE);

            ServerPlayer player = sneakingMiner(helper);
            helper.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            helper.setBlock(CONNECTED, Blocks.DIAMOND_ORE);

            helper.assertTrue(
                    player.gameMode.destroyBlock(helper.absolutePos(ORIGIN)),
                    "Expected the player to destroy the vein origin.");

            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, CONNECTED);

            helper.succeedWhen(
                    () -> {
                        helper.assertValueEqual(
                                countInventoryItem(player, Items.DIAMOND),
                                telekinesis ? 2 : 0,
                                "inventory diamonds");
                        helper.assertValueEqual(
                                countWorldItem(helper, Items.DIAMOND),
                                telekinesis ? 0 : 2,
                                "world diamonds");
                    });
        } finally {
            config.enableVeinMining = previousEnabled;
            config.maxOreBlocks = previousMaxOreBlocks;
            config.whitelist.clear();
            config.whitelist.addAll(previousWhitelist);
        }
    }

    public static void equivalentOreBlockTypesFormOneVein(
            GameTestHelper helper, boolean telekinesis) {
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
            allowBlock(config, Blocks.DIAMOND_ORE);
            allowBlock(config, Blocks.DEEPSLATE_DIAMOND_ORE);

            ServerPlayer player = sneakingMiner(helper);
            helper.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            helper.setBlock(CONNECTED, Blocks.DEEPSLATE_DIAMOND_ORE);

            helper.assertTrue(
                    player.gameMode.destroyBlock(helper.absolutePos(ORIGIN)),
                    "Expected the player to destroy the mixed vein origin.");

            helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            helper.assertBlockNotPresent(Blocks.DEEPSLATE_DIAMOND_ORE, CONNECTED);

            helper.succeedWhen(
                    () -> {
                        helper.assertValueEqual(
                                countInventoryItem(player, Items.DIAMOND),
                                telekinesis ? 2 : 0,
                                "inventory diamonds from a mixed vein");
                        helper.assertValueEqual(
                                countWorldItem(helper, Items.DIAMOND),
                                telekinesis ? 0 : 2,
                                "world diamonds from a mixed vein");
                    });
        } finally {
            config.enableVeinMining = previousEnabled;
            config.betterOreVeinMining = previousBetterOreVeinMining;
            config.maxOreBlocks = previousMaxOreBlocks;
            config.whitelist.clear();
            config.whitelist.addAll(previousWhitelist);
        }
    }

    private static ServerPlayer sneakingMiner(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        return player;
    }
}
