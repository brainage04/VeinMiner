package io.github.brainage04.vein_miner;

import io.github.brainage04.vein_miner.gametest.VeinMinerMiningGameTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;

public class VeinMinerGameTest {
    @GameTest
    public void connectedOreVeinDropsRespectTelekinesisCompatibility(GameTestHelper context) {
        VeinMinerMiningGameTests.connectedOreVeinDropsRespectTelekinesisCompatibility(
                context, telekinesisLoaded());
    }

    @GameTest
    public void equivalentOreBlockTypesFormOneVein(GameTestHelper context) {
        VeinMinerMiningGameTests.equivalentOreBlockTypesFormOneVein(context, telekinesisLoaded());
    }

    private static boolean telekinesisLoaded() {
        return FabricLoader.getInstance().isModLoaded("telekinesis");
    }
}
