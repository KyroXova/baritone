/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.schematic;

import baritone.api.schematic.Axis;
import baritone.api.schematic.ISchematic;
import baritone.api.schematic.Mirror;
import baritone.api.schematic.MirroredSchematic;
import baritone.api.schematic.RotatedSchematic;
import baritone.api.schematic.Rotation;
import baritone.api.utils.BlockState;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class RotatedSchematicTest {

    private static class DummySchematic implements ISchematic {
        private final int width;
        private final int height;
        private final int length;

        DummySchematic(int width, int height, int length) {
            this.width = width;
            this.height = height;
            this.length = length;
        }

        @Override
        public BlockState desiredState(int x, int y, int z, BlockState current, List<BlockState> approxPlaceable) {
            return null;
        }

        @Override
        public int widthX() { return width; }

        @Override
        public int heightY() { return height; }

        @Override
        public int lengthZ() { return length; }
    }

    @Test
    public void testRotatedDimensions() {
        DummySchematic base = new DummySchematic(3, 4, 5);

        RotatedSchematic rot0 = new RotatedSchematic(base, Rotation.NONE);
        assertEquals(3, rot0.widthX());
        assertEquals(4, rot0.heightY());
        assertEquals(5, rot0.lengthZ());

        RotatedSchematic rot90 = new RotatedSchematic(base, Rotation.CLOCKWISE_90);
        assertEquals(5, rot90.widthX());
        assertEquals(4, rot90.heightY());
        assertEquals(3, rot90.lengthZ());

        RotatedSchematic rot180 = new RotatedSchematic(base, Rotation.CLOCKWISE_180);
        assertEquals(3, rot180.widthX());
        assertEquals(4, rot180.heightY());
        assertEquals(5, rot180.lengthZ());

        RotatedSchematic rot270 = new RotatedSchematic(base, Rotation.COUNTERCLOCKWISE_90);
        assertEquals(5, rot270.widthX());
        assertEquals(4, rot270.heightY());
        assertEquals(3, rot270.lengthZ());
    }

    @Test
    public void testMirroredDimensions() {
        DummySchematic base = new DummySchematic(3, 4, 5);

        MirroredSchematic mirrorLR = new MirroredSchematic(base, Mirror.LEFT_RIGHT);
        assertEquals(3, mirrorLR.widthX());
        assertEquals(4, mirrorLR.heightY());
        assertEquals(5, mirrorLR.lengthZ());

        MirroredSchematic mirrorFB = new MirroredSchematic(base, Mirror.FRONT_BACK);
        assertEquals(3, mirrorFB.widthX());
        assertEquals(4, mirrorFB.heightY());
        assertEquals(5, mirrorFB.lengthZ());
    }

    @Test
    public void testAxisSizes() {
        DummySchematic base = new DummySchematic(3, 4, 5);
        assertEquals(3, base.size(Axis.X));
        assertEquals(4, base.size(Axis.Y));
        assertEquals(5, base.size(Axis.Z));
    }
}
