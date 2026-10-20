package io.github.brainage04.vein_miner.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.brainage04.brainagelib.storage.JsonFile;
import io.github.brainage04.vein_miner.VeinMiner;
import java.nio.file.Path;
import java.util.List;

/**
 * The server-wide {@code vein_miner.json}. A file that fails to load keeps the current config and is not
 * overwritten until a later reload succeeds; admin changes made meanwhile apply in memory only.
 */
public final class VeinMinerConfigManager {
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static JsonFile<VeinMinerConfig> file;
    private static VeinMinerConfig config = VeinMinerConfig.createDefault();
    /** Set by the decoder when the loaded document was migrated or corrected and should be written back. */
    private static boolean rewriteLoadedDocument;

    private VeinMinerConfigManager() {}

    public static synchronized void initialize(Path configDirectory) {
        file = JsonFile.builder(configDirectory.resolve("vein_miner.json"), GSON, VeinMinerConfig.class)
                .decoder(VeinMinerConfigManager::decode)
                .description("Vein Miner config")
                .logger(VeinMiner.LOGGER)
                .build();
        reloadFromDisk();
    }

    public static synchronized VeinMinerConfig getConfig() {
        return config;
    }

    public static synchronized boolean saveToDisk() {
        return file.save(config).saved();
    }

    /**
     * Loads the file. A missing file is recreated with defaults; one that cannot be loaded keeps the current
     * config and returns false.
     */
    public static synchronized boolean reloadFromDisk() {
        rewriteLoadedDocument = false;
        return switch (file.load()) {
            case JsonFile.LoadResult.Missing<VeinMinerConfig> missing -> {
                config = VeinMinerConfig.createDefault();
                yield saveToDisk();
            }
            case JsonFile.LoadResult.Loaded<VeinMinerConfig>(VeinMinerConfig loaded) -> {
                config = loaded;
                if (rewriteLoadedDocument) {
                    saveToDisk();
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
        List<String> corrections = loadedConfig.normalize();
        for (String correction : corrections) {
            VeinMiner.LOGGER.warn("Vein Miner config correction: {}", correction);
        }
        rewriteLoadedDocument = migrated || !corrections.isEmpty();
        return loadedConfig;
    }

    static boolean migrateLegacyVeinSize(JsonObject document, VeinMinerConfig loadedConfig) {
        if (!document.has("veinSize")
                || document.has("maxOreBlocks")
                || document.has("maxTreeBlocks")
                || document.has("maxOtherBlocks")) {
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
}
