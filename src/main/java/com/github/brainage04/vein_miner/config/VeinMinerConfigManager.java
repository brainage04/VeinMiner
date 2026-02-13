package com.github.brainage04.vein_miner.config;

import com.github.brainage04.vein_miner.VeinMiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VeinMinerConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "config/vein_miner.json";

    private static VeinMinerConfig config = VeinMinerConfig.createDefault();
    private static Path configPath;

    private VeinMinerConfigManager() {
    }

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(VeinMinerConfigManager::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            config = VeinMinerConfig.createDefault();
            configPath = null;
        });
    }

    public static VeinMinerConfig getConfig() {
        return config;
    }

    public static void saveToDisk(MinecraftServer server) {
        ensureConfigPath(server);

        try {
            Files.createDirectories(configPath.getParent());

            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            VeinMiner.LOGGER.error("Failed to save VeinMiner config to {}", configPath, exception);
        }
    }

    public static boolean reloadFromDisk(MinecraftServer server) {
        ensureConfigPath(server);

        if (Files.notExists(configPath)) {
            saveToDisk(server);
            return true;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            VeinMinerConfig loadedConfig = GSON.fromJson(reader, VeinMinerConfig.class);

            if (loadedConfig == null) {
                VeinMiner.LOGGER.warn("VeinMiner config file was empty; preserving current config.");
                return false;
            }

            loadedConfig.normalize();
            config = loadedConfig;
            return true;
        } catch (IOException | JsonParseException exception) {
            VeinMiner.LOGGER.error("Failed to load VeinMiner config from {}", configPath, exception);
            return false;
        }
    }

    private static void onServerStarted(MinecraftServer server) {
        ensureConfigPath(server);

        if (!reloadFromDisk(server)) {
            saveToDisk(server);
        }
    }

    private static void ensureConfigPath(MinecraftServer server) {
        if (configPath == null) {
            configPath = server.getFile(CONFIG_FILE);
        }
    }
}
