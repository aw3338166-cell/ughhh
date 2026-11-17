package com.austinwing.mcaibuild;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectionBoundsTest {
    @Test
    void normalizesCornersAndUsesInclusiveDimensions() {
        SelectionBounds bounds = SelectionBounds.between(
                new BlockPos(8, 12, -2),
                new BlockPos(3, 7, 4));

        assertEquals(6, bounds.width());
        assertEquals(6, bounds.height());
        assertEquals(7, bounds.length());
        assertEquals(252, bounds.volume());
    }

    @Test
    void singleBlockSelectionHasVolumeOne() {
        SelectionBounds bounds = SelectionBounds.between(
                new BlockPos(-4, 64, 10),
                new BlockPos(-4, 64, 10));

        assertEquals(1, bounds.width());
        assertEquals(1, bounds.height());
        assertEquals(1, bounds.length());
        assertEquals(1, bounds.volume());
    }
}