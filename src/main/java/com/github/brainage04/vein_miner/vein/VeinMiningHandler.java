package com.github.brainage04.vein_miner.vein;

import com.github.brainage04.vein_miner.config.VeinMinerConfig;
import com.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class VeinMiningHandler {
    private static final int INITIAL_DROPS_MAX_AGE_TICKS = 1;
    private static final int DEFERRED_DROPS_MAX_AGE_TICKS = 20;
    private static final double DEFERRED_MERGE_RADIUS = 2.0D;

    private static final Set<UUID> activelyVeinMiningPlayers = new HashSet<>();
    private static final List<PendingOriginMerge> pendingOriginMerges = new ArrayList<>();

    private VeinMiningHandler() {
    }

    public static void initialize() {
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
                return;
            }

            onBlockBroken(serverLevel, serverPlayer, pos, state);
        });
        ServerTickEvents.END_SERVER_TICK.register(VeinMiningHandler::tick);
    }

    private static void onBlockBroken(ServerLevel level, ServerPlayer player, BlockPos originPos, BlockState originState) {
        UUID playerId = player.getUUID();
        if (activelyVeinMiningPlayers.contains(playerId)) {
            return;
        }

        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        if (!config.enableVeinMining) {
            return;
        }
        if (!player.isShiftKeyDown()) {
            return;
        }
        if (!player.hasCorrectToolForDrops(originState)) {
            return;
        }
        if (!config.isBlockWhitelisted(originState.getBlock())) {
            return;
        }

        int maxAdditionalBlocks = config.veinSize - 1;
        if (maxAdditionalBlocks <= 0) {
            return;
        }

        LongArrayList blocksToBreak = collectConnectedVein(level, originPos, originState, config, maxAdditionalBlocks);

        if (blocksToBreak.isEmpty()) {
            return;
        }

        breakBlocksAndConsolidateDrops(level, player, originPos, blocksToBreak);
    }

    private static LongArrayList collectConnectedVein(
            ServerLevel level,
            BlockPos originPos,
            BlockState originState,
            VeinMinerConfig config,
            int maxAdditionalBlocks
    ) {
        LongArrayList result = new LongArrayList(Math.min(maxAdditionalBlocks, 256));
        LongOpenHashSet visited = new LongOpenHashSet(Math.max(maxAdditionalBlocks * 4, 64));
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        long origin = originPos.asLong();
        visited.add(origin);
        queue.enqueue(origin);

        while (!queue.isEmpty() && result.size() < maxAdditionalBlocks) {
            long current = queue.dequeueLong();
            int x = BlockPos.getX(current);
            int y = BlockPos.getY(current);
            int z = BlockPos.getZ(current);

            scanNeighbors:
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }

                        int neighborX = x + dx;
                        int neighborY = y + dy;
                        int neighborZ = z + dz;
                        long neighbor = BlockPos.asLong(neighborX, neighborY, neighborZ);
                        if (!visited.add(neighbor)) {
                            continue;
                        }

                        mutablePos.set(neighborX, neighborY, neighborZ);
                        BlockState neighborState = level.getBlockState(mutablePos);
                        if (neighborState.isAir()) {
                            continue;
                        }
                        if (!config.isBlockWhitelisted(neighborState.getBlock())) {
                            continue;
                        }
                        if (!isSameVeinTarget(originState, neighborState, config.betterOreVeinMining)) {
                            continue;
                        }

                        result.add(neighbor);
                        queue.enqueue(neighbor);

                        if (result.size() >= maxAdditionalBlocks) {
                            break scanNeighbors;
                        }
                    }
                }
            }
        }

        return result;
    }

    private static void breakBlocksAndConsolidateDrops(
            ServerLevel level,
            ServerPlayer player,
            BlockPos originPos,
            LongArrayList blocksToBreak
    ) {
        UUID playerId = player.getUUID();
        activelyVeinMiningPlayers.add(playerId);

        int minX = originPos.getX();
        int minY = originPos.getY();
        int minZ = originPos.getZ();
        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;

        try {
            for (long blockToBreak : blocksToBreak) {
                BlockPos targetPos = BlockPos.of(blockToBreak);
                minX = Math.min(minX, targetPos.getX());
                minY = Math.min(minY, targetPos.getY());
                minZ = Math.min(minZ, targetPos.getZ());
                maxX = Math.max(maxX, targetPos.getX());
                maxY = Math.max(maxY, targetPos.getY());
                maxZ = Math.max(maxZ, targetPos.getZ());

                BlockState targetState = level.getBlockState(targetPos);
                if (targetState.isAir()) {
                    continue;
                }
                if (!player.hasCorrectToolForDrops(targetState)) {
                    continue;
                }

                player.gameMode.destroyBlock(targetPos);
            }
        } finally {
            activelyVeinMiningPlayers.remove(playerId);
        }

        AABB searchBounds = new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D).inflate(2.5D);
        List<ItemEntity> droppedItems = level.getEntitiesOfClass(
                ItemEntity.class,
                searchBounds,
                item -> item.isAlive() && item.getAge() <= INITIAL_DROPS_MAX_AGE_TICKS
        );
        if (droppedItems.isEmpty()) {
            queueDeferredMerge(level, originPos);
            return;
        }

        double originX = originPos.getX() + 0.5D;
        double originY = originPos.getY() + 0.5D;
        double originZ = originPos.getZ() + 0.5D;
        for (ItemEntity droppedItem : droppedItems) {
            droppedItem.setPos(originX, originY, originZ);
            droppedItem.setDeltaMovement(0.0D, 0.0D, 0.0D);
        }

        mergeItemsInPlace(droppedItems);
        queueDeferredMerge(level, originPos);
    }

    private static void tick(MinecraftServer server) {
        if (pendingOriginMerges.isEmpty()) {
            return;
        }

        int currentTick = server.getTickCount();
        Iterator<PendingOriginMerge> iterator = pendingOriginMerges.iterator();
        while (iterator.hasNext()) {
            PendingOriginMerge pendingMerge = iterator.next();
            if (pendingMerge.executeAtTick > currentTick) {
                continue;
            }

            ServerLevel level = server.getLevel(pendingMerge.dimension);
            if (level != null) {
                runDeferredOriginMerge(level, pendingMerge.originPos);
            }

            iterator.remove();
        }
    }

    private static void queueDeferredMerge(ServerLevel level, BlockPos originPos) {
        pendingOriginMerges.add(new PendingOriginMerge(level.dimension(), originPos.immutable(), level.getServer().getTickCount()));
    }

    private static void runDeferredOriginMerge(ServerLevel level, BlockPos originPos) {
        double originX = originPos.getX() + 0.5D;
        double originY = originPos.getY() + 0.5D;
        double originZ = originPos.getZ() + 0.5D;
        AABB searchBounds = new AABB(
                originX - DEFERRED_MERGE_RADIUS,
                originY - DEFERRED_MERGE_RADIUS,
                originZ - DEFERRED_MERGE_RADIUS,
                originX + DEFERRED_MERGE_RADIUS,
                originY + DEFERRED_MERGE_RADIUS,
                originZ + DEFERRED_MERGE_RADIUS
        );

        List<ItemEntity> nearbyDrops = level.getEntitiesOfClass(
                ItemEntity.class,
                searchBounds,
                item -> item.isAlive() && item.getAge() <= DEFERRED_DROPS_MAX_AGE_TICKS
        );
        if (nearbyDrops.isEmpty()) {
            return;
        }

        for (ItemEntity nearbyDrop : nearbyDrops) {
            nearbyDrop.setPos(originX, originY, originZ);
            nearbyDrop.setDeltaMovement(0.0D, 0.0D, 0.0D);
        }

        mergeItemsInPlace(nearbyDrops);
    }

    private static void mergeItemsInPlace(List<ItemEntity> itemEntities) {
        for (int i = 0; i < itemEntities.size(); i++) {
            ItemEntity current = itemEntities.get(i);
            if (!current.isAlive()) {
                continue;
            }

            ItemStack currentStack = current.getItem();
            if (currentStack.isEmpty()) {
                continue;
            }

            for (int j = i + 1; j < itemEntities.size(); j++) {
                ItemEntity other = itemEntities.get(j);
                if (!other.isAlive()) {
                    continue;
                }

                ItemStack otherStack = other.getItem();
                if (otherStack.isEmpty()) {
                    continue;
                }
                if (!ItemStack.isSameItemSameComponents(currentStack, otherStack)) {
                    continue;
                }

                int transferable = Math.min(currentStack.getMaxStackSize() - currentStack.getCount(), otherStack.getCount());
                if (transferable <= 0) {
                    continue;
                }

                currentStack.grow(transferable);
                otherStack.shrink(transferable);

                if (otherStack.isEmpty()) {
                    other.discard();
                } else {
                    other.setItem(otherStack);
                }
            }

            current.setItem(currentStack);
        }
    }

    private static boolean isSameVeinTarget(BlockState originState, BlockState candidateState, boolean betterOreVeinMiningEnabled) {
        if (originState.getBlock() == candidateState.getBlock()) {
            return true;
        }

        return betterOreVeinMiningEnabled && isEquivalentOre(originState, candidateState);
    }

    private static boolean isEquivalentOre(BlockState first, BlockState second) {
        if (!first.is(ConventionalBlockTags.ORES) || !second.is(ConventionalBlockTags.ORES)) {
            return false;
        }

        Identifier firstId = BuiltInRegistries.BLOCK.getKey(first.getBlock());
        Identifier secondId = BuiltInRegistries.BLOCK.getKey(second.getBlock());
        if (firstId == BuiltInRegistries.BLOCK.getDefaultKey() || secondId == BuiltInRegistries.BLOCK.getDefaultKey()) {
            return false;
        }
        if (!firstId.getNamespace().equals(secondId.getNamespace())) {
            return false;
        }

        return normalizeOrePath(firstId.getPath()).equals(normalizeOrePath(secondId.getPath()));
    }

    private static String normalizeOrePath(String path) {
        if (path.startsWith("deepslate_")) {
            return path.substring("deepslate_".length());
        }

        return path;
    }

    private record PendingOriginMerge(ResourceKey<Level> dimension, BlockPos originPos, int executeAtTick) {
    }
}
