package com.github.brainage04.vein_miner.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class VeinMinerConfig {
    public static final int DEFAULT_VEIN_SIZE = 64;

    public boolean enableVeinMining;
    public int veinSize;
    public boolean betterOreVeinMining;
    public boolean betterTreeVeinMining;
    public LinkedHashSet<String> whitelist;

    public VeinMinerConfig() {
        this.enableVeinMining = true;
        this.veinSize = DEFAULT_VEIN_SIZE;
        this.betterOreVeinMining = true;
        this.betterTreeVeinMining = true;
        this.whitelist = defaultWhitelist();
    }

    public static VeinMinerConfig createDefault() {
        return new VeinMinerConfig();
    }

    public void normalize() {
        if (this.veinSize < 1) {
            this.veinSize = DEFAULT_VEIN_SIZE;
        }

        if (this.whitelist == null) {
            this.whitelist = new LinkedHashSet<>();
        }

        this.whitelist.removeIf(id -> id == null || Identifier.tryParse(id) == null);
    }

    public boolean isBlockWhitelisted(Block block) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        return blockId != null && this.whitelist.contains(blockId.toString());
    }

    public boolean addBlockToWhitelist(Block block) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        return blockId != null && this.whitelist.add(blockId.toString());
    }

    public boolean removeBlockFromWhitelist(Block block) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        return blockId != null && this.whitelist.remove(blockId.toString());
    }

    public List<String> whitelistAsSortedList() {
        List<String> values = new ArrayList<>(this.whitelist);
        values.sort(String::compareTo);
        return values;
    }

    private static LinkedHashSet<String> defaultWhitelist() {
        LinkedHashSet<String> defaults = new LinkedHashSet<>();

        for (Block block : defaultWhitelistBlocks()) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            if (blockId != null) {
                defaults.add(blockId.toString());
            }
        }

        return defaults;
    }

    private static Set<Block> defaultWhitelistBlocks() {
        return new LinkedHashSet<>(Arrays.asList(
                Blocks.COAL_ORE,
                Blocks.IRON_ORE,
                Blocks.GOLD_ORE,
                Blocks.COPPER_ORE,
                Blocks.DIAMOND_ORE,
                Blocks.EMERALD_ORE,
                Blocks.LAPIS_ORE,
                Blocks.REDSTONE_ORE,
                Blocks.NETHER_QUARTZ_ORE,
                Blocks.NETHER_GOLD_ORE,
                Blocks.ANCIENT_DEBRIS,
                Blocks.DEEPSLATE_COAL_ORE,
                Blocks.DEEPSLATE_IRON_ORE,
                Blocks.DEEPSLATE_GOLD_ORE,
                Blocks.DEEPSLATE_COPPER_ORE,
                Blocks.DEEPSLATE_DIAMOND_ORE,
                Blocks.DEEPSLATE_EMERALD_ORE,
                Blocks.DEEPSLATE_LAPIS_ORE,
                Blocks.DEEPSLATE_REDSTONE_ORE,
                Blocks.OAK_LOG,
                Blocks.SPRUCE_LOG,
                Blocks.BIRCH_LOG,
                Blocks.JUNGLE_LOG,
                Blocks.ACACIA_LOG,
                Blocks.DARK_OAK_LOG,
                Blocks.PALE_OAK_LOG,
                Blocks.MANGROVE_LOG,
                Blocks.CHERRY_LOG,
                Blocks.CRIMSON_STEM,
                Blocks.WARPED_STEM,
                Blocks.OAK_LEAVES,
                Blocks.SPRUCE_LEAVES,
                Blocks.BIRCH_LEAVES,
                Blocks.JUNGLE_LEAVES,
                Blocks.ACACIA_LEAVES,
                Blocks.DARK_OAK_LEAVES,
                Blocks.PALE_OAK_LEAVES,
                Blocks.MANGROVE_LEAVES,
                Blocks.CHERRY_LEAVES,
                Blocks.AZALEA_LEAVES,
                Blocks.FLOWERING_AZALEA_LEAVES,
                Blocks.NETHER_WART_BLOCK,
                Blocks.WARPED_WART_BLOCK
        ));
    }
}
