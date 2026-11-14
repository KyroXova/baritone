package baritone.utils;

import baritone.api.utils.BetterBlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockInteractionTest {
    @Test
    public void acceptsHitsWithinTheSpecifiedBlock() {
        BetterBlockPos pos = new BetterBlockPos(-4, 64, 9);
        assertTrue(BlockInteraction.validHit(pos, ForgeDirection.UP, Vec3.createVectorHelper(-3.5, 65, 9.5)));
        assertTrue(BlockInteraction.validHit(pos, ForgeDirection.EAST, Vec3.createVectorHelper(-3, 64.5, 9.5)));
        assertFalse(BlockInteraction.validHit(pos, ForgeDirection.UP, Vec3.createVectorHelper(-2.9, 65, 9.5)));
        assertFalse(BlockInteraction.validHit(pos, ForgeDirection.UNKNOWN, Vec3.createVectorHelper(-3.5, 65, 9.5)));
        assertFalse(BlockInteraction.validHit(pos, ForgeDirection.UP, Vec3.createVectorHelper(Double.NaN, 65, 9.5)));
    }
}
