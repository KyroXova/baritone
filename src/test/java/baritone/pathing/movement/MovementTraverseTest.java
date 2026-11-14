/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.movement;

import baritone.api.IBaritone;
import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.movements.MovementTraverse;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MovementTraverseTest {

    @Test
    public void testTraverseMovementProperties() {
        BetterBlockPos from = new BetterBlockPos(10, 64, 20);
        BetterBlockPos to = new BetterBlockPos(11, 64, 20);

        MovementTraverse traverse = new MovementTraverse(null, from, to);

        assertEquals(from, traverse.getSrc());
        assertEquals(to, traverse.getDest());
        assertEquals(new BetterBlockPos(1, 0, 0), traverse.getDirection());
        assertEquals(new BetterBlockPos(11, 63, 20), traverse.positionToPlace);
        assertEquals(2, traverse.toBreakAll().length);
        assertEquals(to.above(), traverse.toBreakAll()[0]);
        assertEquals(to, traverse.toBreakAll()[1]);
    }
}
