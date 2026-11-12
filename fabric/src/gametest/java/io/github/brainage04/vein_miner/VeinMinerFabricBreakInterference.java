package io.github.brainage04.vein_miner;

import io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.BreakInterference;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;

/** Stands in for another mod listening to Fabric's {@code PlayerBlockBreakEvents.BEFORE}. */
final class VeinMinerFabricBreakInterference implements BreakInterference {
    static final VeinMinerFabricBreakInterference INSTANCE = new VeinMinerFabricBreakInterference();

    private BlockPos refusedPos;
    private BlockPos actionPos;
    private Runnable action;

    private VeinMinerFabricBreakInterference() {
        PlayerBlockBreakEvents.BEFORE.register(
                (level, player, pos, state, blockEntity) -> interfere(pos));
    }

    @Override
    public void refuseAt(BlockPos pos) {
        stop();
        refusedPos = pos;
    }

    @Override
    public void runBefore(BlockPos pos, Runnable action) {
        stop();
        actionPos = pos;
        this.action = action;
    }

    @Override
    public void stop() {
        refusedPos = null;
        actionPos = null;
        action = null;
    }

    private boolean interfere(BlockPos pos) {
        if (action != null && pos.equals(actionPos)) {
            Runnable pending = action;
            action = null;
            pending.run();
        }
        return !pos.equals(refusedPos);
    }
}
