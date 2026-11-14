package io.github.brainage04.vein_miner.telekinesis;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class VeinMinerTelekinesisGameTest {
    @GameTest
    public void veinDropsReachTheMinerThroughTelekinesis(GameTestHelper context) {
        VeinMinerTelekinesisGameTests.veinDropsReachTheMinerThroughTelekinesis(context);
    }

    @GameTest
    public void veinDropsStayInTheWorldWithTelekinesisOff(GameTestHelper context) {
        VeinMinerTelekinesisGameTests.veinDropsStayInTheWorldWithTelekinesisOff(context);
    }
}
