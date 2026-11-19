/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.movement;

import baritone.api.utils.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import static org.junit.Assert.*;

public class MovementHelperTest {

    @Test
    public void lilyPadsAreObstaclesForSwimmers() {
        assertEquals(baritone.pathing.precompute.Ternary.NO,
                LegacyMovementRules.canWalkThroughBlockState(new BlockState(new net.minecraft.block.BlockLilyPad() {}, 0)));
    }

    @Test
    public void testBlockClassification() {
        Block rock = new Block(Material.rock) {};
        Block air = new Block(Material.air) {};
        Block water = new Block(Material.water) {};
        Block lava = new Block(Material.lava) {};

        assertTrue(MovementHelper.isTransparent(air));
        assertTrue(MovementHelper.isTransparent(water));
        assertTrue(MovementHelper.isTransparent(lava));
        assertFalse(MovementHelper.isTransparent(rock));
    }

    @Test
    public void testLiquidClassification() {
        Block rock = new Block(Material.rock) {};
        Block water = new Block(Material.water) {};
        Block lava = new Block(Material.lava) {};

        BlockState waterState = new BlockState(water, 0);
        BlockState lavaState = new BlockState(lava, 0);
        BlockState rockState = new BlockState(rock, 0);

        assertTrue(MovementHelper.isLiquid(waterState));
        assertTrue(MovementHelper.isLiquid(lavaState));
        assertFalse(MovementHelper.isLiquid(rockState));
        assertTrue(MovementHelper.isLava(lavaState));
        assertFalse(MovementHelper.isLava(waterState));
    }

    @Test
    public void testMovementDirections() {
        assertEquals(5, Movement.HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP.length);
        for (ForgeDirection dir : Movement.HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP) {
            assertNotEquals(ForgeDirection.UP, dir);
            assertNotEquals(ForgeDirection.UNKNOWN, dir);
        }
    }
}
