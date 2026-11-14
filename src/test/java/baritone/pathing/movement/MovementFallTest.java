/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.movement;

import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.movements.MovementFall;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MovementFallTest {

    @Test
    public void testFallMovementProperties() {
        BetterBlockPos from = new BetterBlockPos(10, 64, 20);
        BetterBlockPos to = new BetterBlockPos(10, 60, 20);

        MovementFall fall = new MovementFall(null, from, to);

        assertEquals(from, fall.getSrc());
        assertEquals(to, fall.getDest());
        assertEquals(new BetterBlockPos(0, -4, 0), fall.getDirection());
        // diffY = 4, length = 6
        assertEquals(6, fall.toBreakAll().length);
        assertEquals(new BetterBlockPos(10, 65, 20), fall.toBreakAll()[0]);
        assertEquals(new BetterBlockPos(10, 64, 20), fall.toBreakAll()[1]);
        assertEquals(new BetterBlockPos(10, 63, 20), fall.toBreakAll()[2]);
        assertEquals(new BetterBlockPos(10, 62, 20), fall.toBreakAll()[3]);
        assertEquals(new BetterBlockPos(10, 61, 20), fall.toBreakAll()[4]);
        assertEquals(new BetterBlockPos(10, 60, 20), fall.toBreakAll()[5]);
    }
}
