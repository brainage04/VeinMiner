package io.github.brainage04.vein_miner.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;
import java.util.function.Consumer;

/** Loader services used by Vein Miner's server-only shared implementation. */
public interface ServerPlatform {
    String loaderName();

    Path configDirectory();

    void registerCommands(CommandRegistrar registrar);

    void registerBlockBreak(BlockBreakCallback callback);

    void registerServerTick(Consumer<MinecraftServer> callback);

    void registerServerStarted(Consumer<MinecraftServer> callback);

    void registerServerStopping(Consumer<MinecraftServer> callback);


    @FunctionalInterface
    interface CommandRegistrar {
        void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context);
    }

    @FunctionalInterface
    interface BlockBreakCallback {
        boolean beforeBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity);
    }
}
