/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.movement;

import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.movements.MovementAscend;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MovementAscendTest {

    @Test
    public void testAscendMovementProperties() {
        BetterBlockPos from = new BetterBlockPos(10, 64, 20);
        BetterBlockPos to = new BetterBlockPos(11, 65, 20);

        MovementAscend ascend = new MovementAscend(null, from, to);

        assertEquals(from, ascend.getSrc());
        assertEquals(to, ascend.getDest());
        assertEquals(new BetterBlockPos(1, 1, 0), ascend.getDirection());
        assertEquals(new BetterBlockPos(11, 64, 20), ascend.positionToPlace);
        assertEquals(3, ascend.toBreakAll().length);
        assertEquals(to, ascend.toBreakAll()[0]);
        assertEquals(from.above(2), ascend.toBreakAll()[1]);
        assertEquals(to.above(), ascend.toBreakAll()[2]);
    }
}
