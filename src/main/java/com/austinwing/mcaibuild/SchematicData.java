package com.austinwing.mcaibuild;

import java.util.List;

public record SchematicData(String name, int width, int height, int length, List<BlockEntry> blocks) {
    public record BlockEntry(int x, int y, int z, String blockId) {}
}