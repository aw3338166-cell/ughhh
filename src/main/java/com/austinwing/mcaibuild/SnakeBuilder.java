package com.austinwing.mcaibuild;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Produces an optional layer-by-layer serpentine placement order.
 * It only orders schematic entries; placement remains owned by the normal builder.
 */
public final class SnakeBuilder {
    private SnakeBuilder() {}

    public static List<Integer> createOrder(List<SchematicData.BlockEntry> blocks) {
        if (blocks == null || blocks.isEmpty()) return List.of();

        int minY = blocks.stream().mapToInt(SchematicData.BlockEntry::y).min().orElse(0);
        int minZ = blocks.stream().mapToInt(SchematicData.BlockEntry::z).min().orElse(0);
        List<Integer> order = new ArrayList<>(blocks.size());
        for (int i = 0; i < blocks.size(); i++) order.add(i);

        order.sort(Comparator
                .comparingInt((Integer i) -> blocks.get(i).y())
                .thenComparingInt(i -> placementPriority(blocks.get(i).blockId()))
                .thenComparingInt(i -> blocks.get(i).z())
                .thenComparingInt(i -> {
                    SchematicData.BlockEntry entry = blocks.get(i);
                    boolean reverse = ((entry.y() - minY + entry.z() - minZ) & 1) != 0;
                    return reverse ? -entry.x() : entry.x();
                }));
        return List.copyOf(order);
    }

    private static int placementPriority(String blockId) {
        String path = blockId.substring(blockId.indexOf(':') + 1);
        if (path.contains("wall_torch") || path.contains("wall_sign") || path.contains("wall_hanging_sign")
                || path.contains("ladder") || path.contains("wall_banner") || path.contains("wall_head")
                || path.contains("wall_skull") || path.contains("button") || path.contains("lever")
                || path.contains("tripwire_hook") || path.contains("redstone_wire") || path.contains("repeater")
                || path.contains("comparator")) return 2;
        if (path.contains("door") || path.contains("trapdoor") || path.contains("fence_gate")
                || path.contains("stairs") || path.contains("slab") || path.contains("fence")
                || path.contains("pane") || path.contains("rail")) return 1;
        return 0;
    }
}
