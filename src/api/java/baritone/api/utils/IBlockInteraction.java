/* This file is part of Baritone, distributed under the LGPL-3.0-or-later. */
package baritone.api.utils;

import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

/** One-shot interactions; callers own retries, timing, and server acknowledgments. */
public interface IBlockInteraction {
    enum Result {
        /** The client controller was invoked; this does not confirm server acceptance. */
        ATTEMPTED,
        UNAVAILABLE,
        WRONG_THREAD,
        INVALID_TARGET,
        OUT_OF_REACH,
        OBSTRUCTED
    }

    /**
     * Use a specific block face once. The hit vector is in world coordinates.
     * When requireLineOfSight is true, the requested block and face must be the
     * first block hit by a ray from the player's eyes.
     */
    Result useBlock(BetterBlockPos pos, ForgeDirection face, Vec3 hit, boolean sneak, boolean requireLineOfSight);

    /** Start a single left click against a block; repeated calls are not a mining operation. */
    Result attackBlock(BetterBlockPos pos, ForgeDirection face, Vec3 hit, boolean requireLineOfSight);
}
