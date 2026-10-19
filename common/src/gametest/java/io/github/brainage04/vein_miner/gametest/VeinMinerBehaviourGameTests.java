package io.github.brainage04.vein_miner.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.brainage04.vein_miner.command.VeinMinerCommand;
import io.github.brainage04.vein_miner.config.ActivationMode;
import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import io.github.brainage04.vein_miner.player.VeinMinerPlayerSettings;
import java.util.LinkedHashSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Shared scenarios run through each loader's real command and mining hooks. */
public final class VeinMinerBehaviourGameTests {
    private static final BlockPos ORIGIN = new BlockPos(1, 1, 1);

    private VeinMinerBehaviourGameTests() {}

    public static void activationAndTogglesControlConnectedMining(GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        boolean enabled = config.enableVeinMining;
        int limit = config.maxOreBlocks;
        var deniedBlocks = new LinkedHashSet<>(config.deniedBlocks);
        var deniedTags = new LinkedHashSet<>(config.deniedTags);
        var whitelist = new LinkedHashSet<>(config.whitelist);
        ServerPlayer player = player(helper);
        try {
            config.enableVeinMining = true;
            config.maxOreBlocks = 2;
            config.deniedBlocks.clear();
            config.deniedTags.clear();
            config.addBlockToWhitelist(Blocks.DIAMOND_ORE);
            for (ActivationMode mode : ActivationMode.values()) {
                VeinMinerPlayerSettings.setActivationMode(player, mode);
                for (boolean sneaking : new boolean[] {false, true}) {
                    player.setShiftKeyDown(sneaking);
                    boolean expected = switch (mode) {
                        case ALWAYS -> true;
                        case NEVER -> false;
                        case WHILE_SNEAKING -> sneaking;
                        case WHILE_NOT_SNEAKING -> !sneaking;
                    };
                    minePair(helper, player, expected);
                }
            }
            VeinMinerPlayerSettings.setActivationMode(player, ActivationMode.ALWAYS);
            VeinMinerPlayerSettings.setEnabled(player, false);
            minePair(helper, player, false);
            VeinMinerPlayerSettings.setEnabled(player, true);
            config.enableVeinMining = false;
            minePair(helper, player, false);
            config.enableVeinMining = true;
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
            minePair(helper, player, false);
        } finally {
            config.enableVeinMining = enabled;
            config.maxOreBlocks = limit;
            config.deniedBlocks = deniedBlocks;
            config.deniedTags = deniedTags;
            config.whitelist = whitelist;
            resetPlayer(player);
        }
        helper.succeed();
    }

    public static void personalCommandsPersistAndCannotOverrideServerDenials(GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        var whitelist = new LinkedHashSet<>(config.whitelist);
        var deniedBlocks = new LinkedHashSet<>(config.deniedBlocks);
        var deniedTags = new LinkedHashSet<>(config.deniedTags);
        ServerPlayer player = player(helper);
        // Vanilla's unattached GameTest player needs a listener for command click sounds.
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(),
                new Connection(PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        CommandSourceStack source = player.createCommandSourceStack().withSuppressedOutput();
        try {
            config.deniedBlocks.clear();
            config.deniedTags.clear();
            config.addBlockToWhitelist(Blocks.DIAMOND_ORE);
            run(source, "veinminer whitelist add minecraft:diamond_ore");
            helper.assertTrue(VeinMinerPlayerSettings.isPersonalWhitelistEnabled(player), "Adding a block must enable the personal filter");
            run(source, "veinminer mode while_not_sneaking");
            run(source, "veinminer disable");
            VeinMinerPlayerSettings.load(helper.getLevel().getServer());
            helper.assertTrue(!VeinMinerPlayerSettings.isEnabled(player), "Disabled preference must survive reload");
            helper.assertTrue(VeinMinerPlayerSettings.activationMode(player) == ActivationMode.WHILE_NOT_SNEAKING, "Activation mode must survive reload");
            helper.assertTrue(VeinMinerPlayerSettings.allowsBlock(player, Blocks.DIAMOND_ORE.defaultBlockState()), "Personal whitelist must survive reload");
            run(source, "veinminer whitelist clear");
            helper.assertTrue(!VeinMinerPlayerSettings.allowsBlock(player, Blocks.DIAMOND_ORE.defaultBlockState()), "An enabled empty filter must reject blocks");
            config.deniedBlocks.add("minecraft:diamond_ore");
            helper.assertTrue(execute(source, "veinminer whitelist add minecraft:diamond_ore") == 0, "A personal command must not admit a server-denied block");
            run(source, "veinminer whitelist disable");
            helper.assertTrue(VeinMinerPlayerSettings.allowsBlock(player, Blocks.DIAMOND_ORE.defaultBlockState()), "Disabling the personal filter must restore its pass-through");
        } finally {
            config.whitelist = whitelist;
            config.deniedBlocks = deniedBlocks;
            config.deniedTags = deniedTags;
            resetPlayer(player);
        }
        helper.succeed();
    }

    public static void adminCommandsPersistSettingsAndRejectInvalidLimits(GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        boolean enabled = config.enableVeinMining;
        int ore = config.maxOreBlocks;
        int tree = config.maxTreeBlocks;
        int other = config.maxOtherBlocks;
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
        ServerPlayer player = player(helper);
        try {
            run(source, "veinminer admin disable");
            run(source, "veinminer admin limit ore 7");
            run(source, "veinminer admin limit tree 9");
            run(source, "veinminer admin limit other 3");
            config.enableVeinMining = true;
            config.maxOreBlocks = 1;
            run(source, "veinminer admin reload");
            VeinMinerConfig reloaded = VeinMinerConfigManager.getConfig();
            helper.assertTrue(!reloaded.enableVeinMining && reloaded.maxOreBlocks == 7 && reloaded.maxTreeBlocks == 9 && reloaded.maxOtherBlocks == 3, "Admin settings must be saved and reloaded independently");
            expectRejected(source, "veinminer admin limit ore 0");
            expectRejected(source, "veinminer admin limit ore 4097");
            expectRejected(player.createCommandSourceStack(), "veinminer admin enable");
            helper.assertTrue(!reloaded.enableVeinMining && reloaded.maxOreBlocks == 7, "Rejected commands must leave policy unchanged");
        } finally {
            VeinMinerConfig current = VeinMinerConfigManager.getConfig();
            current.enableVeinMining = enabled;
            current.maxOreBlocks = ore;
            current.maxTreeBlocks = tree;
            current.maxOtherBlocks = other;
            VeinMinerConfigManager.saveToDisk();
            resetPlayer(player);
        }
        helper.succeed();
    }

    public static void globalClearConfirmationDoesNotSurviveShutdown(GameTestHelper helper) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        var whitelist = new LinkedHashSet<>(config.whitelist);
        ServerPlayer player = player(helper);
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack()
                .withEntity(player).withSuppressedOutput();
        try {
            config.addBlockToWhitelist(Blocks.DIAMOND_ORE);
            run(source, "veinminer admin selection blocks allow clear");
            helper.assertTrue(!config.whitelist.isEmpty(), "First clear must request confirmation");
            VeinMinerCommand.shutdown(source.getServer());
            run(source, "veinminer admin selection blocks allow clear");
            helper.assertTrue(!config.whitelist.isEmpty(), "A previous server's confirmation must not clear this server's whitelist");
            run(source, "veinminer admin selection blocks allow clear");
            helper.assertTrue(config.whitelist.isEmpty(), "Second clear in the same session must clear the whitelist");
        } finally {
            VeinMinerCommand.shutdown(source.getServer());
            config.whitelist = whitelist;
            VeinMinerConfigManager.saveToDisk();
            resetPlayer(player);
        }
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        VeinMinerPlayerSettings.setEnabled(player, true);
        VeinMinerPlayerSettings.setPersonalWhitelistEnabled(player, false);
        return player;
    }

    private static void minePair(GameTestHelper helper, ServerPlayer player, boolean connectedMined) {
        helper.setBlock(ORIGIN, Blocks.DIAMOND_ORE);
        helper.setBlock(ORIGIN.east(), Blocks.DIAMOND_ORE);
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(ORIGIN)), "Original block must be mined normally");
        helper.assertTrue(helper.getBlockState(ORIGIN.east()).isAir() == connectedMined, "Connected mining did not match the activation/tool policy");
    }

    private static void resetPlayer(ServerPlayer player) {
        VeinMinerPlayerSettings.setEnabled(player, true);
        VeinMinerPlayerSettings.setActivationMode(player, ActivationMode.WHILE_SNEAKING);
        VeinMinerPlayerSettings.clearPersonalWhitelist(player);
        VeinMinerPlayerSettings.setPersonalWhitelistEnabled(player, false);
    }

    private static int execute(CommandSourceStack source, String command) {
        try {
            return source.getServer().getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException exception) {
            throw new AssertionError("Unexpected rejection of /" + command, exception);
        }
    }

    private static void run(CommandSourceStack source, String command) {
        if (execute(source, command) != 1) {
            throw new AssertionError("Command failed: /" + command);
        }
    }

    private static void expectRejected(CommandSourceStack source, String command) {
        try {
            source.getServer().getCommands().getDispatcher().execute(command, source);
            throw new AssertionError("Command should have been rejected: /" + command);
        } catch (CommandSyntaxException expected) {
            // Rejection must happen before any mutation.
        }
    }
}
