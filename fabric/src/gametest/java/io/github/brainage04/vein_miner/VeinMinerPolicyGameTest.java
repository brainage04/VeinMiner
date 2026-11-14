package io.github.brainage04.vein_miner;

import io.github.brainage04.vein_miner.gametest.VeinMinerBehaviourGameTests;
import io.github.brainage04.vein_miner.gametest.VeinMinerCommandGameTests;
import io.github.brainage04.vein_miner.gametest.VeinMinerPolicyGameTests;
import io.github.brainage04.vein_miner.leaf.VeinMinerLeafGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class VeinMinerPolicyGameTest {
    @GameTest
    public void queuedDecayHonoursDisableAndShutdown(GameTestHelper context) {
        VeinMinerLeafGameTests.queuedDecayHonoursDisableAndShutdown(context);
    }

    @GameTest
    public void queuedDecayHonoursRandomTickRuleAndLeafState(GameTestHelper context) {
        VeinMinerLeafGameTests.queuedDecayHonoursRandomTickRuleAndLeafState(context);
    }

    @GameTest
    public void unreadablePlayerSettingsAreNotOverwritten(GameTestHelper context) {
        VeinMinerBehaviourGameTests.unreadablePlayerSettingsAreNotOverwritten(context);
    }

    @GameTest
    public void playerSettingsFromPreviousVersionLoad(GameTestHelper context) {
        VeinMinerBehaviourGameTests.playerSettingsFromPreviousVersionLoad(context);
    }

    @GameTest
    public void globalClearConfirmationDoesNotSurviveShutdown(GameTestHelper context) {
        VeinMinerBehaviourGameTests.globalClearConfirmationDoesNotSurviveShutdown(context);
    }

    @GameTest
    public void activationAndTogglesControlConnectedMining(GameTestHelper context) {
        VeinMinerBehaviourGameTests.activationAndTogglesControlConnectedMining(context);
    }

    @GameTest
    public void personalCommandsPersistAndCannotOverrideServerDenials(GameTestHelper context) {
        VeinMinerBehaviourGameTests.personalCommandsPersistAndCannotOverrideServerDenials(context);
    }

    @GameTest
    public void adminCommandsPersistSettingsAndRejectInvalidLimits(GameTestHelper context) {
        VeinMinerBehaviourGameTests.adminCommandsPersistSettingsAndRejectInvalidLimits(context);
    }

    @GameTest
    public void faceAdjacencyLeavesDiagonalOreIntact(GameTestHelper context) {
        VeinMinerPolicyGameTests.faceAdjacencyLeavesDiagonalOreIntact(context);
    }

    @GameTest
    public void categoryLimitCapsConnectedOreCount(GameTestHelper context) {
        VeinMinerPolicyGameTests.categoryLimitCapsConnectedOreCount(context);
    }

    @GameTest
    public void blocksBeyondTheWorldBorderAreNotMined(GameTestHelper context) {
        VeinMinerPolicyGameTests.blocksBeyondTheWorldBorderAreNotMined(context);
    }

    @GameTest
    public void borderMovedDuringTheVeinStopsTheVeinThere(GameTestHelper context) {
        VeinMinerPolicyGameTests.borderMovedDuringTheVeinStopsTheVeinThere(
                context, VeinMinerFabricBreakInterference.INSTANCE);
    }

    @GameTest
    public void refusedExtraBlockStaysWhileTheRestIsMined(GameTestHelper context) {
        VeinMinerPolicyGameTests.refusedExtraBlockStaysWhileTheRestIsMined(
                context, VeinMinerFabricBreakInterference.INSTANCE);
    }

    @GameTest
    public void veinSearchLeavesUnloadedChunksUnloaded(GameTestHelper context) {
        VeinMinerPolicyGameTests.veinSearchLeavesUnloadedChunksUnloaded(context);
    }

    @GameTest
    public void additionalBlocksUseConfiguredCostsAndAwardMiningStats(GameTestHelper context) {
        VeinMinerPolicyGameTests.additionalBlocksUseConfiguredCostsAndAwardMiningStats(context);
    }

    @GameTest
    public void toolProtectionStopsBeforeLastDurabilityPoint(GameTestHelper context) {
        VeinMinerPolicyGameTests.toolProtectionStopsBeforeLastDurabilityPoint(context);
    }

    @GameTest
    public void betterTreeMiningConnectsStrippedWoodFamily(GameTestHelper context) {
        VeinMinerPolicyGameTests.betterTreeMiningConnectsStrippedWoodFamily(context);
    }

    @GameTest
    public void selectionTagCommandsAcceptNamespacedIds(GameTestHelper context) {
        VeinMinerCommandGameTests.selectionTagCommandsAcceptNamespacedIds(context);
    }

    @GameTest
    public void denyTagOverridesAllowedBlock(GameTestHelper context) {
        VeinMinerPolicyGameTests.denyTagOverridesAllowedBlock(context);
    }

    @GameTest
    public void personalWhitelistFurtherNarrowsServerSelection(GameTestHelper context) {
        VeinMinerPolicyGameTests.personalWhitelistFurtherNarrowsServerSelection(context);
    }

    @GameTest
    public void fastLeafDecayHasAnIndependentMasterSwitch(GameTestHelper context) {
        VeinMinerPolicyGameTests.fastLeafDecayHasAnIndependentMasterSwitch(context);
    }

    @GameTest
    public void playerToggleIsPersistedInWorldStorage(GameTestHelper context) {
        VeinMinerPolicyGameTests.playerToggleIsPersistedInWorldStorage(context);
    }

    @GameTest
    public void combinedServerHelpIncludesVeinMiner(GameTestHelper context) {
        VeinMinerPolicyGameTests.combinedServerHelpIncludesVeinMiner(context);
    }
}
