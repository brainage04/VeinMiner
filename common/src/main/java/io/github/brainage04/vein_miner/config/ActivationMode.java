package io.github.brainage04.vein_miner.config;

import com.google.gson.annotations.SerializedName;
import net.minecraft.server.level.ServerPlayer;

public enum ActivationMode {
    @SerializedName("while_sneaking")
    WHILE_SNEAKING("while_sneaking") {
        @Override
        public boolean isActive(ServerPlayer player) {
            return player.isShiftKeyDown();
        }
    },
    @SerializedName("while_not_sneaking")
    WHILE_NOT_SNEAKING("while_not_sneaking") {
        @Override
        public boolean isActive(ServerPlayer player) {
            return !player.isShiftKeyDown();
        }
    },
    @SerializedName("always")
    ALWAYS("always") {
        @Override
        public boolean isActive(ServerPlayer player) {
            return true;
        }
    },
    @SerializedName("never")
    NEVER("never") {
        @Override
        public boolean isActive(ServerPlayer player) {
            return false;
        }
    };

    private final String serializedName;

    ActivationMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public abstract boolean isActive(ServerPlayer player);

    public String serializedName() {
        return serializedName;
    }
}
