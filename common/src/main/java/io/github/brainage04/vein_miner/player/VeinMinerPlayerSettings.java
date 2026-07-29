package io.github.brainage04.vein_miner.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.brainage04.vein_miner.VeinMiner;
import io.github.brainage04.vein_miner.config.ActivationMode;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VeinMinerPlayerSettings {
    public static final int MAX_PERSONAL_WHITELIST_SIZE = 64;
    private static final int SCHEMA_VERSION = 1;
    private static final String FILE_NAME = "vein-miner-players.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<UUID, PlayerPreference> PREFERENCES = new HashMap<>();
    private static Path statePath;

    private VeinMinerPlayerSettings() {
    }

    public static void shutdown(MinecraftServer server) {
        save();
        synchronized (VeinMinerPlayerSettings.class) {
            PREFERENCES.clear();
            statePath = null;
        }
    }

    public static synchronized boolean isEnabled(ServerPlayer player) {
        return preference(player).enabled;
    }

    public static synchronized ActivationMode activationMode(ServerPlayer player) {
        ActivationMode mode = preference(player).activationMode;
        return mode == null ? VeinMinerConfigManager.getConfig().defaultActivationMode : mode;
    }

    public static synchronized boolean shouldActivate(ServerPlayer player) {
        return isEnabled(player) && activationMode(player).isActive(player);
    }

    public static synchronized boolean isPersonalWhitelistEnabled(ServerPlayer player) {
        return preference(player).personalWhitelistEnabled;
    }

    public static synchronized boolean allowsBlock(ServerPlayer player, BlockState state) {
        PlayerPreference preference = preference(player);
        if (!preference.personalWhitelistEnabled) {
            return true;
        }
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return blockId != BuiltInRegistries.BLOCK.getDefaultKey()
                && preference.personalWhitelist.contains(blockId.toString());
    }

    public static synchronized boolean setEnabled(ServerPlayer player, boolean enabled) {
        mutablePreference(player).enabled = enabled;
        return save();
    }

    public static synchronized boolean setActivationMode(ServerPlayer player, ActivationMode mode) {
        mutablePreference(player).activationMode = mode;
        return save();
    }

    public static synchronized boolean setPersonalWhitelistEnabled(ServerPlayer player, boolean enabled) {
        mutablePreference(player).personalWhitelistEnabled = enabled;
        return save();
    }

    public static synchronized WhitelistAddResult addPersonalBlock(ServerPlayer player, Block block) {
        PlayerPreference preference = mutablePreference(player);
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (blockId == BuiltInRegistries.BLOCK.getDefaultKey()
                || !VeinMinerConfigManager.getConfig().isBlockWhitelisted(block.defaultBlockState())) {
            return WhitelistAddResult.NOT_GLOBALLY_ALLOWED;
        }
        if (preference.personalWhitelist.contains(blockId.toString())) {
            return WhitelistAddResult.ALREADY_PRESENT;
        }
        if (preference.personalWhitelist.size() >= MAX_PERSONAL_WHITELIST_SIZE) {
            return WhitelistAddResult.LIMIT_REACHED;
        }
        preference.personalWhitelist.add(blockId.toString());
        preference.personalWhitelistEnabled = true;
        save();
        return WhitelistAddResult.ADDED;
    }

    public static synchronized boolean removePersonalBlock(ServerPlayer player, Block block) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        boolean removed = blockId != BuiltInRegistries.BLOCK.getDefaultKey()
                && mutablePreference(player).personalWhitelist.remove(blockId.toString());
        if (removed) {
            save();
        }
        return removed;
    }

    public static synchronized boolean clearPersonalWhitelist(ServerPlayer player) {
        PlayerPreference preference = mutablePreference(player);
        preference.personalWhitelist.clear();
        preference.personalWhitelistEnabled = true;
        return save();
    }

    public static synchronized List<String> personalWhitelist(ServerPlayer player) {
        List<String> entries = new ArrayList<>(preference(player).personalWhitelist);
        entries.sort(String::compareTo);
        return List.copyOf(entries);
    }

    private static PlayerPreference preference(ServerPlayer player) {
        PlayerPreference preference = PREFERENCES.get(player.getUUID());
        return preference == null ? PlayerPreference.defaults() : preference;
    }

    private static PlayerPreference mutablePreference(ServerPlayer player) {
        return PREFERENCES.computeIfAbsent(player.getUUID(), uuid -> PlayerPreference.defaults());
    }

    public static synchronized void load(MinecraftServer server) {
        statePath = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
        PREFERENCES.clear();
        if (!Files.exists(statePath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(statePath)) {
            SavedState loaded = GSON.fromJson(reader, SavedState.class);
            if (loaded == null || loaded.schemaVersion != SCHEMA_VERSION || loaded.players == null) {
                throw new JsonParseException("Unsupported or incomplete player settings document");
            }
            for (Map.Entry<String, PlayerPreference> entry : loaded.players.entrySet()) {
                try {
                    PlayerPreference preference = entry.getValue();
                    if (preference == null) {
                        continue;
                    }
                    preference.normalize();
                    PREFERENCES.put(UUID.fromString(entry.getKey()), preference);
                } catch (IllegalArgumentException exception) {
                    VeinMiner.LOGGER.warn("Ignoring invalid player settings entry in {}: {}", statePath, entry.getKey());
                }
            }
        } catch (IOException | JsonParseException exception) {
            VeinMiner.LOGGER.error("Failed to load Vein Miner player settings from {}", statePath, exception);
        }
    }

    private static synchronized boolean save() {
        if (statePath == null) {
            return false;
        }
        Path temporaryPath = statePath.resolveSibling(statePath.getFileName() + ".tmp");
        Map<String, PlayerPreference> serialized = new HashMap<>();
        PREFERENCES.forEach((uuid, preference) -> serialized.put(uuid.toString(), preference));
        SavedState state = new SavedState(SCHEMA_VERSION, serialized);

        try {
            Files.createDirectories(statePath.getParent());
            try (Writer writer = Files.newBufferedWriter(temporaryPath)) {
                GSON.toJson(state, writer);
            }
            moveAtomically(temporaryPath, statePath);
            return true;
        } catch (IOException exception) {
            VeinMiner.LOGGER.error("Failed to save Vein Miner player settings to {}", statePath, exception);
            try {
                Files.deleteIfExists(temporaryPath);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            return false;
        }
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public enum WhitelistAddResult {
        ADDED,
        ALREADY_PRESENT,
        NOT_GLOBALLY_ALLOWED,
        LIMIT_REACHED
    }

    private static final class SavedState {
        private int schemaVersion;
        private Map<String, PlayerPreference> players;

        private SavedState(int schemaVersion, Map<String, PlayerPreference> players) {
            this.schemaVersion = schemaVersion;
            this.players = players;
        }
    }

    private static final class PlayerPreference {
        private boolean enabled = true;
        private ActivationMode activationMode;
        private boolean personalWhitelistEnabled;
        private LinkedHashSet<String> personalWhitelist = new LinkedHashSet<>();

        private static PlayerPreference defaults() {
            return new PlayerPreference();
        }

        private void normalize() {
            if (personalWhitelist == null) {
                personalWhitelist = new LinkedHashSet<>();
            }
            personalWhitelist.removeIf(id -> id == null || Identifier.tryParse(id) == null);
            while (personalWhitelist.size() > MAX_PERSONAL_WHITELIST_SIZE) {
                personalWhitelist.remove(personalWhitelist.getLast());
            }
        }
    }
}
