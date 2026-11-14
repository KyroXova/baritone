/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.process;

import baritone.api.pathing.goals.Goal;
import baritone.api.utils.BetterBlockPos;

import java.util.List;

public interface IElytraProcess extends IBaritoneProcess {

    void repackChunks();

    /**
     * @return Where it is currently flying to, null if not active
     */
    BetterBlockPos currentDestination();

    /**
     * @return Current active path, empty if not active or no path has been calculated yet
     */
    List<BetterBlockPos> getPath();

    void pathTo(BetterBlockPos destination);

    void pathTo(Goal destination);

    /**
     * Resets the state of the process but will maintain the same destination and will try to keep flying
     */
    void resetState();

    /**
     * @return {@code true} if the native library loaded and elytra is actually usable
     */
    boolean isLoaded();

    default boolean isSafeToCancel() {
        return true;
    }
}
