/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.movement;

import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.movements.MovementDiagonal;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MovementDiagonalTest {

    @Test
    public void testDiagonalMovementProperties() {
        BetterBlockPos from = new BetterBlockPos(10, 64, 20);
        MovementDiagonal diagonal = new MovementDiagonal(null, from, ForgeDirection.EAST, ForgeDirection.SOUTH, 0);

        BetterBlockPos expectedDest = new BetterBlockPos(11, 64, 21);
        assertEquals(from, diagonal.getSrc());
        assertEquals(expectedDest, diagonal.getDest());
        assertEquals(new BetterBlockPos(1, 0, 1), diagonal.getDirection());
        assertEquals(6, diagonal.toBreakAll().length);
        assertEquals(new BetterBlockPos(11, 64, 20), diagonal.toBreakAll()[0]);
        assertEquals(new BetterBlockPos(11, 65, 20), diagonal.toBreakAll()[1]);
        assertEquals(new BetterBlockPos(10, 64, 21), diagonal.toBreakAll()[2]);
        assertEquals(new BetterBlockPos(10, 65, 21), diagonal.toBreakAll()[3]);
        assertEquals(expectedDest, diagonal.toBreakAll()[4]);
        assertEquals(expectedDest.above(), diagonal.toBreakAll()[5]);
    }
}
