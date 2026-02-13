package com.github.brainage04.vein_miner.mixin;

import com.github.brainage04.vein_miner.leaf.LeafDecayRateHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockDecayMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void veinMiner$queueAcceleratedDecay(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        LeafDecayRateHandler.queueIfDecaying(level, pos, level.getBlockState(pos));
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void veinMiner$cancelVanillaDecay(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!LeafDecayRateHandler.shouldCancelVanillaDecay(state)) {
            return;
        }

        LeafDecayRateHandler.queueIfDecaying(level, pos, state);
        ci.cancel();
    }
}
