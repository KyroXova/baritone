/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.path;

import baritone.api.utils.BetterBlockPos;
import baritone.utils.pathing.Avoidance;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AvoidanceTest {

    @Test
    public void testAvoidanceCoefficients() {
        BetterBlockPos center = new BetterBlockPos(10, 64, 10);
        Avoidance avoidance = new Avoidance(center, 2.5D, 3);

        // Center should return 2.5
        assertEquals(2.5D, avoidance.coefficient(10, 64, 10), 0.0001D);

        // Within radius (distance 2)
        assertEquals(2.5D, avoidance.coefficient(10, 66, 10), 0.0001D);

        // Outside radius (distance 4)
        assertEquals(1.0D, avoidance.coefficient(10, 68, 10), 0.0001D);
        assertEquals(1.0D, avoidance.coefficient(14, 64, 10), 0.0001D);
    }

    @Test
    public void testApplySpherical() {
        BetterBlockPos center = new BetterBlockPos(0, 0, 0);
        Avoidance avoidance = new Avoidance(center, 3.0D, 1);

        Long2DoubleOpenHashMap map = new Long2DoubleOpenHashMap();
        map.defaultReturnValue(1.0D);

        long centerHash = BetterBlockPos.longHash(0, 0, 0);
        long outsideHash = BetterBlockPos.longHash(5, 5, 5);

        avoidance.applySpherical(map);

        assertEquals(3.0D, map.get(centerHash), 0.0001D);
        assertEquals(1.0D, map.get(outsideHash), 0.0001D);
    }
}
