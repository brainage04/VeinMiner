package io.github.brainage04.vein_miner.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.brainage04.brainagelib.storage.ConfigFile;
import io.github.brainage04.brainagelib.storage.JsonFile;
import io.github.brainage04.vein_miner.VeinMiner;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class VeinMinerConfigManager {
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    // The ids 1.0.1 wrote as its default whitelist: VeinMinerConfig.defaultWhitelistBlocks() at tag
    // v1.0.1 (src/main/java/io/github/brainage04/vein_miner/config/VeinMinerConfig.java:83-120).
    private static final Set<String> LEGACY_DEFAULT_WHITELIST =
            Set.of(
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
    private static ConfigFile<VeinMinerConfig> file;
    private static boolean rewriteLoadedDocument;

    private VeinMinerConfigManager() {}

    public static synchronized void initialize(Path configDirectory) {
        file =
                ConfigFile.builder(
                                configDirectory.resolve("vein_miner.json"),
                                GSON,
                                VeinMinerConfig.class,
                                VeinMinerConfig.createDefault())
                        .decoder(VeinMinerConfigManager::decode)
                        .description("Vein Miner config")
                        .logger(VeinMiner.LOGGER)
                        .build();
        reloadFromDisk();
    }

    public static synchronized VeinMinerConfig getConfig() {
        return file.value();
    }

    public static synchronized boolean saveToDisk() {
        return file.save();
    }

    public static synchronized boolean reloadFromDisk() {
        rewriteLoadedDocument = false;
        return switch (file.loadOrCreate()) {
            // loadOrCreate only logs a failed write of the default; the reload command reports it.
            case JsonFile.LoadResult.Missing<VeinMinerConfig> missing -> file.save();
            case JsonFile.LoadResult.Loaded<VeinMinerConfig> loaded -> {
                if (rewriteLoadedDocument) {
                    file.save();
                }
                yield true;
            }
            default -> false;
        };
    }

    private static VeinMinerConfig decode(JsonElement element) {
        if (!(element instanceof JsonObject document)) {
            throw new JsonParseException("Expected a JSON object");
        }
        VeinMinerConfig loadedConfig = GSON.fromJson(document, VeinMinerConfig.class);
        boolean migrated = migrateLegacyVeinSize(document, loadedConfig);
        migrated |= migrateLegacyWhitelist(document, loadedConfig);
        List<String> corrections = loadedConfig.normalize();
        for (String correction : corrections) {
            VeinMiner.LOGGER.warn("Vein Miner config correction: {}", correction);
        }
        rewriteLoadedDocument = migrated || !corrections.isEmpty();
        return loadedConfig;
    }

    static boolean migrateLegacyVeinSize(JsonObject document, VeinMinerConfig loadedConfig) {
        if (!document.has("veinSize") || hasCategoryLimits(document)) {
            return false;
        }

        int legacyLimit;
        try {
            legacyLimit = document.get("veinSize").getAsInt();
        } catch (NumberFormatException | UnsupportedOperationException exception) {
            VeinMiner.LOGGER.warn("Ignoring invalid legacy veinSize; using category defaults");
            return true;
        }
        int migratedLimit = Math.max(1, Math.min(VeinMinerConfig.MAX_BLOCKS_PER_VEIN, legacyLimit));
        loadedConfig.maxOreBlocks = migratedLimit;
        loadedConfig.maxTreeBlocks = migratedLimit;
        loadedConfig.maxOtherBlocks = migratedLimit;
        VeinMiner.LOGGER.info(
                "Migrated legacy veinSize={} to all three category limits", legacyLimit);
        return true;
    }

    /**
     * Up to 1.0.1 the whitelist was the complete list of blocks to vein mine, and the file had no
     * allowedTags. A whitelist other than 1.0.1's default stays the complete list; the default one
     * gets the default tags, which include all of its blocks. Every file 1.0.1 wrote has veinSize
     * (v1.0.1 VeinMinerConfig.java:15 is a primitive field that Gson always writes), so a file
     * without it was written by hand for a later version.
     */
    static boolean migrateLegacyWhitelist(JsonObject document, VeinMinerConfig loadedConfig) {
        if (!document.has("veinSize")
                || !document.has("whitelist")
                || document.has("allowedTags")
                || hasCategoryLimits(document)
                || LEGACY_DEFAULT_WHITELIST.equals(loadedConfig.whitelist)) {
            return false;
        }
        loadedConfig.allowedTags = new LinkedHashSet<>();
        VeinMiner.LOGGER.info(
                "Migrated a legacy whitelist: it stays the complete selection, with no tags");
        return true;
    }

    private static boolean hasCategoryLimits(JsonObject document) {
        return document.has("maxOreBlocks")
                || document.has("maxTreeBlocks")
                || document.has("maxOtherBlocks");
    }
}
