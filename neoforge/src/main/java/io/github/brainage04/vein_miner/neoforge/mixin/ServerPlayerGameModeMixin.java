package io.github.brainage04.vein_miner.neoforge.mixin;

import io.github.brainage04.vein_miner.vein.VeinMiningHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void veinMiner$prepareConnectedMining(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (!VeinMiningHandler.beforeBlockBreak(player.level(), player, pos, player.level().getBlockState(pos), player.level().getBlockEntity(pos))) {
            callback.setReturnValue(false);
        }
    }
}
