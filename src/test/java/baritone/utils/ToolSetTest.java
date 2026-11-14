/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ToolSetTest {
    @Test
    public void estimatesLegacyMiningProgress() {
        assertEquals(1.0 / 150.0, ToolSet.miningProgress(1, 0, false, 1.5F), 1e-8);
        assertEquals(6.0 / 45.0, ToolSet.miningProgress(6, 0, true, 1.5F), 1e-8);
        assertEquals(11.0 / 45.0, ToolSet.miningProgress(6, 2, true, 1.5F), 1e-8);
        assertEquals(1.0 / 45.0, ToolSet.miningProgress(1, 5, true, 1.5F), 1e-8);
        assertEquals(-1, ToolSet.miningProgress(6, 0, true, -1), 0);
        assertTrue(Double.isInfinite(ToolSet.miningProgress(1, 0, true, 0)));
    }
}
