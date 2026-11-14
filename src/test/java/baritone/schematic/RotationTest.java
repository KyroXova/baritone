/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.schematic;

import baritone.api.schematic.Mirror;
import baritone.api.schematic.Rotation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RotationTest {

    @Test
    public void testRotationTransitions() {
        assertEquals(4, Rotation.values().length);

        assertEquals(Rotation.CLOCKWISE_90, Rotation.NONE.getRotated(Rotation.CLOCKWISE_90));
        assertEquals(Rotation.CLOCKWISE_180, Rotation.CLOCKWISE_90.getRotated(Rotation.CLOCKWISE_90));
        assertEquals(Rotation.COUNTERCLOCKWISE_90, Rotation.CLOCKWISE_180.getRotated(Rotation.CLOCKWISE_90));
        assertEquals(Rotation.NONE, Rotation.COUNTERCLOCKWISE_90.getRotated(Rotation.CLOCKWISE_90));

        assertEquals(Rotation.CLOCKWISE_180, Rotation.NONE.getRotated(Rotation.CLOCKWISE_180));
        assertEquals(Rotation.COUNTERCLOCKWISE_90, Rotation.CLOCKWISE_90.getRotated(Rotation.CLOCKWISE_180));
        assertEquals(Rotation.NONE, Rotation.CLOCKWISE_180.getRotated(Rotation.CLOCKWISE_180));
        assertEquals(Rotation.CLOCKWISE_90, Rotation.COUNTERCLOCKWISE_90.getRotated(Rotation.CLOCKWISE_180));
    }

    @Test
    public void testMirrorValues() {
        assertEquals(3, Mirror.values().length);
        assertEquals(Mirror.NONE, Mirror.valueOf("NONE"));
        assertEquals(Mirror.LEFT_RIGHT, Mirror.valueOf("LEFT_RIGHT"));
        assertEquals(Mirror.FRONT_BACK, Mirror.valueOf("FRONT_BACK"));
    }
}
