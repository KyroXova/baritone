/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.selection;

import baritone.api.selection.ISelection;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

public class Selection implements ISelection {

    private final BetterBlockPos pos1;
    private final BetterBlockPos pos2;
    private final BetterBlockPos min;
    private final BetterBlockPos max;
    private final BetterBlockPos size;
    private final AxisAlignedBB aabb;

    public Selection(BetterBlockPos pos1, BetterBlockPos pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;

        this.min = new BetterBlockPos(
                Math.min(pos1.x, pos2.x),
                Math.min(pos1.y, pos2.y),
                Math.min(pos1.z, pos2.z)
        );

        this.max = new BetterBlockPos(
                Math.max(pos1.x, pos2.x),
                Math.max(pos1.y, pos2.y),
                Math.max(pos1.z, pos2.z)
        );

        this.size = new BetterBlockPos(
                max.x - min.x + 1,
                max.y - min.y + 1,
                max.z - min.z + 1
        );

        this.aabb = AxisAlignedBB.getBoundingBox(min.x, min.y, min.z, max.x + 1, max.y + 1, max.z + 1);
    }

    @Override
    public BetterBlockPos pos1() {
        return pos1;
    }

    @Override
    public BetterBlockPos pos2() {
        return pos2;
    }

    @Override
    public BetterBlockPos min() {
        return min;
    }

    @Override
    public BetterBlockPos max() {
        return max;
    }

    @Override
    public BetterBlockPos size() {
        return size;
    }

    @Override
    public AxisAlignedBB aabb() {
        return aabb;
    }

    @Override
    public int hashCode() {
        return pos1.hashCode() ^ pos2.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ISelection)) return false;
        ISelection s = (ISelection) other;
        return pos1.equals(s.pos1()) && pos2.equals(s.pos2());
    }

    @Override
    public String toString() {
        return String.format("Selection{pos1=%s,pos2=%s}", pos1, pos2);
    }

    private boolean isPos2(ForgeDirection facing) {
        switch (facing) {
            case EAST:
                return pos2.x > pos1.x;
            case WEST:
                return pos2.x < pos1.x;
            case UP:
                return pos2.y > pos1.y;
            case DOWN:
                return pos2.y < pos1.y;
            case SOUTH:
                return pos2.z > pos1.z;
            case NORTH:
                return pos2.z < pos1.z;
            default:
                throw new IllegalStateException("Bad direction " + facing);
        }
    }

    @Override
    public ISelection expand(ForgeDirection direction, int blocks) {
        if (isPos2(direction)) {
            return new Selection(pos1, pos2.relative(direction, blocks));
        } else {
            return new Selection(pos1.relative(direction, blocks), pos2);
        }
    }

    @Override
    public ISelection contract(ForgeDirection direction, int blocks) {
        if (isPos2(direction)) {
            return new Selection(pos1.relative(direction, blocks), pos2);
        } else {
            return new Selection(pos1, pos2.relative(direction, blocks));
        }
    }

    @Override
    public ISelection shift(ForgeDirection direction, int blocks) {
        return new Selection(pos1.relative(direction, blocks), pos2.relative(direction, blocks));
    }
}
