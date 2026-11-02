package com.notheartyt.fastworldjoin;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastWorldJoinClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("FastWorldJoin");

    private static FastWorldJoinConfig config;
    private static int waited = 0;

    @Override
    public void onInitializeClient() {
        config = FastWorldJoinConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(FastWorldJoinClient::onTick);
        LOGGER.info("Fast World Join loaded (enabled={})", config.enabled);
    }

    private static void onTick(MinecraftClient client) {
        if (!config.enabled || !(client.currentScreen instanceof LevelLoadingScreen)) {
            waited = 0;
            return;
        }
        if (client.player == null || client.world == null) {
            return;
        }

        waited++;
        if (waited < config.minWaitTicks) {
            return;
        }

        if (waited >= config.maxWaitTicks || nearbyChunksReady(client)) {
            client.setScreen(null);
            waited = 0;
        }
    }

    private static boolean nearbyChunksReady(MinecraftClient client) {
        BlockPos pos = client.player.getBlockPos();
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        int r = config.requiredRadius;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (client.world.getChunkManager().getChunk(cx + dx, cz + dz, ChunkStatus.FULL, false) == null) {
                    return false;
                }
            }
        }
        return true;
    }
}
