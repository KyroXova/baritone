/* This file is part of Baritone, distributed under the LGPL-3.0-or-later. */
package baritone.utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class WorldRenderOriginTest {
    @Test
    public void interpolatesTheCameraForTheCurrentFrame() {
        assertEquals(-120.0, WorldRenderOrigin.interpolate(-120.0, -116.0, 0.0F), 0.0);
        assertEquals(-119.0, WorldRenderOrigin.interpolate(-120.0, -116.0, 0.25F), 0.0);
        assertEquals(-116.0, WorldRenderOrigin.interpolate(-120.0, -116.0, 1.0F), 0.0);
    }
}
