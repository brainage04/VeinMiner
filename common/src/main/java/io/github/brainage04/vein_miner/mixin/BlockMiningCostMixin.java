package io.github.brainage04.vein_miner.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.brainage04.vein_miner.vein.VeinMiningHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Block.class)
public abstract class BlockMiningCostMixin {
    @WrapOperation(
            method = "playerDestroy",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"
            )
    )
    private void veinMiner$applyConfiguredExhaustionCost(Player player, float amount, Operation<Void> original) {
        if (VeinMiningHandler.isMiningAdditionalBlock()) {
            player.causeFoodExhaustion(VeinMiningHandler.additionalBlockExhaustionCost());
        } else {
            original.call(player, amount);
        }
    }
}
