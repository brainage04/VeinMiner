package io.github.brainage04.vein_miner;

import io.github.brainage04.minecraftmodconventions.ClientGameTestServers;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

@SuppressWarnings("UnstableApiUsage")
public class VeinMinerClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        ClientGameTestServers.withDedicatedServer(
                context,
                ClientGameTestServers.flatServerProperties(),
                "VeinMiner GameTest",
                server -> {
                    ClientGameTestServers.assertClientWorldAndPlayerAvailable(context);
                    VeinMinerShowcase.film(context, server);
                });
    }
}
