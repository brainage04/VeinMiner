package io.github.brainage04.vein_miner.config;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VeinMinerConfigManagerTest {
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
}
