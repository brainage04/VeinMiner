package io.github.brainage04.vein_miner;

import io.github.brainage04.vein_miner.gametest.VeinMinerCommandGameTests;
import io.github.brainage04.vein_miner.gametest.VeinMinerBehaviourGameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Exercises the loader-neutral mining path through NeoForge's server GameTest runner. */
@EventBusSubscriber(modid = VeinMiner.MOD_ID)
public final class VeinMinerNeoForgeGameTest {
    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        VeinMinerNeoForgeMiningGameTests miningTests = new VeinMinerNeoForgeMiningGameTests();
        VeinMinerNeoForgePolicyGameTests policyTests = new VeinMinerNeoForgePolicyGameTests();
        register(event, "activation_and_toggles_control_connected_mining",
                VeinMinerBehaviourGameTests::activationAndTogglesControlConnectedMining);
        register(event, "personal_commands_persist_and_cannot_override_server_denials",
                VeinMinerBehaviourGameTests::personalCommandsPersistAndCannotOverrideServerDenials);
        register(event, "admin_commands_persist_settings_and_reject_invalid_limits",
                VeinMinerBehaviourGameTests::adminCommandsPersistSettingsAndRejectInvalidLimits);
        register(
                event,
                "connected_ore_vein_drops_respect_telekinesis_compatibility",
                miningTests::connectedOreVeinDropsRespectTelekinesisCompatibility);
        register(
                event,
                "equivalent_ore_block_types_form_one_vein",
                miningTests::equivalentOreBlockTypesFormOneVein);
        register(
                event,
                "face_adjacency_leaves_diagonal_ore_intact",
                policyTests::faceAdjacencyLeavesDiagonalOreIntact);
        register(
                event,
                "category_limit_caps_connected_ore_count",
                policyTests::categoryLimitCapsConnectedOreCount);
        register(
                event,
                "additional_blocks_use_configured_costs_and_award_mining_stats",
                policyTests::additionalBlocksUseConfiguredCostsAndAwardMiningStats);
        register(
                event,
                "tool_protection_stops_before_last_durability_point",
                policyTests::toolProtectionStopsBeforeLastDurabilityPoint);
        register(
                event,
                "better_tree_mining_connects_stripped_wood_family",
                policyTests::betterTreeMiningConnectsStrippedWoodFamily);
        register(
                event,
                "selection_tag_commands_accept_namespaced_ids",
                VeinMinerCommandGameTests::selectionTagCommandsAcceptNamespacedIds);
        register(
                event,
                "deny_tag_overrides_allowed_block",
                policyTests::denyTagOverridesAllowedBlock);
        register(
                event,
                "personal_whitelist_further_narrows_server_selection",
                policyTests::personalWhitelistFurtherNarrowsServerSelection);
        register(
                event,
                "fast_leaf_decay_has_an_independent_master_switch",
                policyTests::fastLeafDecayHasAnIndependentMasterSwitch);
        register(
                event,
                "player_toggle_is_persisted_in_world_storage",
                policyTests::playerToggleIsPersistedInWorldStorage);
        register(
                event,
                "combined_server_help_includes_vein_miner",
                policyTests::combinedServerHelpIncludesVeinMiner);
    }

    private static void register(
            RegisterEvent event,
            String path,
            java.util.function.Consumer<GameTestHelper> function) {
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(VeinMiner.MOD_ID, path),
                () -> function);
    }

}
