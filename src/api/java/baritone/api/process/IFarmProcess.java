/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.process;

import baritone.api.utils.BetterBlockPos;

public interface IFarmProcess extends IBaritoneProcess {

    /**
     * Begin to search for crops to farm with in specified aria
     * from specified location.
     *
     * @param range The distance from center to farm from
     * @param pos   The center position to base the range from
     */
    void farm(int range, BetterBlockPos pos);

    /**
     * Begin to search for nearby crops to farm.
     */
    default void farm() {farm(0, null);}

    /**
     * Begin to search for crops to farm with in specified aria
     * from the position the command was executed.
     *
     * @param range The distance to search for crops to farm
     */
    default void farm(int range) {farm(range, null);}
}
