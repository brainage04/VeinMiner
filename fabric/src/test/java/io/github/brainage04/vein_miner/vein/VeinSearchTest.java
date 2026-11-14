package io.github.brainage04.vein_miner.vein;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.brainage04.vein_miner.config.AdjacencyMode;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.junit.jupiter.api.Test;

class VeinSearchTest {
    /** Accepted blocks lie at most this many blocks from the origin on each axis. */
    private static final int REACH = 5;

    private static final VeinSearch.LoadedChunks ALL_LOADED = (chunkX, chunkZ) -> true;

    @Property
    void neverReturnsMoreThanTheCapAllows(
            @ForAll @IntRange(min = -2, max = 200) int maxBlocks,
            @ForAll AdjacencyMode adjacency,
            @ForAll long seed,
            @ForAll @IntRange(min = 0, max = 100) int density) {
        BlockPos origin = new BlockPos(0, 64, 0);
        LongArrayList vein =
                VeinSearch.connectedBlocks(
                        origin, maxBlocks, adjacency, ALL_LOADED, accepted(origin, seed, density));
        assertTrue(vein.size() <= Math.max(0, maxBlocks - 1), vein.size() + " for " + maxBlocks);
    }

    @Property
    void anOpenFieldFillsTheCapExactly(
            @ForAll @IntRange(min = 1, max = 600) int maxBlocks,
            @ForAll AdjacencyMode adjacency) {
        LongArrayList vein =
                VeinSearch.connectedBlocks(
                        new BlockPos(3, 70, -9), maxBlocks, adjacency, ALL_LOADED, pos -> true);
        assertEquals(maxBlocks - 1, vein.size());
    }

    @Property
    void neverOffersABlockOutsideALoadedArea(
            @ForAll @IntRange(min = -40, max = 40) int originX,
            @ForAll @IntRange(min = -40, max = 40) int originZ,
            @ForAll AdjacencyMode adjacency,
            @ForAll long seed,
            @ForAll @IntRange(min = 30, max = 100) int density,
            @ForAll long chunkSeed,
            @ForAll @IntRange(min = 50, max = 100) int loadedShare) {
        BlockPos origin = new BlockPos(originX, 64, originZ);
        VeinSearch.LoadedChunks chunks = loadedChunks(chunkSeed, loadedShare);
        Predicate<BlockPos> accepts = accepted(origin, seed, density);
        List<BlockPos> offered = new ArrayList<>();
        LongArrayList vein =
                VeinSearch.connectedBlocks(
                        origin,
                        10_000,
                        adjacency,
                        chunks,
                        pos -> {
                            offered.add(pos.immutable());
                            return accepts.test(pos);
                        });

        for (BlockPos pos : offered) {
            assertTrue(areaLoaded(chunks, pos), "offered " + pos + " outside a loaded area");
        }
        for (long packed : vein) {
            assertTrue(areaLoaded(chunks, BlockPos.of(packed)));
        }
    }

    @Property
    void offersEachBlockOnceAndNeverTheOrigin(
            @ForAll AdjacencyMode adjacency,
            @ForAll long seed,
            @ForAll @IntRange(min = 0, max = 100) int density,
            @ForAll @IntRange(min = 2, max = 400) int maxBlocks) {
        BlockPos origin = new BlockPos(-7, 12, 30);
        Predicate<BlockPos> accepts = accepted(origin, seed, density);
        LongOpenHashSet offered = new LongOpenHashSet();
        LongArrayList vein =
                VeinSearch.connectedBlocks(
                        origin,
                        maxBlocks,
                        adjacency,
                        ALL_LOADED,
                        pos -> {
                            assertTrue(offered.add(pos.asLong()), pos + " offered twice");
                            return accepts.test(pos);
                        });

        assertFalse(offered.contains(origin.asLong()));
        assertEquals(vein.size(), new LongOpenHashSet(vein).size(), "a block came back twice");
    }

    @Property
    void eachBlockTouchesTheOriginOrAnEarlierBlock(
            @ForAll AdjacencyMode adjacency,
            @ForAll long seed,
            @ForAll @IntRange(min = 20, max = 100) int density,
            @ForAll @IntRange(min = 2, max = 400) int maxBlocks) {
        BlockPos origin = new BlockPos(0, 0, 0);
        LongArrayList vein =
                VeinSearch.connectedBlocks(
                        origin, maxBlocks, adjacency, ALL_LOADED, accepted(origin, seed, density));

        List<BlockPos> reached = new ArrayList<>(List.of(origin));
        for (long packed : vein) {
            BlockPos pos = BlockPos.of(packed);
            assertTrue(
                    reached.stream().anyMatch(earlier -> adjacent(adjacency, earlier, pos)),
                    pos + " touches no earlier block");
            reached.add(pos);
        }
    }

    @Property
    void withRoomToSpareTheVeinIsTheWholeConnectedRegion(
            @ForAll @IntRange(min = -40, max = 40) int originX,
            @ForAll @IntRange(min = -40, max = 40) int originZ,
            @ForAll AdjacencyMode adjacency,
            @ForAll long seed,
            @ForAll @IntRange(min = 0, max = 100) int density,
            @ForAll long chunkSeed,
            @ForAll @IntRange(min = 50, max = 100) int loadedShare) {
        BlockPos origin = new BlockPos(originX, -20, originZ);
        VeinSearch.LoadedChunks chunks = loadedChunks(chunkSeed, loadedShare);
        Predicate<BlockPos> accepts = accepted(origin, seed, density);
        LongArrayList vein =
                VeinSearch.connectedBlocks(origin, 10_000, adjacency, chunks, accepts);

        assertEquals(
                floodFill(origin, adjacency, pos -> areaLoaded(chunks, pos) && accepts.test(pos)),
                Set.copyOf(vein));
    }

    @Test
    void nearerBlocksComeFirstAndTheCapCutsTheFarEnd() {
        BlockPos origin = new BlockPos(0, 64, 0);
        Predicate<BlockPos> line = pos -> pos.getY() == 64 && pos.getZ() == 0 && pos.getX() > 0;
        LongArrayList vein =
                VeinSearch.connectedBlocks(origin, 4, AdjacencyMode.FACES, ALL_LOADED, line);
        assertEquals(
                List.of(
                        new BlockPos(1, 64, 0).asLong(),
                        new BlockPos(2, 64, 0).asLong(),
                        new BlockPos(3, 64, 0).asLong()),
                vein);
    }

    @Test
    void aMissingDiagonalChunkStopsTheVeinAtTheBlocksBesideIt() {
        // The origin is the last block of chunk (0, 0) and the vein runs diagonally into chunk
        // (1, 1). Chunk (2, 2) is missing, so no block of chunk (1, 1) may be read.
        BlockPos origin = new BlockPos(15, 64, 15);
        VeinSearch.LoadedChunks chunks = (chunkX, chunkZ) -> !(chunkX == 2 && chunkZ == 2);
        Predicate<BlockPos> diagonal =
                pos -> pos.getY() == 64 && pos.getX() == pos.getZ() && pos.getX() >= 15;
        LongArrayList vein =
                VeinSearch.connectedBlocks(
                        origin, 64, AdjacencyMode.FACES_EDGES_CORNERS, chunks, diagonal);
        assertTrue(vein.isEmpty(), "found " + vein);
    }

    @Test
    void aCapOfOneOrLessFindsNothingAndAsksNothing() {
        for (int maxBlocks : new int[] {Integer.MIN_VALUE, -1, 0, 1}) {
            LongArrayList vein =
                    VeinSearch.connectedBlocks(
                            BlockPos.ZERO,
                            maxBlocks,
                            AdjacencyMode.FACES,
                            (chunkX, chunkZ) -> {
                                throw new AssertionError("asked for a chunk");
                            },
                            pos -> {
                                throw new AssertionError("asked for a block");
                            });
            assertTrue(vein.isEmpty());
        }
    }

    private static Predicate<BlockPos> accepted(BlockPos origin, long seed, int density) {
        return pos ->
                Math.abs(pos.getX() - origin.getX()) <= REACH
                        && Math.abs(pos.getY() - origin.getY()) <= REACH
                        && Math.abs(pos.getZ() - origin.getZ()) <= REACH
                        && Math.floorMod(hash(seed, pos.getX(), pos.getY(), pos.getZ()), 100)
                                < density;
    }

    private static VeinSearch.LoadedChunks loadedChunks(long seed, int loadedShare) {
        return (chunkX, chunkZ) -> Math.floorMod(hash(seed, chunkX, 0, chunkZ), 100) < loadedShare;
    }

    private static boolean areaLoaded(VeinSearch.LoadedChunks chunks, BlockPos pos) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!chunks.isLoaded(chunkX + dx, chunkZ + dz)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean adjacent(AdjacencyMode adjacency, BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        return Math.abs(dx) <= 1
                && Math.abs(dy) <= 1
                && Math.abs(dz) <= 1
                && !(dx == 0 && dy == 0 && dz == 0)
                && adjacency.includes(dx, dy, dz);
    }

    private static Set<Long> floodFill(
            BlockPos origin, AdjacencyMode adjacency, Predicate<BlockPos> joins) {
        LongOpenHashSet seen = new LongOpenHashSet();
        LongOpenHashSet region = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        seen.add(origin.asLong());
        queue.enqueue(origin.asLong());
        while (!queue.isEmpty()) {
            BlockPos current = BlockPos.of(queue.dequeueLong());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = current.offset(dx, dy, dz);
                        if (adjacent(adjacency, current, next)
                                && seen.add(next.asLong())
                                && joins.test(next)) {
                            region.add(next.asLong());
                            queue.enqueue(next.asLong());
                        }
                    }
                }
            }
        }
        return Set.copyOf(region);
    }

    private static long hash(long seed, int x, int y, int z) {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h = Long.rotateLeft(h, 31) * 0xBF58476D1CE4E5B9L;
        h ^= y * 0xC2B2AE3D27D4EB4FL;
        h = Long.rotateLeft(h, 29) * 0x94D049BB133111EBL;
        h ^= z * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return h;
    }
}
