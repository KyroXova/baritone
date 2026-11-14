/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.movement;

import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.movements.MovementDescend;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MovementDescendTest {

    @Test
    public void testDescendMovementProperties() {
        BetterBlockPos from = new BetterBlockPos(10, 64, 20);
        BetterBlockPos to = new BetterBlockPos(11, 63, 20);

        MovementDescend descend = new MovementDescend(null, from, to);

        assertEquals(from, descend.getSrc());
        assertEquals(to, descend.getDest());
        assertEquals(new BetterBlockPos(1, -1, 0), descend.getDirection());
        assertEquals(new BetterBlockPos(11, 62, 20), descend.positionToPlace);
        assertEquals(3, descend.toBreakAll().length);
        assertEquals(to.above(2), descend.toBreakAll()[0]);
        assertEquals(to.above(), descend.toBreakAll()[1]);
        assertEquals(to, descend.toBreakAll()[2]);
    }
}
