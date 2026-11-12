package io.github.brainage04.vein_miner;

import static io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.configureOreMining;

import io.github.brainage04.minecraftmodconventions.ClientGameTestServers;
import io.github.brainage04.vein_miner.config.VeinMinerConfig;
import io.github.brainage04.vein_miner.config.VeinMinerConfigManager;
import io.github.brainage04.vein_miner.gametest.VeinMinerTestFixtures.ConfigSnapshot;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

@SuppressWarnings("UnstableApiUsage")
public class VeinMinerClientGameTest implements FabricClientGameTest {
    private static final int SPAWN_PROTECTION_RADIUS = 2;
    // High above the world spawn, clear of the film's stage and the player.
    private static final int SPAWN_CHECK_HEIGHT = 30;

    @Override
    public void runTest(ClientGameTestContext context) {
        ClientGameTestServers.withDedicatedServer(
                context,
                ClientGameTestServers.flatServerProperties(),
                "VeinMiner GameTest",
                server -> {
                    ClientGameTestServers.assertClientWorldAndPlayerAvailable(context);
                    VeinMinerShowcase.film(context, server);
                    server.runOnServer(VeinMinerClientGameTest::spawnProtectedBlocksAreNotMined);
                });
    }

    /**
     * Spawn protection exists only on a dedicated server (DedicatedServer.java:512-535); the
     * GameTest server never protects anything (MinecraftServer.java:1413-1415).
     */
    private static void spawnProtectedBlocksAreNotMined(MinecraftServer minecraftServer) {
        DedicatedServer server = (DedicatedServer) minecraftServer;
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        BlockPos spawn = level.getRespawnData().pos();
        BlockPos base = spawn.above(SPAWN_CHECK_HEIGHT);
        // Protected: at most SPAWN_PROTECTION_RADIUS blocks from the spawn on both axes.
        BlockPos origin = base.east(SPAWN_PROTECTION_RADIUS + 2);
        BlockPos outside = base.east(SPAWN_PROTECTION_RADIUS + 1);
        BlockPos protectedEdge = base.east(SPAWN_PROTECTION_RADIUS);
        BlockPos protectedInner = base.east(SPAWN_PROTECTION_RADIUS - 1);
        List<BlockPos> vein = List.of(origin, outside, protectedEdge, protectedInner);
        NameAndId operator = new NameAndId(UUID.randomUUID(), "VeinMinerOperator");
        int previousRadius = server.spawnProtectionRadius();
        GameType previousGameMode = player.gameMode.getGameModeForPlayer();
        ItemStack previousTool = player.getMainHandItem();
        VeinMinerConfig config = VeinMinerConfigManager.getConfig();
        ConfigSnapshot snapshot = new ConfigSnapshot(config);
        try {
            configureOreMining(config, vein.size());
            server.setSpawnProtectionRadius(SPAWN_PROTECTION_RADIUS);
            // DedicatedServer.isUnderSpawnProtection protects nothing while the op list is empty.
            server.getPlayerList().op(operator);
            player.setGameMode(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            player.setShiftKeyDown(true);
            for (BlockPos pos : vein) {
                level.setBlockAndUpdate(pos, Blocks.DIAMOND_ORE.defaultBlockState());
            }
            if (server.isUnderSpawnProtection(level, outside, player)
                    || !server.isUnderSpawnProtection(level, protectedEdge, player)) {
                throw new AssertionError("Expected spawn protection to start at " + protectedEdge);
            }

            if (!player.gameMode.destroyBlock(origin)) {
                throw new AssertionError("Expected the player to break the unprotected origin.");
            }

            if (!level.getBlockState(outside).isAir()) {
                throw new AssertionError("Expected the vein to mine the unprotected block.");
            }
            if (!level.getBlockState(protectedEdge).is(Blocks.DIAMOND_ORE)
                    || !level.getBlockState(protectedInner).is(Blocks.DIAMOND_ORE)) {
                throw new AssertionError("The vein mined blocks under spawn protection.");
            }
        } finally {
            for (BlockPos pos : vein) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            for (ItemEntity item :
                    level.getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(4.0D))) {
                item.discard();
            }
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, previousTool);
            player.setGameMode(previousGameMode);
            server.getPlayerList().deop(operator);
            server.setSpawnProtectionRadius(previousRadius);
            snapshot.restore(config);
        }
    }
}
