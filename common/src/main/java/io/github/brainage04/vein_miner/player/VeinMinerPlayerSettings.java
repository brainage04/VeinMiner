package io.github.brainage04.vein_miner.player;

import io.github.brainage04.brainagelib.storage.WorldPlayerStore;
import io.github.brainage04.vein_miner.VeinMiner;
import io.github.brainage04.vein_miner.config.ActivationMode;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Each player's Vein Miner preferences, stored per world in {@code vein-miner-players.json}. A file that fails to
 * load (malformed or a newer schema) is kept unchanged; changes then apply for the session only.
 */
public final class VeinMinerPlayerSettings {
    public static final int MAX_PERSONAL_WHITELIST_SIZE = 64;
    private static final WorldPlayerStore<PlayerPreference> STORE = WorldPlayerStore
            .builder("vein-miner-players.json", PlayerPreference.class, PlayerPreference::new)
            .normalizer(PlayerPreference::normalize)
            .description("Vein Miner player settings")
            .logger(VeinMiner.LOGGER)
            .build();

    private VeinMinerPlayerSettings() {}

    public static synchronized void load(MinecraftServer server) {
        STORE.load(server);
    }

    public static synchronized void shutdown(MinecraftServer server) {
        STORE.close();
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

    public static synchronized boolean setPersonalWhitelistEnabled(
            ServerPlayer player, boolean enabled) {
        mutablePreference(player).personalWhitelistEnabled = enabled;
        return save();
    }

    public static synchronized WhitelistAddResult addPersonalBlock(
            ServerPlayer player, Block block) {
        PlayerPreference preference = mutablePreference(player);
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (blockId == BuiltInRegistries.BLOCK.getDefaultKey()
                || !VeinMinerConfigManager.getConfig()
                        .isBlockWhitelisted(block.defaultBlockState())) {
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
        return save() ? WhitelistAddResult.ADDED : WhitelistAddResult.ADDED_NOT_SAVED;
    }

    public static synchronized WhitelistRemoveResult removePersonalBlock(ServerPlayer player, Block block) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        boolean removed =
                blockId != BuiltInRegistries.BLOCK.getDefaultKey()
                        && mutablePreference(player).personalWhitelist.remove(blockId.toString());
        if (!removed) {
            return WhitelistRemoveResult.NOT_PRESENT;
        }
        return save() ? WhitelistRemoveResult.REMOVED : WhitelistRemoveResult.REMOVED_NOT_SAVED;
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
        return STORE.get(player.getUUID());
    }

    private static PlayerPreference mutablePreference(ServerPlayer player) {
        return STORE.getOrCreate(player.getUUID());
    }

    private static boolean save() {
        return STORE.save().saved();
    }

    public enum WhitelistAddResult {
        ADDED,
        ADDED_NOT_SAVED,
        ALREADY_PRESENT,
        NOT_GLOBALLY_ALLOWED,
        LIMIT_REACHED
    }

    public enum WhitelistRemoveResult {
        REMOVED,
        REMOVED_NOT_SAVED,
        NOT_PRESENT
    }

    private static final class PlayerPreference {
        private boolean enabled = true;
        private ActivationMode activationMode;
        private boolean personalWhitelistEnabled;
        private LinkedHashSet<String> personalWhitelist = new LinkedHashSet<>();

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
