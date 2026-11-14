/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.schematic;

import baritone.api.schematic.*;
import baritone.api.schematic.mask.shape.CylinderMask;
import baritone.api.utils.BlockOptionalMeta;
import baritone.api.utils.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class SchematicTest {

    private static class TestBlock extends Block {
        protected TestBlock() {
            super(Material.rock);
        }
    }

    private static class SimpleStaticSchematic implements IStaticSchematic {
        private final int w, h, l;
        private final BlockState state;

        SimpleStaticSchematic(int w, int h, int l, BlockState state) {
            this.w = w;
            this.h = h;
            this.l = l;
            this.state = state;
        }

        @Override public BlockState getDirect(int x, int y, int z) { return state; }
        @Override public BlockState desiredState(int x, int y, int z, BlockState current, List<BlockState> approxPlaceable) { return state; }
        @Override public int widthX() { return w; }
        @Override public int heightY() { return h; }
        @Override public int lengthZ() { return l; }
    }

    @Test
    public void testFillSchematic() {
        Block blockA = new TestBlock();
        FillSchematic fill = new FillSchematic(4, 5, 6, new BlockOptionalMeta(blockA, 2));

        assertEquals(4, fill.widthX());
        assertEquals(5, fill.heightY());
        assertEquals(6, fill.lengthZ());

        assertTrue(fill.inSchematic(0, 0, 0, null));
        assertTrue(fill.inSchematic(3, 4, 5, null));
        assertFalse(fill.inSchematic(4, 0, 0, null));

        BlockState desired = fill.desiredState(1, 1, 1, null, Collections.emptyList());
        assertEquals(blockA, desired.getBlock());
        assertEquals(2, desired.getMetadata());
    }

    @Test
    public void testWallsSchematic() {
        Block block = new TestBlock();
        SimpleStaticSchematic base = new SimpleStaticSchematic(5, 5, 5, new BlockState(block, 0));
        WallsSchematic walls = new WallsSchematic(base);

        // Border blocks are in schematic
        assertTrue(walls.inSchematic(0, 2, 2, null));
        assertTrue(walls.inSchematic(4, 2, 2, null));
        assertTrue(walls.inSchematic(2, 2, 0, null));
        assertTrue(walls.inSchematic(2, 2, 4, null));

        // Center interior block is NOT in walls schematic
        assertFalse(walls.inSchematic(2, 2, 2, null));
    }

    @Test
    public void testShellSchematic() {
        Block block = new TestBlock();
        SimpleStaticSchematic base = new SimpleStaticSchematic(5, 5, 5, new BlockState(block, 0));
        ShellSchematic shell = new ShellSchematic(base);

        // Outer surface blocks are in shell
        assertTrue(shell.inSchematic(0, 2, 2, null));
        assertTrue(shell.inSchematic(2, 0, 2, null));
        assertTrue(shell.inSchematic(2, 4, 2, null));

        // Interior block is not
        assertFalse(shell.inSchematic(2, 2, 2, null));
    }

    @Test
    public void testSubstituteSchematic() {
        Block original = new TestBlock();
        Block substitute = new TestBlock();

        SimpleStaticSchematic base = new SimpleStaticSchematic(3, 3, 3, new BlockState(original, 5));

        Map<Block, List<Block>> subs = new HashMap<>();
        subs.put(original, Collections.singletonList(substitute));

        SubstituteSchematic subSchematic = new SubstituteSchematic(base, subs);

        BlockState desired = subSchematic.desiredState(1, 1, 1, null, Collections.emptyList());
        assertNotNull(desired);
        assertEquals(substitute, desired.getBlock());
        assertEquals(5, desired.getMetadata());
    }

    @Test
    public void testCylinderMask() {
        CylinderMask mask = new CylinderMask(10, 10, 10, true, Axis.Y);
        assertEquals(10, mask.widthX());
        assertEquals(10, mask.heightY());
        assertEquals(10, mask.lengthZ());

        // Center should be part of mask
        assertTrue(mask.partOfMask(5, 5, 5));
        // Corners should not be part of circular mask
        assertFalse(mask.partOfMask(0, 5, 0));
    }
}
