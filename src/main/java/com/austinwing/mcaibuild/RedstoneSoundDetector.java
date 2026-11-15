package com.austinwing.mcaibuild;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class RedstoneSoundDetector {
    public record Detection(String soundId, double x, double y, double z, long detectedAt) {}

    private static final ArrayDeque<Detection> RECENT = new ArrayDeque<>();
    private static final int MAX_HISTORY = 40;
    private static final int[] RANGES = {16, 32, 48, 64};
    private static boolean enabled = true;
    private static int rangeIndex = 2;

    private RedstoneSoundDetector() {}

    public static void record(String soundId, double x, double y, double z) {
        if (!enabled || soundId == null || !isRedstoneRelated(soundId)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        double dx = x - client.player.getX();
        double dy = y - client.player.getY();
        double dz = z - client.player.getZ();
        if (dx * dx + dy * dy + dz * dz > getRange() * getRange()) return;

        RECENT.addFirst(new Detection(soundId, x, y, z, System.currentTimeMillis()));
        while (RECENT.size() > MAX_HISTORY) RECENT.removeLast();
    }

    private static boolean isRedstoneRelated(String id) {
        String value = id.toLowerCase(java.util.Locale.ROOT);
        return value.contains("piston")
                || value.contains("dispenser")
                || value.contains("dropper")
                || value.contains("repeater")
                || value.contains("comparator")
                || value.contains("lever")
                || value.contains("button")
                || value.contains("tripwire")
                || value.contains("observer")
                || value.contains("redstone_torch");
    }

    public static boolean isEnabled() { return enabled; }
    public static void toggleEnabled() { enabled = !enabled; }
    public static int getRange() { return RANGES[rangeIndex]; }
    public static void cycleRange() { rangeIndex = (rangeIndex + 1) % RANGES.length; }
    public static List<Detection> getRecent() { return new ArrayList<>(RECENT); }
    public static void clear() { RECENT.clear(); }

    public static String directionFromPlayer(Detection detection) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return "--";
        double dx = detection.x() - client.player.getX();
        double dz = detection.z() - client.player.getZ();
        String[] directions = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int index = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(dx, dz)) / 45.0), 8);
        return directions[index];
    }

    public static double distanceFromPlayer(Detection detection) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return 0;
        double dx = detection.x() - client.player.getX();
        double dy = detection.y() - client.player.getY();
        double dz = detection.z() - client.player.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static String displayName(String soundId) {
        String name = soundId.replace("minecraft:block.", "").replace("minecraft:", "");
        return name.replace('.', ' ').replace('_', ' ');
    }

    public static String ageLabel(Detection detection) {
        long seconds = Math.max(0, (System.currentTimeMillis() - detection.detectedAt()) / 1000);
        if (seconds < 2) return "just now";
        if (seconds < 60) return seconds + "s ago";
        return (seconds / 60) + "m ago";
    }
}
