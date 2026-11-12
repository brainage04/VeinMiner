package io.github.brainage04.vein_miner.vein;

import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfig.BlockCategory;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import io.github.brainage04.vein_miner.player.VeinMinerPlayerSettings;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class VeinMiningHandler {
    private static final ThreadLocal<MiningContext> MINING_CONTEXT = new ThreadLocal<>();
    private static PendingVein pendingVein;
    private static boolean miningVein;

    private VeinMiningHandler() {}

    public static boolean isMiningAdditionalBlock() {
        return MINING_CONTEXT.get() != null;
    }

    public static int additionalBlockDurabilityCost() {
        MiningContext context = MINING_CONTEXT.get();
        return context == null ? 0 : context.durabilityCost;
    }

    public static float additionalBlockExhaustionCost() {
        MiningContext context = MINING_CONTEXT.get();
        return context == null ? 0.0F : context.exhaustionCost;
    }

    public static boolean beforeBlockBreak(
            Level world,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        pendingVein = null;
        if (miningVein
                || !(world instanceof ServerLevel level)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }

        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ItemStack tool = serverPlayer.getMainHandItem();
        if (!config.enableVeinMining
                || !VeinMinerPlayerSettings.shouldActivate(serverPlayer)
                || !config.isBlockWhitelisted(state)
                || !VeinMinerPlayerSettings.allowsBlock(serverPlayer, state)
                || state.getBlock() instanceof LeavesBlock
                || !tool.isCorrectToolForDrops(state)) {
            return true;
        }

        pendingVein = new PendingVein(level, serverPlayer, pos.immutable(), state);
        return true;
    }

    public static void completeBlockBreak(ServerPlayer player, BlockPos pos, boolean destroyed) {
        if (miningVein) {
            return;
        }

        PendingVein pending = pendingVein;
        pendingVein = null;
        if (!destroyed
                || pending == null
                || pending.player != player
                || !pending.originPos.equals(pos)) {
            return;
        }

        mineConnectedBlocks(pending);
    }

    private static void mineConnectedBlocks(PendingVein pending) {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        VeinMinerConfig.Selection selection = config.selection();
        BlockCategory category = config.category(pending.originState);
        int maxBlocks = config.maxBlocks(category);
        if (maxBlocks <= 1) {
            return;
        }

        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        LongOpenHashSet visited =
                new LongOpenHashSet(
                        Math.min(maxBlocks * 2, VeinMinerConfig.MAX_BLOCKS_PER_VEIN * 2));
        LongArrayList connected = new LongArrayList(Math.min(maxBlocks - 1, 128));
        Long2BooleanOpenHashMap loadedAreas = new Long2BooleanOpenHashMap();
        long origin = pending.originPos.asLong();
        queue.enqueue(origin);
        visited.add(origin);

        while (!queue.isEmpty() && connected.size() + 1 < maxBlocks) {
            BlockPos current = BlockPos.of(queue.dequeueLong());
            for (int dx = -1; dx <= 1 && connected.size() + 1 < maxBlocks; dx++) {
                for (int dy = -1; dy <= 1 && connected.size() + 1 < maxBlocks; dy++) {
                    for (int dz = -1; dz <= 1 && connected.size() + 1 < maxBlocks; dz++) {
                        if ((dx == 0 && dy == 0 && dz == 0)
                                || !config.adjacencyMode.includes(dx, dy, dz)) {
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
                        if (!isAreaLoaded(pending.level, neighborPos, loadedAreas)
                                || !pending.player.mayInteract(pending.level, neighborPos)) {
                            continue;
                        }
                        BlockState neighborState = pending.level.getBlockState(neighborPos);
                        if (!selection.allows(neighborState)
                                || !VeinMinerPlayerSettings.allowsBlock(
                                        pending.player, neighborState)
                                || !isEquivalentTarget(
                                        pending.originState, neighborState, category, config)) {
                            continue;
                        }
                        queue.enqueue(neighbor);
                        connected.add(neighbor);
                    }
                }
            }
        }

        miningVein = true;
        MiningContext context =
                new MiningContext(config.durabilityCostPerBlock, config.exhaustionCostPerBlock);
        try {
            for (long packedPos : connected) {
                BlockPos targetPos = BlockPos.of(packedPos);
                // Another mod's break listener may move the world border or change the ops
                // while earlier blocks of the vein break.
                if (!pending.player.mayInteract(pending.level, targetPos)) {
                    continue;
                }
                BlockState targetState = pending.level.getBlockState(targetPos);
                ItemStack currentTool = pending.player.getMainHandItem();
                if (!selection.allows(targetState)
                        || !VeinMinerPlayerSettings.allowsBlock(pending.player, targetState)
                        || !isEquivalentTarget(pending.originState, targetState, category, config)
                        || currentTool.isEmpty()
                        || !currentTool.isCorrectToolForDrops(targetState)
                        || !canAffordAdditionalBlock(currentTool, config)) {
                    break;
                }

                MINING_CONTEXT.set(context);
                try {
                    pending.player.gameMode.destroyBlock(targetPos);
                } finally {
                    MINING_CONTEXT.remove();
                }
            }
        } finally {
            MINING_CONTEXT.remove();
            miningVein = false;
        }
    }

    /**
     * Whether the chunk holding {@code pos} and the eight chunks around it are all loaded as full
     * chunks right now: {@code getChunkNow} returns a chunk only when it is present at FULL and its
     * ticket still allows FULL (ServerChunkCache.java:175-203, GenerationChunkHolder.java:260-262).
     * A chunk with a ticket that is still loading or generating does not count. Reading a block in
     * a chunk that is not loaded blocks the server thread until the chunk is loaded or generated
     * (Level.java:353-360, ServerChunkCache.java:162-163). Breaking a block reads its six
     * neighbours (Level.java:245,254, NeighborUpdater.java:45), which lie in these nine chunks. A
     * neighbour that changes shape reads its own neighbours and updates them in turn
     * (RedstoneWireBlock.java:181-185, NeighborUpdater.java:48-49), so those further reads have no
     * fixed reach and are not covered.
     */
    private static boolean isAreaLoaded(
            ServerLevel level, BlockPos pos, Long2BooleanOpenHashMap cache) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        long key = ChunkPos.pack(chunkX, chunkZ);
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        ServerChunkCache chunks = level.getChunkSource();
        boolean loaded = true;
        for (int dx = -1; dx <= 1 && loaded; dx++) {
            for (int dz = -1; dz <= 1 && loaded; dz++) {
                loaded = chunks.getChunkNow(chunkX + dx, chunkZ + dz) != null;
            }
        }
        cache.put(key, loaded);
        return loaded;
    }

    private static boolean canAffordAdditionalBlock(ItemStack tool, VeinMinerConfig config) {
        if (!config.stopBeforeBreakingTool
                || !tool.isDamageableItem()
                || config.durabilityCostPerBlock == 0) {
            return true;
        }
        int remainingDurability = tool.getMaxDamage() - tool.getDamageValue();
        return remainingDurability - config.durabilityCostPerBlock
                >= config.minimumRemainingDurability;
    }

    private static boolean isEquivalentTarget(
            BlockState origin,
            BlockState candidate,
            BlockCategory category,
            VeinMinerConfig config) {
        if (config.category(candidate) != category) {
            return false;
        }
        if (origin.getBlock() == candidate.getBlock()) {
            return true;
        }
        return switch (category) {
            case ORE -> config.betterOreVeinMining && sameOreFamily(origin, candidate);
            case TREE -> config.betterTreeVeinMining && sameWoodFamily(origin, candidate);
            case OTHER -> false;
        };
    }

    private static boolean sameOreFamily(BlockState first, BlockState second) {
        Identifier firstId = blockId(first);
        Identifier secondId = blockId(second);
        return firstId.getNamespace().equals(secondId.getNamespace())
                && stripPrefix(firstId.getPath(), "deepslate_")
                        .equals(stripPrefix(secondId.getPath(), "deepslate_"));
    }

    private static boolean sameWoodFamily(BlockState first, BlockState second) {
        Identifier firstId = blockId(first);
        Identifier secondId = blockId(second);
        return firstId.getNamespace().equals(secondId.getNamespace())
                && woodFamily(firstId.getPath()).equals(woodFamily(secondId.getPath()));
    }

    private static String woodFamily(String path) {
        String normalized = stripPrefix(path, "stripped_");
        for (String suffix : new String[] {"_log", "_wood", "_stem", "_hyphae"}) {
            if (normalized.endsWith(suffix)) {
                return normalized.substring(0, normalized.length() - suffix.length());
            }
        }
        return normalized;
    }

    private static String stripPrefix(String value, String prefix) {
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static Identifier blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock());
    }

    private record PendingVein(
            ServerLevel level, ServerPlayer player, BlockPos originPos, BlockState originState) {}

    private record MiningContext(int durabilityCost, float exhaustionCost) {}
}
