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

    private static boolean boosted = false;
    private static int originalViewDistance = 0;
    private static int restoreCountdown = -1;

    @Override
    public void onInitializeClient() {
        config = FastWorldJoinConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(FastWorldJoinClient::onTick);
        LOGGER.info("Fast World Join loaded (enabled={})", config.enabled);
    }

    private static void onTick(MinecraftClient client) {
        if (!config.enabled) {
            return;
        }

        boolean loading = client.currentScreen instanceof LevelLoadingScreen;

        // 1) Lower render distance while the loading screen is up.
        if (loading) {
            restoreCountdown = -1;
            if (!boosted) {
                int current = client.options.getViewDistance().getValue();
                if (current > config.joinViewDistance) {
                    originalViewDistance = current;
                    client.options.getViewDistance().setValue(config.joinViewDistance);
                    boosted = true;
                }
            }
        }

        // 2) Restore it a moment after the screen is gone.
        if (!loading && boosted) {
            if (client.world == null) {
                restoreViewDistance(client);
            } else {
                if (restoreCountdown < 0) {
                    restoreCountdown = config.restoreDelayTicks;
                }
                if (restoreCountdown-- <= 0) {
                    restoreViewDistance(client);
                }
            }
        }

        // 3) Close the loading screen as soon as nearby chunks are ready.
        if (!loading) {
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

    private static void restoreViewDistance(MinecraftClient client) {
        client.options.getViewDistance().setValue(originalViewDistance);
        boosted = false;
        restoreCountdown = -1;
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
