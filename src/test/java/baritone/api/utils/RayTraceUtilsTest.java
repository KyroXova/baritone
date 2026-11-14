/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.api.utils;

import net.minecraft.util.Vec3;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RayTraceUtilsTest {
    @Test
    public void lookDirectionMatchesMinecraftAngles() {
        Vec3 south = RayTraceUtils.lookDirection(new Rotation(0, 0));
        assertEquals(0, south.xCoord, 0.0002);
        assertEquals(0, south.yCoord, 0.0002);
        assertEquals(1, south.zCoord, 0.0002);

        Vec3 west = RayTraceUtils.lookDirection(new Rotation(90, 0));
        assertEquals(-1, west.xCoord, 0.0002);
        assertEquals(0, west.zCoord, 0.0002);

        Vec3 down = RayTraceUtils.lookDirection(new Rotation(0, 90));
        assertEquals(-1, down.yCoord, 0.0002);
    }
}
