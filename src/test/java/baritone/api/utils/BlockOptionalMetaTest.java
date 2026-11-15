/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.api.utils;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import java.util.Random;
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
        assertTrue(lookup.has(new BlockState(testBlockA, 146)));
        assertTrue(lookup.has(new BlockState(testBlockA, 1024)));
        assertTrue(lookup.has(new BlockState(testBlockB, 7)));
        assertFalse(lookup.has(new BlockState(testBlockB, 0)));
        assertFalse(lookup.has(new BlockState(testBlockB, 146)));
    }

    @Test
    public void testExtendedMetadataBlockState() {
        Block testBlock = new TestBlock();
        BlockState extended = new BlockState(testBlock, 146);
        assertEquals(146, extended.getMetadata());
        assertSame(testBlock, extended.getBlock());

        BlockOptionalMeta bomSpecific = new BlockOptionalMeta(testBlock, 146);
        assertTrue(bomSpecific.matches(extended));
        assertFalse(bomSpecific.matches(new BlockState(testBlock, 0)));

        BlockOptionalMeta bomAny = new BlockOptionalMeta(testBlock);
        assertTrue(bomAny.matches(extended));
    }

    @Test
    public void testStackHashes() {
        Block testBlock = new TestBlock();
        BlockOptionalMeta bomAny = new BlockOptionalMeta(testBlock);
        assertNotNull(bomAny.stackHashes());

        BlockOptionalMeta bomMeta = new BlockOptionalMeta(testBlock, 3);
        assertNotNull(bomMeta.stackHashes());
    }

    @Test
    public void matchesDroppedItemInsteadOfOnlyBlockItem() {
        final Item drop = new Item();
        Block ore = new TestBlock() {
            @Override
            public Item getItemDropped(int metadata, Random random, int fortune) {
                return drop;
            }
        };
        BlockOptionalMetaLookup lookup = new BlockOptionalMetaLookup(new BlockOptionalMeta(ore));
        assertTrue(lookup.has(new ItemStack(drop, 1, 0)));
        assertFalse(lookup.has(new ItemStack(new Item(), 1, 0)));
    }

    @Test
    public void parsesNamespacedAndUnqualifiedMetadataSelectors() {
        assertArrayEquals(new String[] {"stone", "3"}, BlockOptionalMeta.parseSelector("stone:3"));
        assertArrayEquals(new String[] {"minecraft:stone", "3"}, BlockOptionalMeta.parseSelector("minecraft:stone[3]"));
        assertArrayEquals(new String[] {"minecraft:stone", "3"}, BlockOptionalMeta.parseSelector("minecraft:stone:3"));
    }
}
