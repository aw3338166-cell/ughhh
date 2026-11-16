package com.austinwing.mcaibuild;

import net.minecraft.client.MinecraftClient;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Incremental, client-thread-only scanner for chunks already loaded by Minecraft.
 * It never requests an unloaded chunk and never reads world state on a worker thread.
 */
public final class BaseFinderScanner {
    private static final int BLOCKS_PER_TICK = 4096;
    private static final int MAX_CANDIDATES = 20;

    public record Candidate(int chunkX, int chunkZ, int y, int score,
                            int containers, int workstations, int redstone,
                            int doors, int crops, int lighting, boolean underground) {
        public String confidence() {
            return score >= 18 ? "HIGH" : score >= 10 ? "MEDIUM" : "LOW";
        }
        public String evidence() {
            return "Storage " + containers + " | Workstations " + workstations
                    + " | Redstone " + redstone + " | Doors " + doors
                    + " | Crops " + crops + " | Lights " + lighting
                    + (underground ? " | below surface" : "");
        }
    }

    private static final class ChunkWork {
        final int chunkX, chunkZ, bottomY, topY, seaLevel;
        final Chunk chunk;
        int cursor;
        int containers, workstations, redstone, doors, crops, lighting;
        int lowestFeatureY = Integer.MAX_VALUE;
        boolean underground;

        ChunkWork(int chunkX, int chunkZ, Chunk chunk, int bottomY, int topY, int seaLevel) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.chunk = chunk;
            this.bottomY = bottomY;
            this.topY = topY;
            this.seaLevel = seaLevel;
        }
    }

    private static final List<ChunkWork> queue = new ArrayList<>();
    private static final List<Candidate> candidates = new ArrayList<>();
    private static int queueIndex;
    private static int scannedChunks;
    private static int totalBlocks;
    private static int processedBlocks;
    private static boolean scanning;
    private static String message = "Scan nearby loaded chunks to find possible player-built areas.";

    private BaseFinderScanner() {}

    public static void start(MinecraftClient client, int radius) {
        queue.clear();
        candidates.clear();
        queueIndex = scannedChunks = totalBlocks = processedBlocks = 0;
        if (client.player == null || client.world == null) {
            scanning = false;
            message = "Join a world before scanning.";
            return;
        }

        int centerX = client.player.getBlockX() >> 4;
        int centerZ = client.player.getBlockZ() >> 4;
        int bottomY = client.world.getBottomY();
        int topY = bottomY + client.world.getHeight();
        int seaLevel = client.world.getSeaLevel();

        for (int cx = centerX - radius; cx <= centerX + radius; cx++) {
            for (int cz = centerZ - radius; cz <= centerZ + radius; cz++) {
                // Only inspect chunks the client has already received and currently has loaded.
                if (!client.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
                Chunk chunk = client.world.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                queue.add(new ChunkWork(cx, cz, chunk, bottomY, topY, seaLevel));
                totalBlocks += 256 * (topY - bottomY);
            }
        }
        scanning = !queue.isEmpty();
        message = scanning
                ? "Scanning " + queue.size() + " loaded chunks incrementally..."
                : "No chunks in that radius are currently loaded.";
        if (!scanning) finish();
    }

    /** Call once per client tick. World access remains on the client thread. */
    public static void tick(MinecraftClient client) {
        if (!scanning) return;
        if (client.world == null || client.player == null) {
            scanning = false;
            message = "Scan stopped because the world was closed.";
            return;
        }

        int budget = BLOCKS_PER_TICK;
        while (budget > 0 && queueIndex < queue.size()) {
            ChunkWork work = queue.get(queueIndex);
            int volume = 256 * (work.topY - work.bottomY);
            int count = Math.min(budget, volume - work.cursor);
            scanRange(work, work.cursor, work.cursor + count);
            work.cursor += count;
            budget -= count;
            processedBlocks += count;
            if (work.cursor >= volume) {
                scoreChunk(work);
                scannedChunks++;
                queueIndex++;
            }
        }

        if (queueIndex >= queue.size()) finish();
        else message = "Scanning loaded chunks: " + scannedChunks + "/" + queue.size()
                + " chunks (" + (totalBlocks == 0 ? 0 : processedBlocks * 100 / totalBlocks) + "%)";
    }

    private static void scanRange(ChunkWork work, int from, int to) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int height = work.topY - work.bottomY;
        for (int i = from; i < to; i++) {
            int x = i / (height * 16);
            int remainder = i % (height * 16);
            int z = remainder / height;
            int y = work.bottomY + remainder % height;
            pos.set((work.chunkX << 4) + x, y, (work.chunkZ << 4) + z);
            BlockState state = work.chunk.getBlockState(pos);
            if (state.isAir()) continue;
            String id = Registries.BLOCK.getId(state.getBlock()).getPath();
            boolean feature = false;

            if (isContainer(id)) { work.containers++; feature = true; }
            if (isWorkstation(id)) { work.workstations++; feature = true; }
            if (isRedstone(id)) { work.redstone++; feature = true; }
            if (id.endsWith("_door") || id.endsWith("_trapdoor")) { work.doors++; feature = true; }
            if (isCrop(id)) work.crops++;
            if (isLighting(id)) work.lighting++;
            if (feature) {
                work.lowestFeatureY = Math.min(work.lowestFeatureY, y);
                if (y < work.seaLevel - 8) work.underground = true;
            }
        }
    }

    private static void scoreChunk(ChunkWork w) {
        int utility = w.containers + w.workstations + w.redstone;
        int score = w.containers * 3 + w.workstations * 2 + w.redstone * 2
                + w.doors + Math.min(w.crops, 8) + Math.min(w.lighting, 8) / 2;
        if (utility >= 4) score += 3;
        if (w.containers > 0 && w.workstations > 0) score += 2;
        if (w.underground) score += 2;

        // Require multiple independent clues to avoid flagging a single utility block.
        int clueTypes = (w.containers > 0 ? 1 : 0) + (w.workstations > 0 ? 1 : 0)
                + (w.redstone > 0 ? 1 : 0) + (w.doors > 0 ? 1 : 0)
                + (w.crops >= 4 ? 1 : 0) + (w.lighting >= 4 ? 1 : 0);
        if (score < 8 || clueTypes < 2) return;

        int y = w.lowestFeatureY == Integer.MAX_VALUE ? w.seaLevel : w.lowestFeatureY;
        candidates.add(new Candidate(w.chunkX, w.chunkZ, y, score, w.containers,
                w.workstations, w.redstone, w.doors, w.crops, w.lighting, w.underground));
        candidates.sort(Comparator.comparingInt(Candidate::score).reversed());
        if (candidates.size() > MAX_CANDIDATES) candidates.remove(candidates.size() - 1);
    }

    private static void finish() {
        scanning = false;
        message = "Scan complete. Read " + scannedChunks + " loaded chunks; results are clues, not proof.";
    }

    private static boolean isContainer(String id) {
        return id.equals("chest") || id.equals("trapped_chest") || id.equals("barrel")
                || id.endsWith("_shulker_box") || id.equals("hopper");
    }
    private static boolean isWorkstation(String id) {
        return id.equals("crafting_table") || id.equals("furnace") || id.equals("smoker")
                || id.equals("blast_furnace") || id.equals("enchanting_table")
                || id.equals("brewing_stand") || id.equals("smithing_table")
                || id.equals("loom") || id.equals("cartography_table")
                || id.equals("stonecutter") || id.equals("grindstone")
                || id.equals("lectern") || id.endsWith("_anvil");
    }
    private static boolean isRedstone(String id) {
        return id.contains("redstone") || id.contains("piston") || id.contains("observer")
                || id.contains("repeater") || id.contains("comparator")
                || id.equals("lever") || id.endsWith("_button")
                || id.equals("tripwire_hook") || id.equals("target")
                || id.equals("daylight_detector") || id.equals("dispenser") || id.equals("dropper");
    }
    private static boolean isCrop(String id) {
        return id.equals("wheat") || id.equals("carrots") || id.equals("potatoes")
                || id.equals("beetroots") || id.equals("nether_wart") || id.equals("cocoa")
                || id.equals("sweet_berry_bush") || id.equals("melon_stem") || id.equals("pumpkin_stem");
    }
    private static boolean isLighting(String id) {
        return id.contains("torch") || id.contains("lantern") || id.equals("glowstone")
                || id.equals("sea_lantern") || id.equals("shroomlight") || id.equals("glow_lichen")
                || id.equals("ochre_froglight") || id.equals("verdant_froglight")
                || id.equals("pearlescent_froglight");
    }

    public static boolean isScanning() { return scanning; }
    public static String message() { return message; }
    public static int scannedChunks() { return scannedChunks; }
    public static int totalChunks() { return queue.size(); }
    public static int progressPercent() { return totalBlocks == 0 ? 0 : processedBlocks * 100 / totalBlocks; }
    public static List<Candidate> candidates() { return List.copyOf(candidates); }
}
