package baritone.utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockBreakHelperTest {
    @Test
    public void onlySuppressesVanillaResetForAutomatedMining() {
        assertTrue(BlockBreakHelper.shouldPreventVanillaReset(false, true));
        assertFalse(BlockBreakHelper.shouldPreventVanillaReset(false, false));
        assertFalse(BlockBreakHelper.shouldPreventVanillaReset(true, false));
        assertFalse(BlockBreakHelper.shouldPreventVanillaReset(true, true));
    }
}
