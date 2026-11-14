/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.utils.schematic;

import baritone.api.schematic.ISchematic;
import baritone.api.schematic.MaskSchematic;
import baritone.api.selection.ISelection;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockState;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.stream.Stream;

public class SelectionSchematic extends MaskSchematic {

    private final ISelection[] selections;

    public SelectionSchematic(ISchematic schematic, BetterBlockPos origin, ISelection[] selections) {
        super(schematic);
        this.selections = Stream.of(selections).map(
                        sel -> sel
                                .shift(ForgeDirection.WEST, origin.x)
                                .shift(ForgeDirection.DOWN, origin.y)
                                .shift(ForgeDirection.NORTH, origin.z))
                .toArray(ISelection[]::new);
    }

    @Override
    protected boolean partOfMask(int x, int y, int z, BlockState currentState) {
        for (ISelection selection : selections) {
            if (x >= selection.min().x && y >= selection.min().y && z >= selection.min().z
                    && x <= selection.max().x && y <= selection.max().y && z <= selection.max().z) {
                return true;
            }
        }
        return false;
    }
}
