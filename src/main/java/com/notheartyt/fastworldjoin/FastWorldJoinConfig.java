package com.notheartyt.fastworldjoin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FastWorldJoinConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("fastworldjoin.json");

    public boolean enabled = true;
    public int minWaitTicks = 2;
    public int maxWaitTicks = 100;
    public int requiredRadius = 0;

    public static FastWorldJoinConfig load() {
        FastWorldJoinConfig cfg = new FastWorldJoinConfig();
        try {
            if (Files.exists(PATH)) {
                FastWorldJoinConfig read = GSON.fromJson(Files.readString(PATH), FastWorldJoinConfig.class);
                if (read != null) cfg = read;
            }
            cfg.requiredRadius = Math.max(0, Math.min(cfg.requiredRadius, 8));
            cfg.minWaitTicks = Math.max(0, cfg.minWaitTicks);
            cfg.maxWaitTicks = Math.max(cfg.minWaitTicks, cfg.maxWaitTicks);
            Files.writeString(PATH, GSON.toJson(cfg));
        } catch (IOException | RuntimeException e) {
            FastWorldJoinClient.LOGGER.warn("Could not read/write config, using defaults", e);
        }
        return cfg;
    }
}
