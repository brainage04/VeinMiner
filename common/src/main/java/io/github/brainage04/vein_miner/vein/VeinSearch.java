package io.github.brainage04.vein_miner.vein;

import io.github.brainage04.vein_miner.config.AdjacencyMode;
import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

/** The search for a vein's other blocks, apart from the level so that unit tests can reach it. */
public final class VeinSearch {
    private VeinSearch() {}

    /** Whether the chunk at the given chunk coordinates is loaded as a full chunk right now. */
    @FunctionalInterface
    public interface LoadedChunks {
        boolean isLoaded(int chunkX, int chunkZ);
    }

    /**
     * The blocks joined to {@code origin} through blocks that {@code accepts}, nearest steps first,
     * as packed positions. The origin counts towards {@code maxBlocks} and is not in the list, so
     * the list holds at most {@code maxBlocks - 1} blocks. A block is offered to {@code accepts} at
     * most once, and only when the chunk holding it and the eight chunks around it are loaded.
     */
    public static LongArrayList connectedBlocks(
            BlockPos origin,
            int maxBlocks,
            AdjacencyMode adjacency,
            LoadedChunks chunks,
            Predicate<BlockPos> accepts) {
        if (maxBlocks <= 1) {
            return new LongArrayList();
        }

        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        LongOpenHashSet visited =
                new LongOpenHashSet(
                        Math.min(maxBlocks * 2, VeinMinerConfig.MAX_BLOCKS_PER_VEIN * 2));
        LongArrayList connected = new LongArrayList(Math.min(maxBlocks - 1, 128));
        Long2BooleanOpenHashMap loadedAreas = new Long2BooleanOpenHashMap();
        long start = origin.asLong();
        queue.enqueue(start);
        visited.add(start);

        while (!queue.isEmpty() && connected.size() + 1 < maxBlocks) {
            BlockPos current = BlockPos.of(queue.dequeueLong());
            for (int dx = -1; dx <= 1 && connected.size() + 1 < maxBlocks; dx++) {
                for (int dy = -1; dy <= 1 && connected.size() + 1 < maxBlocks; dy++) {
                    for (int dz = -1; dz <= 1 && connected.size() + 1 < maxBlocks; dz++) {
                        if ((dx == 0 && dy == 0 && dz == 0) || !adjacency.includes(dx, dy, dz)) {
                            continue;
                        }

                        long neighbor =
                                BlockPos.asLong(
                                        current.getX() + dx,
                                        current.getY() + dy,
                                        current.getZ() + dz);
                        if (!visited.add(neighbor)) {
                            continue;
                        }

                        BlockPos neighborPos = BlockPos.of(neighbor);
                        if (!isAreaLoaded(chunks, neighborPos, loadedAreas)
                                || !accepts.test(neighborPos)) {
                            continue;
                        }
                        queue.enqueue(neighbor);
                        connected.add(neighbor);
                    }
                }
            }
        }
        return connected;
    }

    /**
     * Whether the chunk holding {@code pos} and the eight chunks around it are all loaded. Reading
     * a block in a chunk that is not loaded blocks the server thread until the chunk is loaded or
     * generated (Level.java:353-360, ServerChunkCache.java:162-163). Breaking a block reads its six
     * neighbours (Level.java:245,254, NeighborUpdater.java:45), which lie in these nine chunks. A
     * neighbour that changes shape reads its own neighbours and updates them in turn
     * (RedstoneWireBlock.java:181-185, NeighborUpdater.java:48-49), so those further reads have no
     * fixed reach and are not covered.
     */
    private static boolean isAreaLoaded(
            LoadedChunks chunks, BlockPos pos, Long2BooleanOpenHashMap cache) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        long key = ChunkPos.pack(chunkX, chunkZ);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        boolean loaded = true;
        for (int dx = -1; dx <= 1 && loaded; dx++) {
            for (int dz = -1; dz <= 1 && loaded; dz++) {
                loaded = chunks.isLoaded(chunkX + dx, chunkZ + dz);
            }
        }
        cache.put(key, loaded);
        return loaded;
    }
}
