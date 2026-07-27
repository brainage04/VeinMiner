package io.github.brainage04.vein_miner.neoforge;

import io.github.brainage04.vein_miner.VeinMiner;
import io.github.brainage04.vein_miner.platform.ServerPlatform;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

@Mod(VeinMiner.MOD_ID)
public final class VeinMinerNeoForge implements ServerPlatform {
    public VeinMinerNeoForge() {
        VeinMiner.initialize(this);
    }

    @Override public String loaderName() { return "NeoForge"; }
    @Override public Path configDirectory() { return FMLPaths.CONFIGDIR.get(); }
    @Override public void registerCommands(CommandRegistrar registrar) {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> registrar.register(event.getDispatcher(), event.getBuildContext()));
    }
    @Override public void registerBlockBreak(BlockBreakCallback callback) {
        // NeoForge 26.2 no longer exposes a cancellable block-break event; the loader mixin invokes this hook.
    }
    @Override public void registerServerTick(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> callback.accept(event.getServer()));
    }
    @Override public void registerServerStarted(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> callback.accept(event.getServer()));
    }
    @Override public void registerServerStopping(Consumer<MinecraftServer> callback) {
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> callback.accept(event.getServer()));
    }
}
