/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.movement;

import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.movements.MovementParkour;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MovementParkourTest {

    @Test
    public void testParkourMovementProperties() {
        BetterBlockPos from = new BetterBlockPos(10, 64, 20);

        MovementParkour parkour = new MovementParkour(null, from, 3, ForgeDirection.EAST, false);

        BetterBlockPos expectedDest = new BetterBlockPos(13, 64, 20);
        assertEquals(from, parkour.getSrc());
        assertEquals(expectedDest, parkour.getDest());
        assertEquals(new BetterBlockPos(3, 0, 0), parkour.getDirection());
        assertEquals(new BetterBlockPos(13, 63, 20), parkour.positionToPlace);
        assertEquals(0, parkour.toBreakAll().length);
    }
}
