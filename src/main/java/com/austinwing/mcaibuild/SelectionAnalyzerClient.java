package com.austinwing.mcaibuild;

import com.austinwing.mcaibuild.materials.MaterialMath;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SelectionAnalyzerClient implements ClientModInitializer {
    private static final long MAX_SELECTION_BLOCKS = 1_000_000L;
    private static final int BLOCKS_PER_TICK = 4096;

    private static KeyBinding setFirstKey;
    private static KeyBinding setSecondKey;
    private static KeyBinding openKey;
    private static BlockPos first;
    private static BlockPos second;
    private static String dimension;
    private static Scan scan;
    private static String status = "Press J and K while looking at blocks to mark both corners.";

    private static final class Scan {
        final SelectionBounds bounds;
        final long volume;
        final Map<String, Integer> materials = new HashMap<>();
        long cursor;
        long blocks;
        long unloaded;

        Scan(SelectionBounds bounds) {
            this.bounds = bounds;
            this.volume = bounds.volume();
        }
    }

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MinecraftAIBuild.MOD_ID, "selection"));
        setFirstKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.minecraft_ai_build.selection_pos1", GLFW.GLFW_KEY_J, category));
        setSecondKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.minecraft_ai_build.selection_pos2", GLFW.GLFW_KEY_K, category));
        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.minecraft_ai_build.selection_analyzer", GLFW.GLFW_KEY_N, category));

        ClientTickEvents.END_CLIENT_TICK.register(SelectionAnalyzerClient::tick);
        MinecraftAIBuild.LOGGER.info("Live selection analyzer controls registered.");
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            if (dimension != null) clearAll();
            return;
        }

        String currentDimension = client.world.getRegistryKey().getValue().toString();
        if (dimension != null && !dimension.equals(currentDimension)) {
            clearAll();
            client.player.sendMessage(Text.literal("[AI Build] Selection cleared after dimension change."), false);
        }
        dimension = currentDimension;

        if (setFirstKey.wasPressed()) mark(client, true);
        if (setSecondKey.wasPressed()) mark(client, false);
        if (openKey.wasPressed() && client.currentScreen == null) {
            client.setScreen(new SelectionAnalyzerScreen());
        }
        advanceScan(client);
    }

    private static void mark(MinecraftClient client, boolean isFirst) {
        if (!(client.crosshairTarget instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            tell(client, "Look directly at a block before setting a selection corner.");
            return;
        }

        BlockPos position = hit.getBlockPos().toImmutable();
        if (isFirst) first = position;
        else second = position;

        tell(client, (isFirst ? "Corner 1" : "Corner 2") + " set to " + position.toShortString() + ".");
        refresh(client);
    }

    public static void refresh(MinecraftClient client) {
        if (client == null || client.world == null || first == null || second == null) {
            scan = null;
            status = "Mark both corners first (J and K while looking at blocks).";
            return;
        }

        SelectionBounds bounds = SelectionBounds.between(first, second);
        if (bounds.volume() > MAX_SELECTION_BLOCKS) {
            scan = null;
            status = "Selection is too large (" + bounds.volume()
                    + " blocks). Maximum analyzer size is " + MAX_SELECTION_BLOCKS + ".";
            return;
        }

        scan = new Scan(bounds);
        status = "Scanning loaded client chunks...";
    }

    private static void advanceScan(MinecraftClient client) {
        if (scan == null || client.world == null) return;

        Scan active = scan;
        int processedThisTick = 0;
        while (active.cursor < active.volume && processedThisTick < BLOCKS_PER_TICK) {
            long index = active.cursor++;
            processedThisTick++;

            int x = active.bounds.minX() + (int) (index % active.bounds.width());
            long yz = index / active.bounds.width();
            int z = active.bounds.minZ() + (int) (yz % active.bounds.length());
            int y = active.bounds.minY() + (int) (yz / active.bounds.length());

            if (!client.world.isChunkLoaded(x >> 4, z >> 4)) {
                active.unloaded++;
                continue;
            }

            BlockState state = client.world.getBlockState(new BlockPos(x, y, z));
            if (state.isAir()) continue;

            active.blocks++;
            String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
            if (state.getBlock().asItem() == Items.AIR) continue;

            Map<String, String> properties = new HashMap<>();
            state.getEntries().forEach((property, value) ->
                    properties.put(property.getName(), String.valueOf(value)));
            int itemCount = MaterialMath.itemCount(blockId, properties);
            if (itemCount > 0) active.materials.merge(blockId, itemCount, Integer::sum);
        }

        if (active.cursor >= active.volume) {
            status = "Complete — " + active.cursor + " positions checked.";
        } else {
            status = "Scanning: " + active.cursor + " / " + active.volume + " positions...";
        }
    }

    public static void clearSelection() {
        first = null;
        second = null;
        scan = null;
        status = "Selection cleared. Mark two new corners with J and K.";
    }

    private static void clearAll() {
        first = null;
        second = null;
        scan = null;
        dimension = null;
        status = "Selection cleared.";
    }

    private static void tell(MinecraftClient client, String message) {
        if (client.player != null) client.player.sendMessage(Text.literal("[AI Build] " + message), false);
    }

    public static BlockPos first() { return first; }
    public static BlockPos second() { return second; }
    public static String status() { return status; }
    public static SelectionBounds bounds() {
        return first == null || second == null ? null : SelectionBounds.between(first, second);
    }
    public static long checked() { return scan == null ? 0 : scan.cursor; }
    public static long volume() { return scan == null ? 0 : scan.volume; }
    public static long blockCount() { return scan == null ? 0 : scan.blocks; }
    public static long unloadedCount() { return scan == null ? 0 : scan.unloaded; }
    public static boolean scanning() {
        return scan != null && scan.cursor < scan.volume;
    }
    public static List<Map.Entry<String, Integer>> topMaterials(int limit) {
        if (scan == null) return List.of();
        return scan.materials.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .toList();
    }
}