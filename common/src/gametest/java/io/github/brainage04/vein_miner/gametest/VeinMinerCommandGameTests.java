package io.github.brainage04.vein_miner.gametest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;

/// GameTest bodies shared by the Fabric and NeoForge GameTest registrations.
public final class VeinMinerCommandGameTests {
    private static final String TAGS = "veinminer admin selection tags ";

    private VeinMinerCommandGameTests() {
    }

    /// Tag ids are namespaced (`c:ores`); an un-namespaced id keeps meaning `minecraft:`.
    public static void selectionTagCommandsAcceptNamespacedIds(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        LinkedHashSet<String> allowed = new LinkedHashSet<>(config.allowedTags);
        LinkedHashSet<String> denied = new LinkedHashSet<>(config.deniedTags);
        try {
            config.allowedTags.clear();
            config.deniedTags.clear();

            run(dispatcher, source, TAGS + "allow add c:ores");
            run(dispatcher, source, TAGS + "allow add planks");
            run(dispatcher, source, TAGS + "deny add minecraft:logs");
            assertSelection(List.of("c:ores", "minecraft:planks"), config.allowedTags, "allowed");
            assertSelection(List.of("minecraft:logs"), config.deniedTags, "denied");

            List<String> removable = suggestions(dispatcher, source, TAGS + "allow remove ");
            assertTrue(removable.equals(List.of("c:ores", "minecraft:planks")),
                    "Expected remove to suggest the allowed tags, got " + removable);
            assertTrue(suggestions(dispatcher, source, TAGS + "deny add minecraft:lo").contains("minecraft:logs"),
                    "Expected add to suggest block tags");

            run(dispatcher, source, TAGS + "allow remove c:ores");
            run(dispatcher, source, TAGS + "allow remove minecraft:planks");
            run(dispatcher, source, TAGS + "deny remove logs");
            assertSelection(List.of(), config.allowedTags, "allowed");
            assertSelection(List.of(), config.deniedTags, "denied");
        } finally {
            config.allowedTags = allowed;
            config.deniedTags = denied;
            VeinMinerConfigManager.saveToDisk();
        }
        helper.succeed();
    }

    private static void run(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, String command) {
        int result;
        try {
            result = dispatcher.execute(command, source);
        } catch (CommandSyntaxException exception) {
            throw new AssertionError("/" + command + " was rejected: " + exception.getMessage(), exception);
        }
        assertTrue(result == 1, "/" + command + " did not succeed");
    }

    private static List<String> suggestions(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSourceStack source,
            String partialCommand
    ) {
        return dispatcher.getCompletionSuggestions(dispatcher.parse(partialCommand, source)).join().getList().stream()
                .map(Suggestion::getText)
                .toList();
    }

    private static void assertSelection(List<String> expected, LinkedHashSet<String> actual, String name) {
        assertTrue(List.copyOf(actual).equals(expected), "Expected " + name + " tags " + expected + ", got " + actual);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
