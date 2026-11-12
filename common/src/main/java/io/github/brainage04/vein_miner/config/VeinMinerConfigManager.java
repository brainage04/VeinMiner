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
import java.util.List;

public final class VeinMinerConfigManager {
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
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
