package com.austinwing.mcaibuild;

import java.util.List;
import java.util.Map;

public record SchematicData(String name, int width, int height, int length, List<BlockEntry> blocks) {
    public record BlockEntry(int x, int y, int z, String blockId, Map<String, String> properties) {
        public BlockEntry(int x, int y, int z, String blockId) { this(x, y, z, blockId, Map.of()); }
        public BlockEntry { properties = properties == null ? Map.of() : Map.copyOf(properties); }
    }
}
