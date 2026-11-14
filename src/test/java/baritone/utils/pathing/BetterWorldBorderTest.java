/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.utils.pathing;

import org.junit.Test;
import static org.junit.Assert.*;

public class BetterWorldBorderTest {
    @Test
    public void usesLegacyWorldCoordinateLimits() {
        BetterWorldBorder border = new BetterWorldBorder();
        assertTrue(border.entirelyContains(-30000000, 0));
        assertTrue(border.entirelyContains(29999999, 0));
        assertFalse(border.entirelyContains(-30000001, 0));
        assertFalse(border.entirelyContains(30000000, 0));
        assertFalse(border.entirelyContains(Integer.MAX_VALUE, 0));
        assertFalse(border.canPlaceAt(-30000000, 0));
        assertFalse(border.canPlaceAt(29999999, 0));
        assertTrue(border.canPlaceAt(29999998, 0));
    }
}
