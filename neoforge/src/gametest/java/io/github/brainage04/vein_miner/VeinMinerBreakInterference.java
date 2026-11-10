package io.github.brainage04.vein_miner;

import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/// Stands in for another mod at the two ends of the bus: it refuses one chosen block, or lets a
/// break the game pre-cancelled through. A test arms it before its break and stops it afterwards.
@EventBusSubscriber(modid = VeinMiner.MOD_ID)
public final class VeinMinerBreakInterference {
    private static EventPriority refusalPriority;
    private static BlockPos refusedPos;
    private static boolean releasesCanceled;

    private VeinMinerBreakInterference() {}

    static void refuseAt(EventPriority priority, BlockPos pos) {
        refusalPriority = priority;
        refusedPos = pos;
        releasesCanceled = false;
    }

    static void releaseCanceled() {
        refusalPriority = null;
        refusedPos = null;
        releasesCanceled = true;
    }

    static void stop() {
        refusalPriority = null;
        refusedPos = null;
        releasesCanceled = false;
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
