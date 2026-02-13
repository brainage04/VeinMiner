package com.github.brainage04.vein_miner.leaf;

import com.github.brainage04.vein_miner.config.VeinMinerConfig;
import com.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class LeafDecayRateHandler {
    private static final int RANDOM_TICK_SECTION_VOLUME = 16 * 16 * 16;

    private static final List<PendingLeafDecay> pendingLeafDecays = new ArrayList<>();
    private static final Set<LeafDecayKey> queuedLeafDecays = new HashSet<>();

    private LeafDecayRateHandler() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(LeafDecayRateHandler::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            pendingLeafDecays.clear();
            queuedLeafDecays.clear();
        });
    }

    public static boolean shouldCancelVanillaDecay(BlockState state) {
        return getLeafDecaySpeedMultiplier() > 1 && isDecayingLeaf(state);
    }

    public static void queueIfDecaying(ServerLevel level, BlockPos pos, BlockState state) {
        int multiplier = getLeafDecaySpeedMultiplier();
        if (multiplier <= 1 || !isDecayingLeaf(state)) {
            return;
        }

        int delay = sampleScaledDecayDelayTicks(level, multiplier);
        if (delay <= 0) {
            return;
        }

        BlockPos immutablePos = pos.immutable();
        LeafDecayKey key = new LeafDecayKey(level.dimension(), immutablePos.asLong());
        if (!queuedLeafDecays.add(key)) {
            return;
        }

        pendingLeafDecays.add(new PendingLeafDecay(
                level.dimension(),
                immutablePos,
                level.getServer().getTickCount() + delay
        ));
    }

    private static void tick(MinecraftServer server) {
        if (pendingLeafDecays.isEmpty()) {
            return;
        }

        int currentTick = server.getTickCount();
        Iterator<PendingLeafDecay> iterator = pendingLeafDecays.iterator();
        while (iterator.hasNext()) {
            PendingLeafDecay pendingDecay = iterator.next();
            if (pendingDecay.executeAtTick > currentTick) {
                continue;
            }

            ServerLevel level = server.getLevel(pendingDecay.dimension);
            if (level == null || !level.isLoaded(pendingDecay.leafPos)) {
                continue;
            }

            iterator.remove();
            queuedLeafDecays.remove(new LeafDecayKey(pendingDecay.dimension, pendingDecay.leafPos.asLong()));
            runQueuedLeafDecay(level, pendingDecay.leafPos);
        }
    }

    private static void runQueuedLeafDecay(ServerLevel level, BlockPos leafPos) {
        BlockState state = level.getBlockState(leafPos);
        if (!isDecayingLeaf(state)) {
            return;
        }

        Block.dropResources(state, level, leafPos);
        level.removeBlock(leafPos, false);
    }

    private static int sampleScaledDecayDelayTicks(ServerLevel level, int multiplier) {
        int randomTickSpeed = level.getGameRules().get(GameRules.RANDOM_TICK_SPEED);
        if (randomTickSpeed <= 0) {
            return -1;
        }

        double missChancePerAttempt = 1.0D - (1.0D / RANDOM_TICK_SECTION_VOLUME);
        double vanillaDecayChancePerTick = 1.0D - Math.pow(missChancePerAttempt, randomTickSpeed);
        double scaledDecayChancePerTick = Math.min(1.0D, vanillaDecayChancePerTick * multiplier);
        if (scaledDecayChancePerTick <= 0.0D) {
            return -1;
        }
        if (scaledDecayChancePerTick >= 1.0D) {
            return 1;
        }

        double random = level.getRandom().nextDouble();
        return 1 + (int) Math.floor(Math.log(1.0D - random) / Math.log(1.0D - scaledDecayChancePerTick));
    }

    private static int getLeafDecaySpeedMultiplier() {
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        return Math.max(1, config.leafDecaySpeedMultiplier);
    }

    private static boolean isDecayingLeaf(BlockState state) {
        if (!(state.getBlock() instanceof LeavesBlock)) {
            return false;
        }
        if (!state.hasProperty(BlockStateProperties.PERSISTENT)
                || !state.hasProperty(BlockStateProperties.DISTANCE)) {
            return false;
        }

        return !state.getValue(BlockStateProperties.PERSISTENT)
                && state.getValue(BlockStateProperties.DISTANCE) == LeavesBlock.DECAY_DISTANCE;
    }

    private record LeafDecayKey(ResourceKey<Level> dimension, long leafPos) {
    }

    private record PendingLeafDecay(ResourceKey<Level> dimension, BlockPos leafPos, int executeAtTick) {
    }
}
