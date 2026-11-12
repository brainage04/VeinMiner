package io.github.brainage04.vein_miner.neoforge;

import io.github.brainage04.vein_miner.VeinMiner;
import io.github.brainage04.vein_miner.platform.ServerPlatform;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(VeinMiner.MOD_ID)
public final class VeinMinerNeoForge implements ServerPlatform {
    public VeinMinerNeoForge() {
        VeinMiner.initialize(this);
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void registerCommands(CommandRegistrar registrar) {
        NeoForge.EVENT_BUS.addListener(
                (RegisterCommandsEvent event) ->
                        registrar.register(event.getDispatcher(), event.getBuildContext()));
    }

    @Override
    public void registerBlockBreak(BlockBreakCallback callback) {
        // Opt in to cancelled events: the game sends one pre-cancelled when it would refuse the
        // break, and
        // another mod may un-cancel it (neoforge-26.3.0.99 CommonHooks.java:606-632).
        NeoForge.EVENT_BUS.addListener(
                true,
                BreakBlockEvent.class,
                (BreakBlockEvent event) -> {
                    // Also fired for a client's own break (neoforge-26.3.0.99 MultiPlayerGameMode
                    // and
                    // ServerPlayerGameMode both call CommonHooks.fireBlockBreak); that
                    // client-thread call
                    // must not clear the vein the server thread recorded.
                    if (event.getLevel() instanceof ServerLevel level
                            && !callback.beforeBreak(
                                    level,
                                    event.getPlayer(),
                                    event.getPos(),
                                    event.getState(),
                                    level.getBlockEntity(event.getPos()))) {
                        event.setCanceled(true);
                    }
                });
    }

    @Override
    public void registerServerTick(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener(
                (ServerTickEvent.Post event) -> callback.accept(event.getServer()));
    }

    @Override
    public void registerServerStarted(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener(
                (ServerStartedEvent event) -> callback.accept(event.getServer()));
    }

    @Override
    public void registerServerStopping(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener(
                (ServerStoppingEvent event) -> callback.accept(event.getServer()));
    }
}
