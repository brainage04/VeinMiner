package io.github.brainage04.vein_miner.fabric;

import io.github.brainage04.vein_miner.VeinMiner;
import io.github.brainage04.vein_miner.platform.ServerPlatform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

public final class VeinMinerFabric implements ModInitializer, ServerPlatform {
    @Override
    public void onInitialize() {
        VeinMiner.initialize(this);
    }

    @Override public String loaderName() { return "Fabric"; }
    @Override public Path configDirectory() { return FabricLoader.getInstance().getConfigDir(); }
    @Override public void registerCommands(CommandRegistrar registrar) {
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> registrar.register(dispatcher, context));
    }
    @Override public void registerBlockBreak(BlockBreakCallback callback) {
        PlayerBlockBreakEvents.BEFORE.register(callback::beforeBreak);
    }
    @Override public void registerServerTick(Consumer<MinecraftServer> callback) { ServerTickEvents.END_SERVER_TICK.register(callback::accept); }
    @Override public void registerServerStarted(Consumer<MinecraftServer> callback) { ServerLifecycleEvents.SERVER_STARTED.register(callback::accept); }
    @Override public void registerServerStopping(Consumer<MinecraftServer> callback) { ServerLifecycleEvents.SERVER_STOPPING.register(callback::accept); }
}
