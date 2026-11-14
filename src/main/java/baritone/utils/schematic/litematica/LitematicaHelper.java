/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils.schematic.litematica;

import baritone.api.schematic.IStaticSchematic;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Pair;

/**
 * Helper class for Litematica schematics (not available on Forge 1.7.10).
 */
public final class LitematicaHelper {

    public static boolean isLitematicaPresent() {
        return false;
    }

    public static boolean hasLoadedSchematic(int i) {
        return false;
    }

    public static Pair<IStaticSchematic, BetterBlockPos> getSchematic(int i) {
        return null;
    }
}
