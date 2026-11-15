package baritone.utils;

import org.junit.Test;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

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

    @Test
    public void targetLockRejectsAdjacentPuzzleBlocks() {
        BetterBlockPos plant = new BetterBlockPos(10, 20, 30);
        assertTrue(BlockBreakHelper.matchesLockedTarget(plant,
                new MovingObjectPosition(10, 20, 30, 1, Vec3.createVectorHelper(10.5, 21, 30.5))));
        assertFalse(BlockBreakHelper.matchesLockedTarget(plant,
                new MovingObjectPosition(10, 19, 30, 1, Vec3.createVectorHelper(10.5, 20, 30.5))));
        assertFalse(BlockBreakHelper.matchesLockedTarget(plant, null));
    }
}
