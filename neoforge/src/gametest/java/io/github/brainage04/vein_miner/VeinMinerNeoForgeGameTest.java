package io.github.brainage04.vein_miner;

import io.github.brainage04.vein_miner.gametest.VeinMinerBehaviourGameTests;
import io.github.brainage04.vein_miner.gametest.VeinMinerCommandGameTests;
import io.github.brainage04.vein_miner.gametest.VeinMinerMiningGameTests;
import io.github.brainage04.vein_miner.gametest.VeinMinerPolicyGameTests;
import io.github.brainage04.vein_miner.leaf.VeinMinerLeafGameTests;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = VeinMiner.MOD_ID)
public final class VeinMinerNeoForgeGameTest {
    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        register(
                event,
                "queued_decay_honours_disable_and_shutdown",
                VeinMinerLeafGameTests::queuedDecayHonoursDisableAndShutdown);
        register(
                event,
                "queued_decay_honours_random_tick_rule_and_leaf_state",
                VeinMinerLeafGameTests::queuedDecayHonoursRandomTickRuleAndLeafState);
        register(
                event,
                "unreadable_player_settings_are_not_overwritten",
                VeinMinerBehaviourGameTests::unreadablePlayerSettingsAreNotOverwritten);
        register(
                event,
                "player_settings_from_previous_version_load",
                VeinMinerBehaviourGameTests::playerSettingsFromPreviousVersionLoad);
        register(
                event,
                "global_clear_confirmation_does_not_survive_shutdown",
                VeinMinerBehaviourGameTests::globalClearConfirmationDoesNotSurviveShutdown);
        register(
                event,
                "activation_and_toggles_control_connected_mining",
                VeinMinerBehaviourGameTests::activationAndTogglesControlConnectedMining);
        register(
                event,
                "personal_commands_persist_and_cannot_override_server_denials",
                VeinMinerBehaviourGameTests::personalCommandsPersistAndCannotOverrideServerDenials);
        register(
                event,
                "admin_commands_persist_settings_and_reject_invalid_limits",
                VeinMinerBehaviourGameTests::adminCommandsPersistSettingsAndRejectInvalidLimits);
        register(
                event,
                "connected_ore_vein_drops_respect_telekinesis_compatibility",
                helper ->
                        VeinMinerMiningGameTests
                                .connectedOreVeinDropsRespectTelekinesisCompatibility(
                                        helper, telekinesisLoaded()));
        register(
                event,
                "equivalent_ore_block_types_form_one_vein",
                helper ->
                        VeinMinerMiningGameTests.equivalentOreBlockTypesFormOneVein(
                                helper, telekinesisLoaded()));
        register(
                event,
                "face_adjacency_leaves_diagonal_ore_intact",
                VeinMinerPolicyGameTests::faceAdjacencyLeavesDiagonalOreIntact);
        register(
                event,
                "refused_origin_at_highest_priority_mines_no_vein",
                VeinMinerNeoForgePolicyGameTests::refusedOriginAtHighestPriorityMinesNoVein);
        register(
                event,
                "refused_origin_at_lowest_priority_mines_no_vein",
                VeinMinerNeoForgePolicyGameTests::refusedOriginAtLowestPriorityMinesNoVein);
        register(
                event,
                "released_pre_cancelled_break_mines_the_vein",
                VeinMinerNeoForgePolicyGameTests::releasedPreCancelledBreakMinesTheVein);
        register(
                event,
                "category_limit_caps_connected_ore_count",
                VeinMinerPolicyGameTests::categoryLimitCapsConnectedOreCount);
        register(
                event,
                "blocks_beyond_the_world_border_are_not_mined",
                VeinMinerPolicyGameTests::blocksBeyondTheWorldBorderAreNotMined);
        register(
                event,
                "border_moved_during_the_vein_stops_the_vein_there",
                helper ->
                        VeinMinerPolicyGameTests.borderMovedDuringTheVeinStopsTheVeinThere(
                                helper, VeinMinerBreakInterference.AT_HIGHEST_PRIORITY));
        register(
                event,
                "refused_extra_block_stays_while_the_rest_is_mined",
                helper ->
                        VeinMinerPolicyGameTests.refusedExtraBlockStaysWhileTheRestIsMined(
                                helper, VeinMinerBreakInterference.AT_HIGHEST_PRIORITY));
        register(
                event,
                "vein_search_leaves_unloaded_chunks_unloaded",
                VeinMinerPolicyGameTests::veinSearchLeavesUnloadedChunksUnloaded);
        register(
                event,
                "additional_blocks_use_configured_costs_and_award_mining_stats",
                VeinMinerPolicyGameTests::additionalBlocksUseConfiguredCostsAndAwardMiningStats);
        register(
                event,
                "tool_protection_stops_before_last_durability_point",
                VeinMinerPolicyGameTests::toolProtectionStopsBeforeLastDurabilityPoint);
        register(
                event,
                "better_tree_mining_connects_stripped_wood_family",
                VeinMinerPolicyGameTests::betterTreeMiningConnectsStrippedWoodFamily);
        register(
                event,
                "selection_tag_commands_accept_namespaced_ids",
                VeinMinerCommandGameTests::selectionTagCommandsAcceptNamespacedIds);
        register(
                event,
                "deny_tag_overrides_allowed_block",
                VeinMinerPolicyGameTests::denyTagOverridesAllowedBlock);
        register(
                event,
                "personal_whitelist_further_narrows_server_selection",
                VeinMinerPolicyGameTests::personalWhitelistFurtherNarrowsServerSelection);
        register(
                event,
                "fast_leaf_decay_has_an_independent_master_switch",
                VeinMinerPolicyGameTests::fastLeafDecayHasAnIndependentMasterSwitch);
        register(
                event,
                "player_toggle_is_persisted_in_world_storage",
                VeinMinerPolicyGameTests::playerToggleIsPersistedInWorldStorage);
        register(
                event,
                "combined_server_help_includes_vein_miner",
                VeinMinerPolicyGameTests::combinedServerHelpIncludesVeinMiner);
    }

    private static boolean telekinesisLoaded() {
        return ModList.get().isLoaded("telekinesis");
    }

    private static void register(
            RegisterEvent event, String path, Consumer<GameTestHelper> function) {
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(VeinMiner.MOD_ID, path),
                () -> function);
    }
}
