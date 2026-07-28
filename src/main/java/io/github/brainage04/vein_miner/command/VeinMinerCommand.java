package io.github.brainage04.vein_miner.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.brainagelib.feedback.ModFeedback;
import io.github.brainage04.vein_miner.VeinMiner;
import io.github.brainage04.vein_miner.config.ActivationMode;
import io.github.brainage04.vein_miner.config.AdjacencyMode;
import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import io.github.brainage04.vein_miner.player.VeinMinerPlayerSettings;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class VeinMinerCommand {
    private static final int CLEAR_CONFIRMATION_WINDOW_TICKS = 10 * 20;
    private static final int LIST_PAGE_SIZE = 10;
    private static final ModFeedback FEEDBACK = ModFeedback.create(VeinMiner.MOD_NAME);
    private static final Map<UUID, Integer> PENDING_GLOBAL_WHITELIST_CLEARS = new HashMap<>();

    private VeinMinerCommand() {
    }

    public static void initialize(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(literal("veinminer")
                .executes(context -> showStatus(context.getSource()))
                .then(literal("status").executes(context -> showStatus(context.getSource())))
                .then(literal("toggle").executes(context -> togglePersonal(context.getSource())))
                .then(literal("enable").executes(context -> setPersonal(context.getSource(), true)))
                .then(literal("disable").executes(context -> setPersonal(context.getSource(), false)))
                .then(literal("mode")
                        .then(literal("while_sneaking").executes(context -> setPersonalMode(context.getSource(), ActivationMode.WHILE_SNEAKING)))
                        .then(literal("while_not_sneaking").executes(context -> setPersonalMode(context.getSource(), ActivationMode.WHILE_NOT_SNEAKING)))
                        .then(literal("always").executes(context -> setPersonalMode(context.getSource(), ActivationMode.ALWAYS)))
                        .then(literal("never").executes(context -> setPersonalMode(context.getSource(), ActivationMode.NEVER))))
                .then(literal("whitelist")
                        .executes(context -> showPersonalWhitelistStatus(context.getSource()))
                        .then(literal("enable").executes(context -> setPersonalWhitelistEnabled(context.getSource(), true)))
                        .then(literal("disable").executes(context -> setPersonalWhitelistEnabled(context.getSource(), false)))
                        .then(literal("add")
                                .then(argument("block", BlockStateArgument.block(buildContext))
                                        .executes(context -> addPersonalBlock(
                                                context.getSource(),
                                                BlockStateArgument.getBlock(context, "block").getState().getBlock()
                                        ))))
                        .then(literal("remove")
                                .then(argument("block", BlockStateArgument.block(buildContext))
                                        .executes(context -> removePersonalBlock(
                                                context.getSource(),
                                                BlockStateArgument.getBlock(context, "block").getState().getBlock()
                                        ))))
                        .then(literal("clear").executes(context -> clearPersonalWhitelist(context.getSource())))
                        .then(literal("list")
                                .executes(context -> listPersonalWhitelist(context.getSource(), 1))
                                .then(argument("page", IntegerArgumentType.integer(1))
                                        .executes(context -> listPersonalWhitelist(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "page")
                                        )))))
                .then(literal("admin")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> showAdminStatus(context.getSource()))
                        .then(literal("toggle").executes(context -> toggleGlobal(context.getSource())))
                        .then(literal("enable").executes(context -> setGlobal(context.getSource(), true)))
                        .then(literal("disable").executes(context -> setGlobal(context.getSource(), false)))
                        .then(literal("reload").executes(context -> reload(context.getSource())))
                        .then(literal("default_mode")
                                .then(literal("while_sneaking").executes(context -> setDefaultMode(context.getSource(), ActivationMode.WHILE_SNEAKING)))
                                .then(literal("while_not_sneaking").executes(context -> setDefaultMode(context.getSource(), ActivationMode.WHILE_NOT_SNEAKING)))
                                .then(literal("always").executes(context -> setDefaultMode(context.getSource(), ActivationMode.ALWAYS)))
                                .then(literal("never").executes(context -> setDefaultMode(context.getSource(), ActivationMode.NEVER))))
                        .then(literal("adjacency")
                                .then(literal("faces").executes(context -> setAdjacency(context.getSource(), AdjacencyMode.FACES)))
                                .then(literal("faces_edges").executes(context -> setAdjacency(context.getSource(), AdjacencyMode.FACES_EDGES)))
                                .then(literal("faces_edges_corners").executes(context -> setAdjacency(context.getSource(), AdjacencyMode.FACES_EDGES_CORNERS))))
                        .then(literal("limit")
                                .then(literal("ore")
                                        .then(argument("blocks", IntegerArgumentType.integer(1, VeinMinerConfig.MAX_BLOCKS_PER_VEIN))
                                                .executes(context -> setLimit(context.getSource(), VeinMinerConfig.BlockCategory.ORE, IntegerArgumentType.getInteger(context, "blocks")))))
                                .then(literal("tree")
                                        .then(argument("blocks", IntegerArgumentType.integer(1, VeinMinerConfig.MAX_BLOCKS_PER_VEIN))
                                                .executes(context -> setLimit(context.getSource(), VeinMinerConfig.BlockCategory.TREE, IntegerArgumentType.getInteger(context, "blocks")))))
                                .then(literal("other")
                                        .then(argument("blocks", IntegerArgumentType.integer(1, VeinMinerConfig.MAX_BLOCKS_PER_VEIN))
                                                .executes(context -> setLimit(context.getSource(), VeinMinerConfig.BlockCategory.OTHER, IntegerArgumentType.getInteger(context, "blocks"))))))
                        .then(literal("durability")
                                .then(literal("cost")
                                        .then(argument("points", IntegerArgumentType.integer(0, VeinMinerConfig.MAX_DURABILITY_COST))
                                                .executes(context -> setDurabilityCost(context.getSource(), IntegerArgumentType.getInteger(context, "points")))))
                                .then(literal("minimum_remaining")
                                        .then(argument("points", IntegerArgumentType.integer(0, VeinMinerConfig.MAX_BLOCKS_PER_VEIN))
                                                .executes(context -> setMinimumDurability(context.getSource(), IntegerArgumentType.getInteger(context, "points")))))
                                .then(literal("protect_tool")
                                        .then(literal("enable").executes(context -> setToolProtection(context.getSource(), true)))
                                        .then(literal("disable").executes(context -> setToolProtection(context.getSource(), false)))))
                        .then(literal("exhaustion")
                                .then(argument("amount", FloatArgumentType.floatArg(0.0F, VeinMinerConfig.MAX_EXHAUSTION_COST))
                                        .executes(context -> setExhaustionCost(context.getSource(), FloatArgumentType.getFloat(context, "amount")))))
                        .then(literal("better_ores")
                                .then(literal("enable").executes(context -> setBetterOres(context.getSource(), true)))
                                .then(literal("disable").executes(context -> setBetterOres(context.getSource(), false))))
                        .then(literal("better_trees")
                                .then(literal("enable").executes(context -> setBetterTrees(context.getSource(), true)))
                                .then(literal("disable").executes(context -> setBetterTrees(context.getSource(), false))))
                        .then(literal("fast_leaf_decay")
                                .then(literal("enable").executes(context -> setFastLeafDecay(context.getSource(), true)))
                                .then(literal("disable").executes(context -> setFastLeafDecay(context.getSource(), false)))
                                .then(literal("multiplier")
                                        .then(argument("amount", IntegerArgumentType.integer(1, VeinMinerConfig.MAX_LEAF_DECAY_SPEED_MULTIPLIER))
                                                .executes(context -> setLeafDecayMultiplier(context.getSource(), IntegerArgumentType.getInteger(context, "amount"))))))
                        .then(literal("selection")
                                .then(literal("blocks")
                                        .then(literal("allow")
                                                .then(literal("add")
                                                        .then(argument("block", BlockStateArgument.block(buildContext))
                                                                .executes(context -> addGlobalBlock(context.getSource(), BlockStateArgument.getBlock(context, "block").getState().getBlock(), false))))
                                                .then(literal("remove")
                                                        .then(argument("block", BlockStateArgument.block(buildContext))
                                                                .executes(context -> removeGlobalBlock(context.getSource(), BlockStateArgument.getBlock(context, "block").getState().getBlock(), false))))
                                                .then(literal("clear").executes(context -> clearGlobalWhitelist(context.getSource())))
                                                .then(literal("list")
                                                        .executes(context -> listSelection(context.getSource(), "Explicitly allowed blocks", VeinMinerConfigManager.getConfig().whitelistAsSortedList(), 1))
                                                        .then(argument("page", IntegerArgumentType.integer(1))
                                                                .executes(context -> listSelection(context.getSource(), "Explicitly allowed blocks", VeinMinerConfigManager.getConfig().whitelistAsSortedList(), IntegerArgumentType.getInteger(context, "page"))))))
                                        .then(literal("deny")
                                                .then(literal("add")
                                                        .then(argument("block", BlockStateArgument.block(buildContext))
                                                                .executes(context -> addGlobalBlock(context.getSource(), BlockStateArgument.getBlock(context, "block").getState().getBlock(), true))))
                                                .then(literal("remove")
                                                        .then(argument("block", BlockStateArgument.block(buildContext))
                                                                .executes(context -> removeGlobalBlock(context.getSource(), BlockStateArgument.getBlock(context, "block").getState().getBlock(), true))))
                                                .then(literal("list")
                                                        .executes(context -> listSelection(context.getSource(), "Denied blocks", sorted(VeinMinerConfigManager.getConfig().deniedBlocks), 1))
                                                        .then(argument("page", IntegerArgumentType.integer(1))
                                                                .executes(context -> listSelection(context.getSource(), "Denied blocks", sorted(VeinMinerConfigManager.getConfig().deniedBlocks), IntegerArgumentType.getInteger(context, "page")))))))
                                .then(literal("tags")
                                        .then(literal("allow")
                                                .then(literal("add")
                                                        .then(argument("tag", StringArgumentType.word())
                                                                .executes(context -> addGlobalTag(context.getSource(), StringArgumentType.getString(context, "tag"), false))))
                                                .then(literal("remove")
                                                        .then(argument("tag", StringArgumentType.word())
                                                                .executes(context -> removeGlobalTag(context.getSource(), StringArgumentType.getString(context, "tag"), false))))
                                                .then(literal("list")
                                                        .executes(context -> listSelection(context.getSource(), "Allowed block tags", sorted(VeinMinerConfigManager.getConfig().allowedTags), 1))
                                                        .then(argument("page", IntegerArgumentType.integer(1))
                                                                .executes(context -> listSelection(context.getSource(), "Allowed block tags", sorted(VeinMinerConfigManager.getConfig().allowedTags), IntegerArgumentType.getInteger(context, "page"))))))
                                        .then(literal("deny")
                                                .then(literal("add")
                                                        .then(argument("tag", StringArgumentType.word())
                                                                .executes(context -> addGlobalTag(context.getSource(), StringArgumentType.getString(context, "tag"), true))))
                                                .then(literal("remove")
                                                        .then(argument("tag", StringArgumentType.word())
                                                                .executes(context -> removeGlobalTag(context.getSource(), StringArgumentType.getString(context, "tag"), true))))
                                                .then(literal("list")
                                                        .executes(context -> listSelection(context.getSource(), "Denied block tags", sorted(VeinMinerConfigManager.getConfig().deniedTags), 1))
                                                        .then(argument("page", IntegerArgumentType.integer(1))
                                                                .executes(context -> listSelection(context.getSource(), "Denied block tags", sorted(VeinMinerConfigManager.getConfig().deniedTags), IntegerArgumentType.getInteger(context, "page"))))))))));
    }

    public static void tick(MinecraftServer server) {
        if (PENDING_GLOBAL_WHITELIST_CLEARS.isEmpty()) {
            return;
        }
        int currentTick = server.getTickCount();
        var iterator = PENDING_GLOBAL_WHITELIST_CLEARS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (currentTick < entry.getValue()) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                FEEDBACK.neutral(player, "Global whitelist clear cancelled.");
            }
            iterator.remove();
        }
    }

    private static int showStatus(CommandSourceStack source) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException exception) {
            return showAdminStatus(source);
        }

        FEEDBACK.neutral(player, "Status");
        FEEDBACK.neutral(player, "Global mining: %s | Your toggle: %s | Effective: %s",
                onOff(config.enableVeinMining),
                onOff(VeinMinerPlayerSettings.isEnabled(player)),
                onOff(config.enableVeinMining && VeinMinerPlayerSettings.isEnabled(player)));
        FEEDBACK.neutral(player, "Activation: %s | Personal whitelist: %s (%d blocks)",
                VeinMinerPlayerSettings.activationMode(player).serializedName(),
                onOff(VeinMinerPlayerSettings.isPersonalWhitelistEnabled(player)),
                VeinMinerPlayerSettings.personalWhitelist(player).size());
        FEEDBACK.neutral(player, "Use /veinminer toggle, /veinminer mode <mode>, or /veinminer whitelist.");
        FEEDBACK.neutral(player, "Operator controls (if permitted): /veinminer admin");
        return 1;
    }

    private static int showAdminStatus(CommandSourceStack source) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        FEEDBACK.neutral(source, "Server config: global=%s, default mode=%s, adjacency=%s",
                onOff(config.enableVeinMining), config.defaultActivationMode.serializedName(), config.adjacencyMode.serializedName());
        FEEDBACK.neutral(source, "Limits: ore=%d, tree=%d, other=%d", config.maxOreBlocks, config.maxTreeBlocks, config.maxOtherBlocks);
        FEEDBACK.neutral(source, "Costs/additional block: durability=%d, exhaustion=%s, protect tool=%s at %d remaining",
                config.durabilityCostPerBlock, config.exhaustionCostPerBlock, onOff(config.stopBeforeBreakingTool), config.minimumRemainingDurability);
        FEEDBACK.neutral(source, "Selection: %d blocks + %d tags allowed; %d blocks + %d tags denied",
                config.whitelist.size(), config.allowedTags.size(), config.deniedBlocks.size(), config.deniedTags.size());
        FEEDBACK.neutral(source, "Trees=%s, ores=%s, fast leaf decay=%s (%dx). Use /veinminer admin <setting>.",
                onOff(config.betterTreeVeinMining), onOff(config.betterOreVeinMining), onOff(config.fastLeafDecayEnabled), config.leafDecaySpeedMultiplier);
        return 1;
    }

    private static int togglePersonal(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return setPersonal(source, !VeinMinerPlayerSettings.isEnabled(player));
    }

    private static int setPersonal(CommandSourceStack source, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean saved = VeinMinerPlayerSettings.setEnabled(player, enabled);
        FEEDBACK.click(player);
        if (!saved) {
            return FEEDBACK.failure(source, "Your Vein Miner toggle changed for this session but could not be saved.");
        }
        return FEEDBACK.success(source, "Your Vein Miner toggle is now %s.", false, onOff(enabled));
    }

    private static int setPersonalMode(CommandSourceStack source, ActivationMode mode) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean saved = VeinMinerPlayerSettings.setActivationMode(player, mode);
        FEEDBACK.click(player);
        if (!saved) {
            return FEEDBACK.failure(source, "Your activation mode changed for this session but could not be saved.");
        }
        return FEEDBACK.success(source, "Your activation mode is now %s.", false, mode.serializedName());
    }

    private static int showPersonalWhitelistStatus(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return FEEDBACK.neutral(source, "Personal whitelist: %s, %d/%d blocks. Use add, remove, list, clear, enable, or disable.",
                onOff(VeinMinerPlayerSettings.isPersonalWhitelistEnabled(player)),
                VeinMinerPlayerSettings.personalWhitelist(player).size(),
                VeinMinerPlayerSettings.MAX_PERSONAL_WHITELIST_SIZE);
    }

    private static int setPersonalWhitelistEnabled(CommandSourceStack source, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean saved = VeinMinerPlayerSettings.setPersonalWhitelistEnabled(player, enabled);
        FEEDBACK.click(player);
        if (!saved) {
            return FEEDBACK.failure(source, "Personal whitelist changed for this session but could not be saved.");
        }
        return FEEDBACK.success(source, "Personal whitelist is now %s.", false, onOff(enabled));
    }

    private static int addPersonalBlock(CommandSourceStack source, Block block) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        return switch (VeinMinerPlayerSettings.addPersonalBlock(player, block)) {
            case ADDED -> FEEDBACK.success(source, "Added %s to your personal whitelist and enabled the filter.", false, blockId);
            case ALREADY_PRESENT -> FEEDBACK.failure(source, "%s is already in your personal whitelist.", blockId);
            case NOT_GLOBALLY_ALLOWED -> FEEDBACK.failure(source, "%s is not allowed by the server selection policy.", blockId);
            case LIMIT_REACHED -> FEEDBACK.failure(source, "Personal whitelist limit reached (%d blocks).", VeinMinerPlayerSettings.MAX_PERSONAL_WHITELIST_SIZE);
        };
    }

    private static int removePersonalBlock(CommandSourceStack source, Block block) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        if (!VeinMinerPlayerSettings.removePersonalBlock(player, block)) {
            return FEEDBACK.failure(source, "%s is not in your personal whitelist.", blockId);
        }
        return FEEDBACK.success(source, "Removed %s from your personal whitelist.", false, blockId);
    }

    private static int clearPersonalWhitelist(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!VeinMinerPlayerSettings.clearPersonalWhitelist(player)) {
            return FEEDBACK.failure(source, "Personal whitelist cleared for this session but could not be saved.");
        }
        return FEEDBACK.success(source, "Personal whitelist cleared; the empty filter remains enabled.", false);
    }

    private static int listPersonalWhitelist(CommandSourceStack source, int page) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return listSelection(source, "Your personal whitelist", VeinMinerPlayerSettings.personalWhitelist(player), page);
    }

    private static int toggleGlobal(CommandSourceStack source) {
        return setGlobal(source, !VeinMinerConfigManager.getConfig().enableVeinMining);
    }

    private static int setGlobal(CommandSourceStack source, boolean enabled) {
        return updateConfig(source, config -> config.enableVeinMining = enabled, "Server-wide Vein Miner is now " + onOff(enabled) + ".");
    }

    private static int setDefaultMode(CommandSourceStack source, ActivationMode mode) {
        return updateConfig(source, config -> config.defaultActivationMode = mode, "Default player activation mode set to " + mode.serializedName() + ".");
    }

    private static int setAdjacency(CommandSourceStack source, AdjacencyMode mode) {
        return updateConfig(source, config -> config.adjacencyMode = mode, "Adjacency mode set to " + mode.serializedName() + ".");
    }

    private static int setLimit(CommandSourceStack source, VeinMinerConfig.BlockCategory category, int blocks) {
        return updateConfig(source, config -> {
            switch (category) {
                case ORE -> config.maxOreBlocks = blocks;
                case TREE -> config.maxTreeBlocks = blocks;
                case OTHER -> config.maxOtherBlocks = blocks;
            }
        }, category.name().toLowerCase() + " limit set to " + blocks + " blocks.");
    }

    private static int setDurabilityCost(CommandSourceStack source, int points) {
        return updateConfig(source, config -> config.durabilityCostPerBlock = points, "Durability cost set to " + points + " per additional block.");
    }

    private static int setMinimumDurability(CommandSourceStack source, int points) {
        return updateConfig(source, config -> config.minimumRemainingDurability = points, "Minimum preserved durability set to " + points + ".");
    }

    private static int setToolProtection(CommandSourceStack source, boolean enabled) {
        return updateConfig(source, config -> config.stopBeforeBreakingTool = enabled, "Tool-break protection is now " + onOff(enabled) + ".");
    }

    private static int setExhaustionCost(CommandSourceStack source, float amount) {
        return updateConfig(source, config -> config.exhaustionCostPerBlock = amount, "Exhaustion cost set to " + amount + " per additional block.");
    }

    private static int setBetterOres(CommandSourceStack source, boolean enabled) {
        return updateConfig(source, config -> config.betterOreVeinMining = enabled, "Cross-variant ore mining is now " + onOff(enabled) + ".");
    }

    private static int setBetterTrees(CommandSourceStack source, boolean enabled) {
        return updateConfig(source, config -> config.betterTreeVeinMining = enabled, "Tree-family mining is now " + onOff(enabled) + ".");
    }

    private static int setFastLeafDecay(CommandSourceStack source, boolean enabled) {
        return updateConfig(source, config -> config.fastLeafDecayEnabled = enabled, "Fast leaf decay is now " + onOff(enabled) + ".");
    }

    private static int setLeafDecayMultiplier(CommandSourceStack source, int amount) {
        return updateConfig(source, config -> config.leafDecaySpeedMultiplier = amount, "Fast leaf decay multiplier set to " + amount + "x.");
    }

    private static int addGlobalBlock(CommandSourceStack source, Block block, boolean denied) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        LinkedHashSet<String> selection = denied ? config.deniedBlocks : config.whitelist;
        String value = id.toString();
        if (id == BuiltInRegistries.BLOCK.getDefaultKey() || selection.contains(value)) {
            return FEEDBACK.failure(source, "%s is already in that selection.", id);
        }
        if (selection.size() >= VeinMinerConfig.MAX_SELECTION_ENTRIES) {
            return FEEDBACK.failure(source, "That selection already contains the maximum of %d entries.",
                    VeinMinerConfig.MAX_SELECTION_ENTRIES);
        }
        selection.add(value);
        return saveSelectionChange(source, "%s %s.", denied ? "Denied" : "Allowed", id);
    }

    private static int removeGlobalBlock(CommandSourceStack source, Block block, boolean denied) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        LinkedHashSet<String> selection = denied ? config.deniedBlocks : config.whitelist;
        if (!selection.remove(id.toString())) {
            return FEEDBACK.failure(source, "%s is not in that selection.", id);
        }
        return saveSelectionChange(source, "Removed %s from the %s block selection.", id, denied ? "denied" : "allowed");
    }

    private static int addGlobalTag(CommandSourceStack source, String tagInput, boolean denied) {
        Identifier tagId = Identifier.tryParse(tagInput);
        if (tagId == null) {
            return FEEDBACK.failure(source, "Invalid block tag identifier: %s", tagInput);
        }
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        LinkedHashSet<String> selection = denied ? config.deniedTags : config.allowedTags;
        String value = tagId.toString();
        if (selection.contains(value)) {
            return FEEDBACK.failure(source, "%s is already in that tag selection.", tagId);
        }
        if (selection.size() >= VeinMinerConfig.MAX_SELECTION_ENTRIES) {
            return FEEDBACK.failure(source, "That tag selection already contains the maximum of %d entries.",
                    VeinMinerConfig.MAX_SELECTION_ENTRIES);
        }
        selection.add(value);
        return saveSelectionChange(source, "%s block tag #%s.", denied ? "Denied" : "Allowed", tagId);
    }

    private static int removeGlobalTag(CommandSourceStack source, String tagInput, boolean denied) {
        Identifier tagId = Identifier.tryParse(tagInput);
        if (tagId == null) {
            return FEEDBACK.failure(source, "Invalid block tag identifier: %s", tagInput);
        }
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        LinkedHashSet<String> selection = denied ? config.deniedTags : config.allowedTags;
        if (!selection.remove(tagId.toString())) {
            return FEEDBACK.failure(source, "#%s is not in that tag selection.", tagId);
        }
        return saveSelectionChange(source, "Removed block tag #%s from the %s selection.", tagId, denied ? "denied" : "allowed");
    }

    private static int clearGlobalWhitelist(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            VeinMinerConfigManager.getConfig().whitelist.clear();
            return saveSelectionChange(source, "Explicit allowed-block whitelist cleared.");
        }
        int currentTick = source.getServer().getTickCount();
        Integer expiresAt = PENDING_GLOBAL_WHITELIST_CLEARS.get(player.getUUID());
        if (expiresAt != null && currentTick < expiresAt) {
            PENDING_GLOBAL_WHITELIST_CLEARS.remove(player.getUUID());
            VeinMinerConfigManager.getConfig().whitelist.clear();
            return saveSelectionChange(source, "Explicit allowed-block whitelist cleared.");
        }
        PENDING_GLOBAL_WHITELIST_CLEARS.put(player.getUUID(), currentTick + CLEAR_CONFIRMATION_WINDOW_TICKS);
        return FEEDBACK.neutral(source, "Run this command again within 10 seconds to clear the global explicit-block whitelist.");
    }

    private static int listSelection(CommandSourceStack source, String title, List<String> entries, int requestedPage) {
        if (entries.isEmpty()) {
            return FEEDBACK.neutral(source, title + " is empty.");
        }
        int pageCount = (entries.size() + LIST_PAGE_SIZE - 1) / LIST_PAGE_SIZE;
        if (requestedPage > pageCount) {
            return FEEDBACK.failure(source, "Page %d does not exist; valid pages are 1-%d.", requestedPage, pageCount);
        }
        int start = (requestedPage - 1) * LIST_PAGE_SIZE;
        int end = Math.min(start + LIST_PAGE_SIZE, entries.size());
        FEEDBACK.neutral(source, "%s (%d entries, page %d/%d):", title, entries.size(), requestedPage, pageCount);
        for (int index = start; index < end; index++) {
            FEEDBACK.neutral(source, Component.literal(" - " + entries.get(index)));
        }
        return 1;
    }

    private static int updateConfig(CommandSourceStack source, Consumer<VeinMinerConfig> change, String message) {
        change.accept(VeinMinerConfigManager.getConfig());
        if (!VeinMinerConfigManager.saveToDisk()) {
            return FEEDBACK.failure(source, "Setting changed in memory but could not be saved. Check server logs.");
        }
        return FEEDBACK.success(source, message, true);
    }

    private static int saveSelectionChange(CommandSourceStack source, String message, Object... arguments) {
        if (!VeinMinerConfigManager.saveToDisk()) {
            return FEEDBACK.failure(source, "Selection changed in memory but could not be saved. Check server logs.");
        }
        return FEEDBACK.success(source, message, true, arguments);
    }

    private static int reload(CommandSourceStack source) {
        if (!VeinMinerConfigManager.reloadFromDisk()) {
            return FEEDBACK.failure(source, "Failed to reload config. Current in-memory settings were preserved; check server logs.");
        }
        return FEEDBACK.success(source, "Vein Miner config reloaded from disk.", true);
    }

    private static List<String> sorted(LinkedHashSet<String> values) {
        return values.stream().sorted().toList();
    }

    private static String onOff(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }
}
