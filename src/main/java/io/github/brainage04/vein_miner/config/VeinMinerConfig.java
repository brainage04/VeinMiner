package io.github.brainage04.vein_miner.config;

import io.github.brainage04.vein_miner.VeinMiner;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class VeinMinerConfig {
    public static final int DEFAULT_MAX_ORE_BLOCKS = 128;
    public static final int DEFAULT_MAX_TREE_BLOCKS = 256;
    public static final int DEFAULT_MAX_OTHER_BLOCKS = 64;
    public static final int DEFAULT_LEAF_DECAY_SPEED_MULTIPLIER = 100;
    public static final int MAX_BLOCKS_PER_VEIN = 4096;
    public static final int MAX_LEAF_DECAY_SPEED_MULTIPLIER = 1000;
    public static final int MAX_DURABILITY_COST = 64;
    public static final float MAX_EXHAUSTION_COST = 4.0F;
    public static final int MAX_SELECTION_ENTRIES = 4096;

    public static final TagKey<Block> ORE_CATEGORY_TAG = blockTag("ores");
    public static final TagKey<Block> TREE_CATEGORY_TAG = blockTag("trees");

    public boolean enableVeinMining = true;
    public ActivationMode defaultActivationMode = ActivationMode.WHILE_SNEAKING;
    public AdjacencyMode adjacencyMode = AdjacencyMode.FACES_EDGES_CORNERS;
    public int maxOreBlocks = DEFAULT_MAX_ORE_BLOCKS;
    public int maxTreeBlocks = DEFAULT_MAX_TREE_BLOCKS;
    public int maxOtherBlocks = DEFAULT_MAX_OTHER_BLOCKS;
    public boolean betterOreVeinMining = true;
    public boolean betterTreeVeinMining = true;
    public boolean stopBeforeBreakingTool = true;
    public int minimumRemainingDurability = 1;
    public int durabilityCostPerBlock = 1;
    public float exhaustionCostPerBlock = 0.005F;
    public boolean fastLeafDecayEnabled = true;
    public int leafDecaySpeedMultiplier = DEFAULT_LEAF_DECAY_SPEED_MULTIPLIER;
    public LinkedHashSet<String> whitelist = new LinkedHashSet<>();
    public LinkedHashSet<String> allowedTags = defaultAllowedTags();
    public LinkedHashSet<String> deniedBlocks = new LinkedHashSet<>();
    public LinkedHashSet<String> deniedTags = new LinkedHashSet<>();

    public static VeinMinerConfig createDefault() {
        return new VeinMinerConfig();
    }

    public List<String> normalize() {
        List<String> corrections = new ArrayList<>();
        maxOreBlocks = clamp("maxOreBlocks", maxOreBlocks, 1, MAX_BLOCKS_PER_VEIN, DEFAULT_MAX_ORE_BLOCKS, corrections);
        maxTreeBlocks = clamp("maxTreeBlocks", maxTreeBlocks, 1, MAX_BLOCKS_PER_VEIN, DEFAULT_MAX_TREE_BLOCKS, corrections);
        maxOtherBlocks = clamp("maxOtherBlocks", maxOtherBlocks, 1, MAX_BLOCKS_PER_VEIN, DEFAULT_MAX_OTHER_BLOCKS, corrections);
        leafDecaySpeedMultiplier = clamp(
                "leafDecaySpeedMultiplier",
                leafDecaySpeedMultiplier,
                1,
                MAX_LEAF_DECAY_SPEED_MULTIPLIER,
                DEFAULT_LEAF_DECAY_SPEED_MULTIPLIER,
                corrections
        );
        durabilityCostPerBlock = clamp(
                "durabilityCostPerBlock",
                durabilityCostPerBlock,
                0,
                MAX_DURABILITY_COST,
                1,
                corrections
        );
        minimumRemainingDurability = clamp(
                "minimumRemainingDurability",
                minimumRemainingDurability,
                0,
                MAX_BLOCKS_PER_VEIN,
                1,
                corrections
        );
        if (!Float.isFinite(exhaustionCostPerBlock)
                || exhaustionCostPerBlock < 0.0F
                || exhaustionCostPerBlock > MAX_EXHAUSTION_COST) {
            corrections.add("exhaustionCostPerBlock reset to 0.005 (allowed range: 0.0-" + MAX_EXHAUSTION_COST + ")");
            exhaustionCostPerBlock = 0.005F;
        }
        if (defaultActivationMode == null) {
            defaultActivationMode = ActivationMode.WHILE_SNEAKING;
            corrections.add("defaultActivationMode reset to while_sneaking");
        }
        if (adjacencyMode == null) {
            adjacencyMode = AdjacencyMode.FACES_EDGES_CORNERS;
            corrections.add("adjacencyMode reset to faces_edges_corners");
        }

        if (allowedTags == null) {
            allowedTags = defaultAllowedTags();
            corrections.add("allowedTags restored to defaults");
        }

        whitelist = normalizeIdentifiers("whitelist", whitelist, corrections);
        allowedTags = normalizeIdentifiers("allowedTags", allowedTags, corrections);
        deniedBlocks = normalizeIdentifiers("deniedBlocks", deniedBlocks, corrections);
        deniedTags = normalizeIdentifiers("deniedTags", deniedTags, corrections);
        return List.copyOf(corrections);
    }

    public boolean isBlockWhitelisted(BlockState state) {
        if (matchesId(state, deniedBlocks) || matchesAnyTag(state, deniedTags)) {
            return false;
        }
        return matchesId(state, whitelist) || matchesAnyTag(state, allowedTags);
    }

    public boolean addBlockToWhitelist(Block block) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        return blockId != BuiltInRegistries.BLOCK.getDefaultKey() && whitelist.add(blockId.toString());
    }

    public boolean removeBlockFromWhitelist(Block block) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        return blockId != BuiltInRegistries.BLOCK.getDefaultKey() && whitelist.remove(blockId.toString());
    }

    public List<String> whitelistAsSortedList() {
        return sorted(whitelist);
    }

    public BlockCategory category(BlockState state) {
        if (state.is(ORE_CATEGORY_TAG)) {
            return BlockCategory.ORE;
        }
        if (state.is(TREE_CATEGORY_TAG)) {
            return BlockCategory.TREE;
        }
        return BlockCategory.OTHER;
    }

    public int maxBlocks(BlockCategory category) {
        return switch (category) {
            case ORE -> maxOreBlocks;
            case TREE -> maxTreeBlocks;
            case OTHER -> maxOtherBlocks;
        };
    }

    public static TagKey<Block> parseBlockTag(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null ? null : TagKey.create(Registries.BLOCK, identifier);
    }

    public enum BlockCategory {
        ORE,
        TREE,
        OTHER
    }

    private static boolean matchesId(BlockState state, LinkedHashSet<String> ids) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return blockId != BuiltInRegistries.BLOCK.getDefaultKey() && ids.contains(blockId.toString());
    }

    private static boolean matchesAnyTag(BlockState state, LinkedHashSet<String> tags) {
        for (String tagId : tags) {
            TagKey<Block> tag = parseBlockTag(tagId);
            if (tag != null && state.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static LinkedHashSet<String> defaultAllowedTags() {
        LinkedHashSet<String> defaults = new LinkedHashSet<>();
        defaults.add(ORE_CATEGORY_TAG.location().toString());
        defaults.add(TREE_CATEGORY_TAG.location().toString());
        return defaults;
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VeinMiner.MOD_ID, path));
    }

    private static int clamp(
            String name,
            int value,
            int minimum,
            int maximum,
            int defaultValue,
            List<String> corrections
    ) {
        if (value >= minimum && value <= maximum) {
            return value;
        }
        corrections.add(name + " reset to " + defaultValue + " (allowed range: " + minimum + "-" + maximum + ")");
        return defaultValue;
    }

    private static LinkedHashSet<String> normalizeIdentifiers(
            String name,
            LinkedHashSet<String> values,
            List<String> corrections
    ) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                Identifier identifier = value == null ? null : Identifier.tryParse(value);
                if (identifier == null) {
                    corrections.add("Removed invalid identifier from " + name + ": " + value);
                } else if (normalized.size() < MAX_SELECTION_ENTRIES) {
                    normalized.add(identifier.toString());
                }
            }
            if (values.size() > MAX_SELECTION_ENTRIES) {
                corrections.add(name + " truncated to " + MAX_SELECTION_ENTRIES + " entries");
            }
        }
        return normalized;
    }

    private static List<String> sorted(LinkedHashSet<String> values) {
        List<String> sorted = new ArrayList<>(values);
        sorted.sort(String::compareTo);
        return sorted;
    }
}
