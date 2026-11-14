/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.movement;

import baritone.api.utils.BetterBlockPos;
import baritone.pathing.movement.movements.MovementPillar;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MovementPillarTest {

    @Test
    public void testPillarMovementProperties() {
        BetterBlockPos from = new BetterBlockPos(10, 64, 20);
        BetterBlockPos to = new BetterBlockPos(10, 65, 20);

        MovementPillar pillar = new MovementPillar(null, from, to);

        assertEquals(from, pillar.getSrc());
        assertEquals(to, pillar.getDest());
        assertEquals(new BetterBlockPos(0, 1, 0), pillar.getDirection());
        assertEquals(from, pillar.positionToPlace);
        assertEquals(1, pillar.toBreakAll().length);
        assertEquals(from.above(2), pillar.toBreakAll()[0]);
    }
}
