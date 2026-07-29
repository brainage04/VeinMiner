package io.github.brainage04.vein_miner.config;

import com.google.gson.annotations.SerializedName;

public enum AdjacencyMode {
    @SerializedName("faces")
    FACES("faces"),
    @SerializedName("faces_edges")
    FACES_EDGES("faces_edges"),
    @SerializedName("faces_edges_corners")
    FACES_EDGES_CORNERS("faces_edges_corners");

    private final String serializedName;

    AdjacencyMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public boolean includes(int dx, int dy, int dz) {
        int changedAxes = (dx == 0 ? 0 : 1) + (dy == 0 ? 0 : 1) + (dz == 0 ? 0 : 1);
        return switch (this) {
            case FACES -> changedAxes == 1;
            case FACES_EDGES -> changedAxes <= 2;
            case FACES_EDGES_CORNERS -> true;
        };
    }

    public String serializedName() {
        return serializedName;
    }
}
