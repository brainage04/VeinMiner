package io.github.brainage04.vein_miner.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VeinMinerConfigManagerTest {
    @TempDir
    Path directory;

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
    void configWrittenByThePreviousVersionLoadsAndKeepsItsFormat() throws IOException {
        // The previous version wrote the config with this Gson and no trailing newline.
        VeinMinerConfig written = VeinMinerConfig.createDefault();
        written.maxOreBlocks = 17;
        String previous = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(written);
        Path path = directory.resolve("vein_miner.json");
        Files.writeString(path, previous);

        VeinMinerConfigManager.initialize(directory);
        assertEquals(17, VeinMinerConfigManager.getConfig().maxOreBlocks);
        assertTrue(VeinMinerConfigManager.saveToDisk());
        assertEquals(previous, Files.readString(path));
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
}
