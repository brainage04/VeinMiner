package io.github.brainage04.vein_miner;

import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.LinkedHashSet;

/** Exercises the loader-neutral mining path through NeoForge's server GameTest runner. */
@EventBusSubscriber(modid = VeinMiner.MOD_ID)
public final class VeinMinerNeoForgeGameTest {
    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        event.register(BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(VeinMiner.MOD_ID, "connected_ore_vein_mines"),
                () -> VeinMinerNeoForgeGameTest::connectedOreVeinMinesOnNeoForge);
        VeinMinerNeoForgeMiningGameTests miningTests = new VeinMinerNeoForgeMiningGameTests();
        VeinMinerNeoForgePolicyGameTests policyTests = new VeinMinerNeoForgePolicyGameTests();
        register(event, "connected_ore_vein_drops_respect_telekinesis_compatibility", miningTests::connectedOreVeinDropsRespectTelekinesisCompatibility);
        register(event, "equivalent_ore_block_types_form_one_vein", miningTests::equivalentOreBlockTypesFormOneVein);
        register(event, "face_adjacency_leaves_diagonal_ore_intact", policyTests::faceAdjacencyLeavesDiagonalOreIntact);
        register(event, "category_limit_caps_connected_ore_count", policyTests::categoryLimitCapsConnectedOreCount);
        register(event, "additional_blocks_use_configured_costs_and_award_mining_stats", policyTests::additionalBlocksUseConfiguredCostsAndAwardMiningStats);
        register(event, "tool_protection_stops_before_last_durability_point", policyTests::toolProtectionStopsBeforeLastDurabilityPoint);
        register(event, "better_tree_mining_connects_stripped_wood_family", policyTests::betterTreeMiningConnectsStrippedWoodFamily);
        register(event, "deny_tag_overrides_allowed_block", policyTests::denyTagOverridesAllowedBlock);
        register(event, "personal_whitelist_further_narrows_server_selection", policyTests::personalWhitelistFurtherNarrowsServerSelection);
        register(event, "fast_leaf_decay_has_an_independent_master_switch", policyTests::fastLeafDecayHasAnIndependentMasterSwitch);
        register(event, "player_toggle_is_persisted_in_world_storage", policyTests::playerToggleIsPersistedInWorldStorage);
        register(event, "combined_server_help_includes_vein_miner", policyTests::combinedServerHelpIncludesVeinMiner);
    }
    private static void register(RegisterEvent event, String path, java.util.function.Consumer<GameTestHelper> function) {
        event.register(BuiltInRegistries.TEST_FUNCTION.key(), Identifier.fromNamespaceAndPath(VeinMiner.MOD_ID, path), () -> function);
    }

    private static void connectedOreVeinMinesOnNeoForge(GameTestHelper context) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        boolean enabled = config.enableVeinMining;
        int maxOreBlocks = config.maxOreBlocks;
        LinkedHashSet<String> whitelist = new LinkedHashSet<>(config.whitelist);
        try {
            config.enableVeinMining = true;
            config.maxOreBlocks = 2;
            config.whitelist.clear();
            config.addBlockToWhitelist(Blocks.DIAMOND_ORE);
            BlockPos origin = new BlockPos(1, 1, 1);
            BlockPos connected = origin.east();
            ServerPlayer player = (ServerPlayer) context.makeMockServerPlayer(GameType.SURVIVAL);
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            context.setBlock(origin, Blocks.DIAMOND_ORE);
            context.setBlock(connected, Blocks.DIAMOND_ORE);
            if (!player.gameMode.destroyBlock(context.absolutePos(origin))) {
                throw new AssertionError("Expected the origin block to be destroyed.");
            }
            context.assertBlockNotPresent(Blocks.DIAMOND_ORE, origin);
            context.assertBlockNotPresent(Blocks.DIAMOND_ORE, connected);
            context.succeed();
        } finally {
            config.enableVeinMining = enabled;
            config.maxOreBlocks = maxOreBlocks;
            config.whitelist.clear();
            config.whitelist.addAll(whitelist);
        }
    }
}
