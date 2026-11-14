package io.github.brainage04.vein_miner;

import io.github.brainage04.minecraftmodconventions.ClientGameTestRecorder;
import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import java.util.LinkedHashSet;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The client GameTest's film: a sneaking survival player clears a mixed diamond vein, then breaks
 * one ore without sneaking and leaves its neighbour.
 */
@SuppressWarnings("UnstableApiUsage")
public final class VeinMinerShowcase {
    private static final int STAGE_Y = 63;
    private static final BlockPos MIXED_ORIGIN = new BlockPos(3, STAGE_Y, 0);
    private static final List<BlockPos> MIXED_VEIN =
            List.of(
                    MIXED_ORIGIN,
                    MIXED_ORIGIN.east(),
                    MIXED_ORIGIN.east(2),
                    MIXED_ORIGIN.east(3),
                    MIXED_ORIGIN.east(4));
    private static final BlockPos UNSNEAKING_ORIGIN = new BlockPos(3, STAGE_Y, 3);
    private static final BlockPos UNSNEAKING_NEIGHBOR = UNSNEAKING_ORIGIN.east();
    private static final AABB STAGE_BOUNDS =
            new AABB(-4.0D, STAGE_Y - 1.0D, -4.0D, 12.0D, STAGE_Y + 4.0D, 8.0D);

    private VeinMinerShowcase() {}

    public static void film(ClientGameTestContext context, TestDedicatedServerContext server) {
        ConfigSnapshot configSnapshot =
                server.computeOnServer(
                        minecraftServer -> new ConfigSnapshot(VeinMinerConfigManager.getConfig()));
        server.runOnServer(minecraftServer -> prepareMixedVein(player(minecraftServer)));
        context.runOnClient(
                client -> {
                    if (client.player == null) {
                        throw new AssertionError(
                                "Expected a connected client player for the recording.");
                    }
                    client.player.setYRot(-90.0F);
                    client.player.setXRot(12.0F);
                });
        context.waitTicks(10);
        try {
            ClientGameTestRecorder.startRecording(context);
            ClientGameTestRecorder.showStep(
                    context,
                    "veinminer.stage",
                    "VeinMiner: mixed ore vein",
                    "A survival player prepares five connected ores");
            context.waitTicks(30);

            ClientGameTestRecorder.showStep(
                    context,
                    "veinminer.sneak",
                    "Sneak mining enabled",
                    "Breaking one ore should clear the mixed diamond vein");
            server.runOnServer(
                    minecraftServer -> breakBlock(player(minecraftServer), MIXED_ORIGIN, true));
            context.waitTicks(30);

            ClientGameTestRecorder.showStep(
                    context,
                    "veinminer.gate",
                    "Sneak gate",
                    "Without sneaking, the connected neighbor stays intact");
            server.runOnServer(minecraftServer -> prepareUnsneakingVein(player(minecraftServer)));
            context.waitTicks(25);
            server.runOnServer(
                    minecraftServer ->
                            breakBlock(player(minecraftServer), UNSNEAKING_ORIGIN, false));
            context.waitTicks(30);
            ClientGameTestRecorder.showStep(
                    context,
                    "veinminer.complete",
                    "VeinMiner verified",
                    "Mixed traversal and sneak-gated mining passed");
            context.waitTicks(20);
        } finally {
            server.runOnServer(
                    minecraftServer -> configSnapshot.restore(VeinMinerConfigManager.getConfig()));
        }
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void prepareMixedVein(ServerPlayer player) {
        ServerLevel level = player.level();
        prepareStage(level, player);
        configureMixedOreMining(VeinMinerConfigManager.getConfig());
        for (int index = 0; index < MIXED_VEIN.size(); index++) {
            Block block = index % 2 == 0 ? Blocks.DIAMOND_ORE : Blocks.DEEPSLATE_DIAMOND_ORE;
            level.setBlock(MIXED_VEIN.get(index), block.defaultBlockState(), 3);
        }
    }

    private static void prepareUnsneakingVein(ServerPlayer player) {
        ServerLevel level = player.level();
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, STAGE_BOUNDS)) {
            itemEntity.discard();
        }
        level.setBlock(UNSNEAKING_ORIGIN, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
        level.setBlock(UNSNEAKING_NEIGHBOR, Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState(), 3);
    }

    private static void prepareStage(ServerLevel level, ServerPlayer player) {
        for (BlockPos position : BlockPos.betweenClosed(-4, STAGE_Y - 1, -4, 12, STAGE_Y + 3, 8)) {
            level.setBlock(
                    position,
                    position.getY() == STAGE_Y - 1
                            ? Blocks.STONE.defaultBlockState()
                            : Blocks.AIR.defaultBlockState(),
                    3);
        }
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        player.setYRot(-90.0F);
        player.setXRot(12.0F);
        player.teleportTo(0.5D, STAGE_Y + 1.0D, 0.5D);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static void configureMixedOreMining(VeinMinerConfig config) {
        config.enableVeinMining = true;
        config.betterOreVeinMining = true;
        config.maxOreBlocks = MIXED_VEIN.size();
        config.whitelist.clear();
        config.whitelist.add("minecraft:diamond_ore");
        config.whitelist.add("minecraft:deepslate_diamond_ore");
    }

    private static void breakBlock(ServerPlayer player, BlockPos position, boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        if (!player.gameMode.destroyBlock(position)) {
            throw new AssertionError("Expected survival player to destroy " + position + ".");
        }
    }

    private static final class ConfigSnapshot {
        private final boolean enabled;
        private final boolean betterOreVeinMining;
        private final int maxOreBlocks;
        private final LinkedHashSet<String> whitelist;

        private ConfigSnapshot(VeinMinerConfig config) {
            enabled = config.enableVeinMining;
            betterOreVeinMining = config.betterOreVeinMining;
            maxOreBlocks = config.maxOreBlocks;
            whitelist = new LinkedHashSet<>(config.whitelist);
        }

        private void restore(VeinMinerConfig config) {
            config.enableVeinMining = enabled;
            config.betterOreVeinMining = betterOreVeinMining;
            config.maxOreBlocks = maxOreBlocks;
            config.whitelist.clear();
            config.whitelist.addAll(whitelist);
        }
    }
}
