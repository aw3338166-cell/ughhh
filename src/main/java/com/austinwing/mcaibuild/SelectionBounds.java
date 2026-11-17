package com.austinwing.mcaibuild;

import net.minecraft.util.math.BlockPos;

/** Inclusive, normalized bounds for a two-corner world selection. */
public record SelectionBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public static SelectionBounds between(BlockPos first, BlockPos second) {
        return new SelectionBounds(
                Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ()),
                Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ()));
    }

    public int width() { return maxX - minX + 1; }
    public int height() { return maxY - minY + 1; }
    public int length() { return maxZ - minZ + 1; }
    public long volume() { return (long) width() * height() * length(); }
}