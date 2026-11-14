package io.github.brainage04.vein_miner;

import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.ORIGIN;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.configureOreMining;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.createPlayer;
import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.resetPlayer;

import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.ConfigSnapshot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;

/** Another mod refusing or releasing the break through NeoForge's BreakBlockEvent. */
public final class VeinMinerNeoForgePolicyGameTests {
    private VeinMinerNeoForgePolicyGameTests() {}

    public static void refusedOriginAtHighestPriorityMinesNoVein(GameTestHelper context) {
        refusedOriginMinesNoVein(context, EventPriority.HIGHEST);
    }

    public static void refusedOriginAtLowestPriorityMinesNoVein(GameTestHelper context) {
        refusedOriginMinesNoVein(context, EventPriority.LOWEST);
    }

    public static void releasedPreCancelledBreakMinesTheVein(GameTestHelper context) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(context, GameType.CREATIVE, creativeOnlyTool());
        try {
            configureOreMining(config, 2);
            context.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            context.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);
            VeinMinerBreakInterference.releaseCanceled();

            context.assertTrue(
                    player.gameMode.destroyBlock(context.absolutePos(ORIGIN)),
                    "Another mod's release must let the break through.");
            context.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN);
            context.assertBlockNotPresent(Blocks.DIAMOND_ORE, ORIGIN.east());
        } finally {
            VeinMinerBreakInterference.stop();
            snapshot.restore(config);
            resetPlayer(player);
        }
        context.succeed();
    }

    private static void refusedOriginMinesNoVein(GameTestHelper context, EventPriority priority) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        ServerPlayer player = createPlayer(context, Items.DIAMOND_PICKAXE);
        try {
            configureOreMining(config, 2);
            context.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
            context.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);
            VeinMinerBreakInterference.refuseAt(priority, context.absolutePos(ORIGIN));

            context.assertFalse(
                    player.gameMode.destroyBlock(context.absolutePos(ORIGIN)),
                    "Another mod's refusal must stop the break.");
            context.assertBlockPresent(Blocks.DIAMOND_ORE, ORIGIN);
            context.assertBlockPresent(Blocks.DIAMOND_ORE, ORIGIN.east());
        } finally {
            VeinMinerBreakInterference.stop();
            snapshot.restore(config);
            resetPlayer(player);
        }
        context.succeed();
    }

    private static ItemStack creativeOnlyTool() {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        Tool tool = pickaxe.get(DataComponents.TOOL);
        pickaxe.set(
                DataComponents.TOOL,
                new Tool(tool.rules(), tool.defaultMiningSpeed(), tool.damagePerBlock(), false));
        return pickaxe;
    }
}
