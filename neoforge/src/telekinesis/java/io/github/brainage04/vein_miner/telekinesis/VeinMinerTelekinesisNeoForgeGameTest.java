package io.github.brainage04.vein_miner.telekinesis;

import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = VeinMinerTelekinesisNeoForgeGameTest.MOD_ID)
public final class VeinMinerTelekinesisNeoForgeGameTest {
    static final String MOD_ID = "vein_miner_telekinesis_gametest";

    private VeinMinerTelekinesisNeoForgeGameTest() {}

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        register(
                event,
                "vein_drops_reach_the_miner_through_telekinesis",
                VeinMinerTelekinesisGameTests::veinDropsReachTheMinerThroughTelekinesis);
        register(
                event,
                "vein_drops_stay_in_the_world_with_telekinesis_off",
                VeinMinerTelekinesisGameTests::veinDropsStayInTheWorldWithTelekinesisOff);
    }

    private static void register(
            RegisterEvent event, String path, Consumer<GameTestHelper> function) {
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(MOD_ID, path),
                () -> function);
    }
}
