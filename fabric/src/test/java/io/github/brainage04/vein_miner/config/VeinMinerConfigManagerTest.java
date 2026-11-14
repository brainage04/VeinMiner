package io.github.brainage04.vein_miner.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VeinMinerConfigManagerTest {
    // Written out from v1.0.1's VeinMinerConfig.defaultWhitelistBlocks(), independently of the
    // copy the migration compares against.
    private static final List<String> LEGACY_1_0_1_DEFAULT_WHITELIST =
            List.of(
                    "minecraft:coal_ore",
                    "minecraft:iron_ore",
                    "minecraft:gold_ore",
                    "minecraft:copper_ore",
                    "minecraft:diamond_ore",
                    "minecraft:emerald_ore",
                    "minecraft:lapis_ore",
                    "minecraft:redstone_ore",
                    "minecraft:deepslate_coal_ore",
                    "minecraft:deepslate_iron_ore",
                    "minecraft:deepslate_gold_ore",
                    "minecraft:deepslate_copper_ore",
                    "minecraft:deepslate_diamond_ore",
                    "minecraft:deepslate_emerald_ore",
                    "minecraft:deepslate_lapis_ore",
                    "minecraft:deepslate_redstone_ore",
                    "minecraft:nether_quartz_ore",
                    "minecraft:nether_gold_ore",
                    "minecraft:ancient_debris",
                    "minecraft:oak_log",
                    "minecraft:spruce_log",
                    "minecraft:birch_log",
                    "minecraft:jungle_log",
                    "minecraft:acacia_log",
                    "minecraft:dark_oak_log",
                    "minecraft:pale_oak_log",
                    "minecraft:mangrove_log",
                    "minecraft:cherry_log",
                    "minecraft:crimson_stem",
                    "minecraft:warped_stem");

    @TempDir Path directory;

    @Test
    void malformedLegacyLimitKeepsCategoryDefaultsAndRequestsRewrite() {
        VeinMinerConfig config = VeinMinerConfig.createDefault();
        var document = JsonParser.parseString("{\"veinSize\":\"not-a-number\"}").getAsJsonObject();

        assertTrue(VeinMinerConfigManager.migrateLegacyVeinSize(document, config));
        assertEquals(VeinMinerConfig.DEFAULT_MAX_ORE_BLOCKS, config.maxOreBlocks);
        assertEquals(VeinMinerConfig.DEFAULT_MAX_TREE_BLOCKS, config.maxTreeBlocks);
        assertEquals(VeinMinerConfig.DEFAULT_MAX_OTHER_BLOCKS, config.maxOtherBlocks);
    }

    @Test
    void numericLegacyLimitMigratesAllCategoryLimits() {
        VeinMinerConfig config = VeinMinerConfig.createDefault();
        var document = JsonParser.parseString("{\"veinSize\":42}").getAsJsonObject();

        assertTrue(VeinMinerConfigManager.migrateLegacyVeinSize(document, config));
        assertEquals(42, config.maxOreBlocks);
        assertEquals(42, config.maxTreeBlocks);
        assertEquals(42, config.maxOtherBlocks);
    }

    @Test
    void configWrittenByThePreviousVersionLoadsAndSavesWithALineBreak() throws IOException {
        // The previous version wrote the config with this Gson and no trailing newline; ConfigFile
        // writes the same document and ends it with a line break.
        VeinMinerConfig written = VeinMinerConfig.createDefault();
        written.maxOreBlocks = 17;
        String previous =
                new GsonBuilder()
                        .setPrettyPrinting()
                        .disableHtmlEscaping()
                        .create()
                        .toJson(written);
        Path path = directory.resolve("vein_miner.json");
        Files.writeString(path, previous);

        VeinMinerConfigManager.initialize(directory);
        assertEquals(17, VeinMinerConfigManager.getConfig().maxOreBlocks);
        assertTrue(VeinMinerConfigManager.saveToDisk());
        assertEquals(previous + "\n", Files.readString(path));
    }

    @Test
    void legacyVeinSizeFileIsMigratedAndRewritten() throws IOException {
        Path path = directory.resolve("vein_miner.json");
        Files.writeString(path, "{\"veinSize\": 12}");

        VeinMinerConfigManager.initialize(directory);
        assertEquals(12, VeinMinerConfigManager.getConfig().maxOreBlocks);
        var rewritten = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals(12, rewritten.get("maxTreeBlocks").getAsInt());
    }

    @Test
    void legacyCustomWhitelistStaysTheCompleteSelection() throws IOException {
        Path path = directory.resolve("vein_miner.json");
        Files.writeString(path, "{\"veinSize\":64,\"whitelist\":[\"minecraft:diamond_ore\"]}");

        VeinMinerConfigManager.initialize(directory);
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        assertEquals(List.of("minecraft:diamond_ore"), List.copyOf(config.whitelist));
        assertEquals(List.of(), List.copyOf(config.allowedTags));
        var rewritten = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals(0, rewritten.getAsJsonArray("allowedTags").size());
    }

    @Test
    void legacyDefaultWhitelistKeepsTheDefaultTags() throws IOException {
        Path path = directory.resolve("vein_miner.json");
        JsonObject legacy = new JsonObject();
        legacy.addProperty("veinSize", 128);
        JsonArray whitelist = new JsonArray();
        LEGACY_1_0_1_DEFAULT_WHITELIST.forEach(whitelist::add);
        legacy.add("whitelist", whitelist);
        Files.writeString(path, legacy.toString());

        VeinMinerConfigManager.initialize(directory);
        assertEquals(
                List.copyOf(VeinMinerConfig.createDefault().allowedTags),
                List.copyOf(VeinMinerConfigManager.getConfig().allowedTags));
    }

    @Test
    void legacyFileWithoutAWhitelistKeepsTheDefaultTags() throws IOException {
        Path path = directory.resolve("vein_miner.json");
        Files.writeString(path, "{\"veinSize\":64}");

        VeinMinerConfigManager.initialize(directory);
        assertEquals(
                List.copyOf(VeinMinerConfig.createDefault().allowedTags),
                List.copyOf(VeinMinerConfigManager.getConfig().allowedTags));
    }

    @Test
    void currentConfigWithoutAllowedTagsIsNotTreatedAsLegacy() throws IOException {
        Path path = directory.resolve("vein_miner.json");
        Files.writeString(path, "{\"maxOreBlocks\":12,\"whitelist\":[\"minecraft:diamond_ore\"]}");

        VeinMinerConfigManager.initialize(directory);
        assertEquals(
                List.copyOf(VeinMinerConfig.createDefault().allowedTags),
                List.copyOf(VeinMinerConfigManager.getConfig().allowedTags));
    }

    @Test
    void whitelistWithoutVeinSizeIsNotTreatedAsLegacy() throws IOException {
        Path path = directory.resolve("vein_miner.json");
        Files.writeString(path, "{\"whitelist\":[\"minecraft:diamond_ore\"]}");

        VeinMinerConfigManager.initialize(directory);
        assertEquals(
                List.copyOf(VeinMinerConfig.createDefault().allowedTags),
                List.copyOf(VeinMinerConfigManager.getConfig().allowedTags));
    }

    @Test
    void configWithAllowedTagsKeepsThem() throws IOException {
        Path path = directory.resolve("vein_miner.json");
        Files.writeString(
                path,
                "{\"whitelist\":[\"minecraft:diamond_ore\"],\"allowedTags\":[\"minecraft:logs\"]}");

        VeinMinerConfigManager.initialize(directory);
        assertEquals(
                List.of("minecraft:logs"),
                List.copyOf(VeinMinerConfigManager.getConfig().allowedTags));
    }

    @Test
    void configThatFailedToLoadIsNotOverwrittenUntilAReloadSucceeds() throws IOException {
        Path path = directory.resolve("vein_miner.json");
        Files.writeString(path, "{\"maxOreBlocks\": ");

        VeinMinerConfigManager.initialize(directory);
        VeinMinerConfigManager.getConfig().maxOreBlocks = 5;
        assertFalse(VeinMinerConfigManager.saveToDisk());
        assertEquals("{\"maxOreBlocks\": ", Files.readString(path));

        Files.writeString(path, "{\"maxOreBlocks\": 9}");
        assertTrue(VeinMinerConfigManager.reloadFromDisk());
        assertEquals(9, VeinMinerConfigManager.getConfig().maxOreBlocks);
        assertTrue(VeinMinerConfigManager.saveToDisk());
    }

    @Test
    void reloadAfterDeletingTheFileRestoresDefaultsAndRecreatesIt() throws IOException {
        Path path = directory.resolve("vein_miner.json");

        VeinMinerConfigManager.initialize(directory);
        assertTrue(Files.exists(path), "A missing config must be created with the defaults");
        VeinMinerConfigManager.getConfig().maxOreBlocks = 5;
        assertTrue(VeinMinerConfigManager.saveToDisk());

        Files.delete(path);
        assertTrue(VeinMinerConfigManager.reloadFromDisk());
        assertEquals(
                VeinMinerConfig.DEFAULT_MAX_ORE_BLOCKS,
                VeinMinerConfigManager.getConfig().maxOreBlocks);
        assertTrue(Files.exists(path), "A reload must recreate a deleted config");
    }
}
