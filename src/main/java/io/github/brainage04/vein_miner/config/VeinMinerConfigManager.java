package io.github.brainage04.vein_miner.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.brainage04.vein_miner.VeinMiner;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class VeinMinerConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("vein_miner.json");
    private static VeinMinerConfig config = VeinMinerConfig.createDefault();

    private VeinMinerConfigManager() {
    }

    public static synchronized void initialize() {
        if (!Files.exists(CONFIG_PATH)) {
            saveToDisk();
            return;
        }
        reloadFromDisk();
    }

    public static synchronized VeinMinerConfig getConfig() {
        return config;
    }

    public static synchronized boolean saveToDisk() {
        Path temporaryPath = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(temporaryPath)) {
                GSON.toJson(config, writer);
            }
            moveAtomically(temporaryPath, CONFIG_PATH);
            return true;
        } catch (IOException exception) {
            VeinMiner.LOGGER.error("Failed to save Vein Miner config to {}", CONFIG_PATH, exception);
            try {
                Files.deleteIfExists(temporaryPath);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            return false;
        }
    }

    public static synchronized boolean reloadFromDisk() {
        if (!Files.exists(CONFIG_PATH)) {
            config = VeinMinerConfig.createDefault();
            return saveToDisk();
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject document = JsonParser.parseReader(reader).getAsJsonObject();
            VeinMinerConfig loadedConfig = GSON.fromJson(document, VeinMinerConfig.class);
            if (loadedConfig == null) {
                throw new JsonParseException("Configuration document is empty");
            }

            boolean migrated = migrateLegacyVeinSize(document, loadedConfig);
            List<String> corrections = loadedConfig.normalize();
            for (String correction : corrections) {
                VeinMiner.LOGGER.warn("Vein Miner config correction: {}", correction);
            }
            config = loadedConfig;
            if (migrated || !corrections.isEmpty()) {
                saveToDisk();
            }
            return true;
        } catch (IOException | IllegalStateException | JsonParseException exception) {
            VeinMiner.LOGGER.error("Failed to load Vein Miner config from {}; preserving current config", CONFIG_PATH, exception);
            return false;
        }
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
        VeinMiner.LOGGER.info("Migrated legacy veinSize={} to all three category limits", legacyLimit);
        return true;
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
