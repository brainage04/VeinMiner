package io.github.brainage04.vein_miner;

import io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.BreakInterference;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/// Stands in for another mod at the two ends of the bus: it refuses one chosen block, runs an
/// action before one chosen break, or lets a break the game pre-cancelled through. A test arms it
/// before its break and stops it afterwards.
@EventBusSubscriber(modid = VeinMiner.MOD_ID)
public final class VeinMinerBreakInterference {
    /// The shared GameTests' interference: another mod listening at the highest priority.
    static final BreakInterference AT_HIGHEST_PRIORITY =
            new BreakInterference() {
                @Override
                public void refuseAt(BlockPos pos) {
                    VeinMinerBreakInterference.refuseAt(EventPriority.HIGHEST, pos);
                }

                @Override
                public void runBefore(BlockPos pos, Runnable action) {
                    VeinMinerBreakInterference.stop();
                    actionPos = pos;
                    VeinMinerBreakInterference.action = action;
                }

                @Override
                public void stop() {
                    VeinMinerBreakInterference.stop();
                }
            };

    private static EventPriority refusalPriority;
    private static BlockPos refusedPos;
    private static boolean releasesCanceled;
    private static BlockPos actionPos;
    private static Runnable action;

    private VeinMinerBreakInterference() {}

    static void refuseAt(EventPriority priority, BlockPos pos) {
        stop();
        refusalPriority = priority;
        refusedPos = pos;
    }

    static void releaseCanceled() {
        stop();
        releasesCanceled = true;
    }

    static void stop() {
        refusalPriority = null;
        refusedPos = null;
        releasesCanceled = false;
        actionPos = null;
        action = null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interfereFirst(BreakBlockEvent event) {
        interfere(EventPriority.HIGHEST, event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void interfereLast(BreakBlockEvent event) {
        interfere(EventPriority.LOWEST, event);
    }

    private static void interfere(EventPriority priority, BreakBlockEvent event) {
        if (action != null
                && priority == EventPriority.HIGHEST
                && event.getPos().equals(actionPos)) {
            Runnable pending = action;
            action = null;
            pending.run();
        }

        if (refusedPos != null
                && priority == refusalPriority
                && event.getPos().equals(refusedPos)) {
            event.setCanceled(true);
        }

        if (releasesCanceled) {
            event.setCanceled(false);
        }
    }
}
