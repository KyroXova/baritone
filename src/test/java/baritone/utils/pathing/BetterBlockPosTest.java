/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.utils.pathing;

import baritone.api.utils.BetterBlockPos;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import static org.junit.Assert.*;

public class BetterBlockPosTest {
    @Test
    public void offsetsFollowForgeDirections() {
        BetterBlockPos pos = new BetterBlockPos(1, 2, 3);
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            for (int distance = -10; distance <= 10; distance++) {
                BetterBlockPos expected = new BetterBlockPos(
                        1 + direction.offsetX * distance,
                        2 + direction.offsetY * distance,
                        3 + direction.offsetZ * distance);
                assertEquals(expected, pos.relative(direction, distance));
            }
        }
        assertSame(pos, pos.relative(null, 0));
        assertEquals(new BetterBlockPos(1, 3, 3), pos.above());
        assertEquals(new BetterBlockPos(1, 1, 3), pos.below());
    }

    @Test
    public void vectorArithmeticAndCrossProduct() {
        BetterBlockPos a = new BetterBlockPos(1, 2, 3);
        BetterBlockPos b = new BetterBlockPos(4, 5, 6);
        assertEquals(new BetterBlockPos(5, 7, 9), a.add(b));
        assertEquals(new BetterBlockPos(5, 7, 9), a.offset(b));
        assertEquals(new BetterBlockPos(-3, -3, -3), a.subtract(b));
        assertEquals(new BetterBlockPos(3, 3, 3), b.subtract(a));
        assertEquals(a, a.add(null));
        assertEquals(a, a.subtract(null));

        // Cross product of (1, 0, 0) and (0, 1, 0) is (0, 0, 1)
        BetterBlockPos x = new BetterBlockPos(1, 0, 0);
        BetterBlockPos y = new BetterBlockPos(0, 1, 0);
        assertEquals(new BetterBlockPos(0, 0, 1), x.cross(y));
        assertEquals(new BetterBlockPos(0, 0, -1), y.cross(x));
        assertEquals(BetterBlockPos.ORIGIN, x.cross(x));
    }

    @Test
    public void serializationAndFloorHandleNegativeCoordinates() {
        BetterBlockPos pos = new BetterBlockPos(-12.1, -0.1, 25.9);
        assertEquals(new BetterBlockPos(-13, -1, 25), pos);
        assertEquals(pos, BetterBlockPos.deserializeFromLong(
                BetterBlockPos.serializeToLong(pos.x, pos.y, pos.z)));
        assertNotEquals(pos, new Object());
        assertEquals(pos.hashCode(), new BetterBlockPos(pos).hashCode());
    }
}
