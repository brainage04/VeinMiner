package io.github.brainage04.vein_miner;

import io.github.brainage04.vein_miner.config.ActivationMode;
import io.github.brainage04.vein_miner.config.AdjacencyMode;
import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VeinMinerConfigTest {
    @Test
    void invalidValuesAreResetToSafeDefaults() {
        VeinMinerConfig config = VeinMinerConfig.createDefault();
        config.maxOreBlocks = VeinMinerConfig.MAX_BLOCKS_PER_VEIN + 1;
        config.maxTreeBlocks = 0;
        config.maxOtherBlocks = -1;
        config.leafDecaySpeedMultiplier = Integer.MAX_VALUE;
        config.durabilityCostPerBlock = -1;
        config.minimumRemainingDurability = -1;
        config.exhaustionCostPerBlock = Float.NaN;
        config.defaultActivationMode = null;
        config.adjacencyMode = null;
        config.whitelist = new LinkedHashSet<>(List.of("minecraft:diamond_ore", "not valid"));

        List<String> corrections = config.normalize();

        assertFalse(corrections.isEmpty());
        assertEquals(VeinMinerConfig.DEFAULT_MAX_ORE_BLOCKS, config.maxOreBlocks);
        assertEquals(VeinMinerConfig.DEFAULT_MAX_TREE_BLOCKS, config.maxTreeBlocks);
        assertEquals(VeinMinerConfig.DEFAULT_MAX_OTHER_BLOCKS, config.maxOtherBlocks);
        assertEquals(VeinMinerConfig.DEFAULT_LEAF_DECAY_SPEED_MULTIPLIER, config.leafDecaySpeedMultiplier);
        assertEquals(1, config.durabilityCostPerBlock);
        assertEquals(1, config.minimumRemainingDurability);
        assertEquals(0.005F, config.exhaustionCostPerBlock);
        assertEquals(ActivationMode.WHILE_SNEAKING, config.defaultActivationMode);
        assertEquals(AdjacencyMode.FACES_EDGES_CORNERS, config.adjacencyMode);
        assertEquals(List.of("minecraft:diamond_ore"), config.whitelistAsSortedList());
    }


    @Test
    void adjacencyModesHaveDistinctNeighborhoods() {
        assertTrue(AdjacencyMode.FACES.includes(1, 0, 0));
        assertFalse(AdjacencyMode.FACES.includes(1, 1, 0));
        assertTrue(AdjacencyMode.FACES_EDGES.includes(1, 1, 0));
        assertFalse(AdjacencyMode.FACES_EDGES.includes(1, 1, 1));
        assertTrue(AdjacencyMode.FACES_EDGES_CORNERS.includes(1, 1, 1));
    }

    @Test
    void defaultsSelectCategoryTagsRatherThanHardCodedBlockLists() {
        VeinMinerConfig config = VeinMinerConfig.createDefault();
        assertEquals(
                List.of("vein_miner:ores", "vein_miner:trees"),
                config.allowedTags.stream().toList()
        );
        assertTrue(config.whitelist.isEmpty());
    }
}
