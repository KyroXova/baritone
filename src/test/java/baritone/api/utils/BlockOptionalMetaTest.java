/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.api.utils;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import org.junit.Test;

import static org.junit.Assert.*;

public class BlockOptionalMetaTest {

    private static class TestBlock extends Block {
        protected TestBlock() {
            super(Material.rock);
        }
    }

    @Test
    public void testBlockOptionalMetaAnyMeta() {
        Block testBlock = new TestBlock();
        Block otherBlock = new TestBlock();

        BlockOptionalMeta bom = new BlockOptionalMeta(testBlock);
        assertSame(testBlock, bom.getBlock());
        assertNull(bom.getMeta());

        assertTrue(bom.matches(testBlock));
        assertFalse(bom.matches(otherBlock));

        assertTrue(bom.matches(new BlockState(testBlock, 0)));
        assertTrue(bom.matches(new BlockState(testBlock, 5)));
        assertFalse(bom.matches(new BlockState(otherBlock, 0)));
    }

    @Test
    public void testBlockOptionalMetaSpecificMeta() {
        Block testBlock = new TestBlock();

        BlockOptionalMeta bom = new BlockOptionalMeta(testBlock, 14);
        assertSame(testBlock, bom.getBlock());
        assertEquals(Integer.valueOf(14), bom.getMeta());

        assertTrue(bom.matches(new BlockState(testBlock, 14)));
        assertFalse(bom.matches(new BlockState(testBlock, 0)));
    }

    @Test
    public void testBlockOptionalMetaLookup() {
        Block testBlockA = new TestBlock();
        Block testBlockB = new TestBlock();
        Block testBlockC = new TestBlock();

        BlockOptionalMeta bomA = new BlockOptionalMeta(testBlockA);
        BlockOptionalMeta bomB = new BlockOptionalMeta(testBlockB, 7);

        BlockOptionalMetaLookup lookup = new BlockOptionalMetaLookup(bomA, bomB);

        assertTrue(lookup.has(testBlockA));
        assertTrue(lookup.has(testBlockB));
        assertFalse(lookup.has(testBlockC));

        assertTrue(lookup.has(new BlockState(testBlockA, 0)));
        assertTrue(lookup.has(new BlockState(testBlockA, 3)));
        assertTrue(lookup.has(new BlockState(testBlockB, 7)));
        assertFalse(lookup.has(new BlockState(testBlockB, 0)));
    }
}
