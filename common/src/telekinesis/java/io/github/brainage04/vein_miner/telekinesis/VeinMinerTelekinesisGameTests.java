package io.github.brainage04.vein_miner.telekinesis;

import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.ORIGIN;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.configureOreMining;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.countInventoryItem;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.countWorldItem;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.createPlayer;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.destroyOrigin;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.resetPlayer;

import io.github.brainage04.telekinesis.player.TelekinesisPlayerSettings;
import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.ConfigSnapshot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Blocks;

/**
 * Vein Miner breaking a vein with Telekinesis installed, registered by both loaders' telekinesis
 * integration suites.
 */
public final class VeinMinerTelekinesisGameTests {
    private static final int VEIN_LENGTH = 3;
    private static final int DURABILITY_COST_PER_BLOCK = 3;

    private VeinMinerTelekinesisGameTests() {}

    public static void veinDropsReachTheMinerThroughTelekinesis(GameTestHelper helper) {
        mineVein(helper, true);
    }

    public static void veinDropsStayInTheWorldWithTelekinesisOff(GameTestHelper helper) {
        mineVein(helper, false);
    }

    private static void mineVein(GameTestHelper helper, boolean telekinesis) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(helper, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, VEIN_LENGTH);
            config.durabilityCostPerBlock = DURABILITY_COST_PER_BLOCK;
            TelekinesisPlayerSettings.setEnabled(player, telekinesis);
            for (int i = 0; i < VEIN_LENGTH; i++) {
                helper.setBlock(ORIGIN.east(i), Blocks.DIAMOND_ORE);
            }
            ItemStack pickaxe = player.getMainHandItem();
            Tool tool = pickaxe.get(DataComponents.TOOL);
            int experienceBefore = player.totalExperience;

            destroyOrigin(helper, player);

            for (int i = 0; i < VEIN_LENGTH; i++) {
                helper.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN.east(i));
            }
            helper.assertValueEqual(
                    pickaxe.getDamageValue(),
                    tool.damagePerBlock() + DURABILITY_COST_PER_BLOCK * (VEIN_LENGTH - 1),
                    "pickaxe damage: vanilla's for the origin, the configured cost for each"
                            + " additional block");
            int experienceGained = player.totalExperience - experienceBefore;
            if (telekinesis) {
                helper.assertTrue(
                        experienceGained > 0,
                        "Expected the vein's experience to go to the miner, gained "
                                + experienceGained);
                helper.assertEntityNotPresent(EntityTypes.EXPERIENCE_ORB);
            } else {
                helper.assertValueEqual(experienceGained, 0, "experience gained directly");
                helper.assertEntityPresent(EntityTypes.EXPERIENCE_ORB);
            }

            helper.succeedWhen(
                    () -> {
                        helper.assertValueEqual(
                                countInventoryItem(player, Items.DIAMOND),
                                telekinesis ? VEIN_LENGTH : 0,
                                "inventory diamonds");
                        helper.assertValueEqual(
                                countWorldItem(helper, Items.DIAMOND),
                                telekinesis ? 0 : VEIN_LENGTH,
                                "world diamonds");
                    });
        } finally {
            snapshot.restore(config);
            resetPlayer(player);
        }
    }
}
