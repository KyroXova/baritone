/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.api.utils;

import net.minecraft.util.Vec3;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class VecUtilsTest {
    @Test
    public void centerAndDistanceHandleNegativeCoordinates() {
        BetterBlockPos pos = new BetterBlockPos(-2, 10, 3);
        Vec3 center = VecUtils.getBlockPosCenter(pos);
        assertEquals(-1.5, center.xCoord, 0);
        assertEquals(10.5, center.yCoord, 0);
        assertEquals(3.5, center.zCoord, 0);
        assertEquals(5, VecUtils.distanceToCenter(pos, 1.5, 14.5, 3.5), 0);
    }
}
