/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.movement;

import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.movements.MovementDownward;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MovementDownwardTest {

    @Test
    public void testDownwardMovementProperties() {
        BetterBlockPos from = new BetterBlockPos(10, 64, 20);
        BetterBlockPos to = new BetterBlockPos(10, 63, 20);

        MovementDownward downward = new MovementDownward(null, from, to);

        assertEquals(from, downward.getSrc());
        assertEquals(to, downward.getDest());
        assertEquals(new BetterBlockPos(0, -1, 0), downward.getDirection());
        assertEquals(1, downward.toBreakAll().length);
        assertEquals(to, downward.toBreakAll()[0]);
    }
}
