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
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;

/**
 * Vein Miner breaking a vein with Telekinesis installed, registered by both loaders' telekinesis
 * integration suites.
 */
public final class VeinMinerTelekinesisGameTests {
    private static final int VEIN_LENGTH = 3;
    private static final int DURABILITY_COST_PER_BLOCK = 3;
    // Sets every mined block's experience to one fixed amount (the suite's data), so the test can
    // count each block's share: an ore's own amount is random (diamond ore 3 to 7,
    // Blocks.java:1340).
    private static final ResourceKey<Enchantment> FIXED_EXPERIENCE =
            ResourceKey.create(
                    Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(
                            "vein_miner_telekinesis_gametest", "fixed_experience"));

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
            pickaxe.enchant(
                    helper.getLevel()
                            .registryAccess()
                            .lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(FIXED_EXPERIENCE),
                    1);
            int experiencePerBlock =
                    EnchantmentHelper.processBlockExperience(helper.getLevel(), pickaxe, 0);
            // One orb per block, which no other block's orb joins (ExperienceOrb.java:204-216
            // merges only within half a block), so the orbs' values add up to the vein's
            // experience.
            helper.assertValueEqual(
                    ExperienceOrb.getExperienceValue(experiencePerBlock),
                    experiencePerBlock,
                    "a block's experience as a single orb");
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
            List<ExperienceOrb> orbs = helper.getEntities(EntityTypes.EXPERIENCE_ORB);
            helper.assertValueEqual(
                    experienceGained,
                    telekinesis ? experiencePerBlock * VEIN_LENGTH : 0,
                    "experience given to the miner");
            helper.assertValueEqual(orbs.size(), telekinesis ? 0 : VEIN_LENGTH, "experience orbs");
            helper.assertValueEqual(
                    orbs.stream().mapToInt(ExperienceOrb::getValue).sum(),
                    telekinesis ? 0 : experiencePerBlock * VEIN_LENGTH,
                    "experience in orbs");

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
