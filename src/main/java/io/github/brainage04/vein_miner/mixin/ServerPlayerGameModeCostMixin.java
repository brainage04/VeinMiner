package io.github.brainage04.vein_miner.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.brainage04.vein_miner.vein.VeinMiningHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeCostMixin {
    @Shadow
    @Final
    protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void veinMiner$mineConnectedBlocksAfterOrigin(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> callback
    ) {
        VeinMiningHandler.completeBlockBreak(player, pos, callback.getReturnValueZ());
    }

    @WrapOperation(
            method = "destroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;mineBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V"
            )
    )
    private void veinMiner$applyConfiguredDurabilityCost(
            ItemStack stack,
            Level level,
            BlockState state,
            BlockPos pos,
            Player player,
            Operation<Void> original
    ) {
        if (!VeinMiningHandler.isMiningAdditionalBlock()) {
            original.call(stack, level, state, pos, player);
            return;
        }

        int durabilityCost = VeinMiningHandler.additionalBlockDurabilityCost();
        if (durabilityCost > 0) {
            stack.hurtAndBreak(durabilityCost, player, EquipmentSlot.MAINHAND);
        }
    }
}
