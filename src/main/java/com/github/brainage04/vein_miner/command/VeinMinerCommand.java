package com.github.brainage04.vein_miner.command;

import com.github.brainage04.vein_miner.config.VeinMinerConfig;
import com.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class VeinMinerCommand {
    private static final int CLEAR_CONFIRMATION_WINDOW_TICKS = 10 * 20;

    private static final Map<UUID, Integer> pendingWhitelistClears = new HashMap<>();

    private VeinMinerCommand() {
    }

    public static void initialize(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(literal("veinminer")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> showStatus(context.getSource()))
                .then(literal("toggle").executes(context -> toggleEnabled(context.getSource())))
                .then(literal("enable").executes(context -> setEnabled(context.getSource(), true)))
                .then(literal("disable").executes(context -> setEnabled(context.getSource(), false)))
                .then(literal("vein_size")
                        .then(argument("amount", IntegerArgumentType.integer(1))
                                .executes(context -> setVeinSize(context.getSource(), IntegerArgumentType.getInteger(context, "amount")))))
                .then(literal("better_ore_vein_mining")
                        .then(literal("toggle").executes(context -> toggleBetterOre(context.getSource())))
                        .then(literal("enable").executes(context -> setBetterOre(context.getSource(), true)))
                        .then(literal("disable").executes(context -> setBetterOre(context.getSource(), false))))
                .then(literal("better_tree_vein_mining")
                        .then(literal("toggle").executes(context -> toggleBetterTree(context.getSource())))
                        .then(literal("enable").executes(context -> setBetterTree(context.getSource(), true)))
                        .then(literal("disable").executes(context -> setBetterTree(context.getSource(), false))))
                .then(literal("whitelist")
                        .then(literal("add")
                                .then(argument("block", BlockStateArgument.block(buildContext))
                                        .executes(context -> whitelistAdd(context.getSource(), BlockStateArgument.getBlock(context, "block").getState().getBlock()))))
                        .then(literal("remove")
                                .then(argument("block", BlockStateArgument.block(buildContext))
                                        .executes(context -> whitelistRemove(context.getSource(), BlockStateArgument.getBlock(context, "block").getState().getBlock()))))
                        .then(literal("list").executes(context -> whitelistList(context.getSource())))
                        .then(literal("clear").executes(context -> whitelistClear(context.getSource()))))
                .then(literal("load_from_disk").executes(context -> loadFromDisk(context.getSource()))));
    }

    public static void tick(MinecraftServer server) {
        if (pendingWhitelistClears.isEmpty()) {
            return;
        }

        int currentTick = server.getTickCount();
        var iterator = pendingWhitelistClears.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (currentTick < entry.getValue()) {
                continue;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                player.sendSystemMessage(Component.literal("Operation cancelled."));
            }

            iterator.remove();
        }
    }

    private static int showStatus(CommandSourceStack source) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        source.sendSuccess(() -> Component.literal("VeinMiner config:"), false);
        source.sendSuccess(() -> Component.literal(" - Enable Vein Mining: " + config.enableVeinMining), false);
        source.sendSuccess(() -> Component.literal(" - Vein Size: " + config.veinSize), false);
        source.sendSuccess(() -> Component.literal(" - Better Ore Vein Mining: " + config.betterOreVeinMining), false);
        source.sendSuccess(() -> Component.literal(" - Better Tree Vein Mining: " + config.betterTreeVeinMining), false);
        source.sendSuccess(() -> Component.literal(" - Whitelist size: " + config.whitelist.size()), false);
        return 1;
    }

    private static int toggleEnabled(CommandSourceStack source) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        return setEnabled(source, !config.enableVeinMining);
    }

    private static int setEnabled(CommandSourceStack source, boolean enabled) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        config.enableVeinMining = enabled;
        VeinMinerConfigManager.saveToDisk(source.getServer());
        source.sendSuccess(() -> Component.literal("Enable Vein Mining set to " + enabled + "."), true);
        return 1;
    }

    private static int setVeinSize(CommandSourceStack source, int veinSize) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        config.veinSize = veinSize;
        VeinMinerConfigManager.saveToDisk(source.getServer());
        source.sendSuccess(() -> Component.literal("Vein Size set to " + veinSize + "."), true);
        return 1;
    }

    private static int toggleBetterOre(CommandSourceStack source) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        return setBetterOre(source, !config.betterOreVeinMining);
    }

    private static int setBetterOre(CommandSourceStack source, boolean enabled) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        config.betterOreVeinMining = enabled;
        VeinMinerConfigManager.saveToDisk(source.getServer());
        source.sendSuccess(() -> Component.literal("Better Ore Vein Mining set to " + enabled + "."), true);
        return 1;
    }

    private static int toggleBetterTree(CommandSourceStack source) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        return setBetterTree(source, !config.betterTreeVeinMining);
    }

    private static int setBetterTree(CommandSourceStack source, boolean enabled) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        config.betterTreeVeinMining = enabled;
        VeinMinerConfigManager.saveToDisk(source.getServer());
        source.sendSuccess(() -> Component.literal("Better Tree Vein Mining set to " + enabled + "."), true);
        return 1;
    }

    private static int whitelistAdd(CommandSourceStack source, Block block) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        boolean added = config.addBlockToWhitelist(block);
        if (!added) {
            source.sendFailure(Component.literal("Block is already in the whitelist."));
            return 0;
        }

        VeinMinerConfigManager.saveToDisk(source.getServer());
        source.sendSuccess(() -> Component.literal("Added " + BuiltInRegistries.BLOCK.getKey(block) + " to the whitelist."), true);
        return 1;
    }

    private static int whitelistRemove(CommandSourceStack source, Block block) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        boolean removed = config.removeBlockFromWhitelist(block);
        if (!removed) {
            source.sendFailure(Component.literal("Block is not in the whitelist."));
            return 0;
        }

        VeinMinerConfigManager.saveToDisk(source.getServer());
        source.sendSuccess(() -> Component.literal("Removed " + BuiltInRegistries.BLOCK.getKey(block) + " from the whitelist."), true);
        return 1;
    }

    private static int whitelistList(CommandSourceStack source) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        if (config.whitelist.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Whitelist is empty."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("Whitelisted blocks (" + config.whitelist.size() + "):"), false);
        for (String blockId : config.whitelistAsSortedList()) {
            source.sendSuccess(() -> Component.literal(" - " + blockId), false);
        }

        return 1;
    }

    private static int whitelistClear(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException exception) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        int currentTick = server.getTickCount();
        UUID playerId = player.getUUID();
        Integer expiresAt = pendingWhitelistClears.get(playerId);

        if (expiresAt != null && currentTick < expiresAt) {
            pendingWhitelistClears.remove(playerId);
            VeinMinerConfig config = VeinMinerConfigManager.getConfig();
            config.whitelist.clear();
            VeinMinerConfigManager.saveToDisk(server);
            source.sendSuccess(() -> Component.literal("Whitelist cleared."), true);
            return 1;
        }

        pendingWhitelistClears.put(playerId, currentTick + CLEAR_CONFIRMATION_WINDOW_TICKS);
        source.sendSuccess(() -> Component.literal("ARE YOU SURE? This operation cannot be undone! Run the command again within 10 seconds to confirm this operation."), false);
        return 1;
    }

    private static int loadFromDisk(CommandSourceStack source) {
        boolean loaded = VeinMinerConfigManager.reloadFromDisk(source.getServer());
        if (!loaded) {
            source.sendFailure(Component.literal("Failed to load config from disk. Check server logs for details."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("VeinMiner config reloaded from disk."), true);
        return 1;
    }
}
