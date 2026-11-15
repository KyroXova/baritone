package baritone.pathing.movement;

import baritone.api.utils.BlockState;
import baritone.pathing.precompute.Ternary;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import org.junit.Test;

import static org.junit.Assert.*;

public class TerrainHazardTest {
    private static class QuicksandBlock extends Block {
        QuicksandBlock() { super(Material.sand); }
    }

    @Test
    public void quicksandIsNeitherWalkableNorPassable() {
        BlockState state = new BlockState(new QuicksandBlock(), 0);
        assertTrue(LegacyBlockRules.isTerrainHazard(state));
        assertEquals(Ternary.NO, LegacyMovementRules.canWalkOnBlockState(state));
        assertEquals(Ternary.NO, LegacyMovementRules.canWalkThroughBlockState(state));
    }

    @Test
    public void biomesOPlentyHazardsUseMetadata() {
        assertEquals(Boolean.TRUE, LegacyBlockRules.metadataHazard("biomesoplenty.common.blocks.BlockMud", 1));
        assertEquals(Boolean.FALSE, LegacyBlockRules.metadataHazard("biomesoplenty.common.blocks.BlockMud", 0));
        assertEquals(Boolean.TRUE, LegacyBlockRules.metadataHazard("biomesoplenty.common.blocks.BlockBOPPlant", 5));
        assertEquals(Boolean.FALSE, LegacyBlockRules.metadataHazard("biomesoplenty.common.blocks.BlockBOPPlant", 0));
        assertTrue(LegacyBlockRules.hazardName("example:berrybush"));
        assertFalse(LegacyBlockRules.hazardName("example:blueberry_juice"));
    }
}
