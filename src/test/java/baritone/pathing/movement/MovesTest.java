/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.movement;

import baritone.api.utils.BetterBlockPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MovesTest {

    @Test
    public void testMoveEnumCountAndStaticOffsets() {
        assertEquals(22, Moves.values().length);

        assertEquals(0, Moves.DOWNWARD.xOffset);
        assertEquals(-1, Moves.DOWNWARD.yOffset);
        assertEquals(0, Moves.DOWNWARD.zOffset);

        assertEquals(0, Moves.PILLAR.xOffset);
        assertEquals(1, Moves.PILLAR.yOffset);
        assertEquals(0, Moves.PILLAR.zOffset);

        assertEquals(0, Moves.TRAVERSE_NORTH.xOffset);
        assertEquals(0, Moves.TRAVERSE_NORTH.yOffset);
        assertEquals(-1, Moves.TRAVERSE_NORTH.zOffset);

        assertEquals(1, Moves.TRAVERSE_EAST.xOffset);
        assertEquals(0, Moves.TRAVERSE_EAST.yOffset);
        assertEquals(0, Moves.TRAVERSE_EAST.zOffset);
    }
}
