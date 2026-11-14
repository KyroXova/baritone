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

package baritone.api.utils;

import javax.annotation.Nonnull;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Immutable integer block coordinates for the 1.7.10 world interface.
 *
 * @author leijurv
 */
public final class BetterBlockPos {

    private static final int NUM_X_BITS = 26;
    private static final int NUM_Z_BITS = NUM_X_BITS;
    private static final int NUM_Y_BITS = 64 - NUM_X_BITS - NUM_Z_BITS;
    private static final int Y_SHIFT = NUM_Z_BITS;
    private static final int X_SHIFT = Y_SHIFT + NUM_Y_BITS;
    private static final long X_MASK = (1L << NUM_X_BITS) - 1L;
    private static final long Y_MASK = (1L << NUM_Y_BITS) - 1L;
    private static final long Z_MASK = (1L << NUM_Z_BITS) - 1L;

    public static final BetterBlockPos ORIGIN = new BetterBlockPos(0, 0, 0);

    public final int x;
    public final int y;
    public final int z;

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    public BetterBlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BetterBlockPos(double x, double y, double z) {
        this(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
    }

    public BetterBlockPos offset(int dx, int dy, int dz) {
        return dx == 0 && dy == 0 && dz == 0 ? this : new BetterBlockPos(x + dx, y + dy, z + dz);
    }

    public BetterBlockPos offset(BetterBlockPos pos) {
        return pos == null || (pos.x == 0 && pos.y == 0 && pos.z == 0) ? this : new BetterBlockPos(x + pos.x, y + pos.y, z + pos.z);
    }

    public BetterBlockPos add(BetterBlockPos pos) {
        return offset(pos);
    }

    public BetterBlockPos add(int dx, int dy, int dz) {
        return offset(dx, dy, dz);
    }

    public BetterBlockPos subtract(BetterBlockPos pos) {
        return pos == null || (pos.x == 0 && pos.y == 0 && pos.z == 0) ? this : new BetterBlockPos(x - pos.x, y - pos.y, z - pos.z);
    }

    public BetterBlockPos subtract(int dx, int dy, int dz) {
        return offset(-dx, -dy, -dz);
    }

    public BetterBlockPos cross(BetterBlockPos pos) {
        return new BetterBlockPos(
                this.y * pos.z - this.z * pos.y,
                this.z * pos.x - this.x * pos.z,
                this.x * pos.y - this.y * pos.x
        );
    }

    public BetterBlockPos(BetterBlockPos pos) {
        this(pos.x, pos.y, pos.z);
    }

    /**
     * Like constructor but returns null if pos is null, good if you just need to possibly censor coordinates
     *
     * @param pos The position, possibly null, to copy
     * @return A BetterBlockPos or null if pos was null
     */
    public static BetterBlockPos from(BetterBlockPos pos) {
        if (pos == null) {
            return null;
        }

        return new BetterBlockPos(pos);
    }

    @Override
    public int hashCode() {
        return (int) longHash(x, y, z);
    }

    public static long longHash(BetterBlockPos pos) {
        return longHash(pos.x, pos.y, pos.z);
    }

    public static long longHash(int x, int y, int z) {
        // TODO use the same thing as BlockPos.fromLong();
        // invertibility would be incredibly useful
        /*
         *   This is the hashcode implementation of Vec3i (the superclass of the class which I shall not name)
         *
         *   public int hashCode() {
         *       return (this.getY() + this.getZ() * 31) * 31 + this.getX();
         *   }
         *
         *   That is terrible and has tons of collisions and makes the HashMap terribly inefficient.
         *
         *   That's why we grab out the X, Y, Z and calculate our own hashcode
         */
        long hash = 3241;
        hash = 3457689L * hash + x;
        hash = 8734625L * hash + y;
        hash = 2873465L * hash + z;
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BetterBlockPos)) {
            return false;
        }
        BetterBlockPos oth = (BetterBlockPos) o;
        return oth.x == x && oth.y == y && oth.z == z;
    }

    public BetterBlockPos above() {
        // this is unimaginably faster than blockpos.up
        // that literally calls
        // this.up(1)
        // which calls this.offset(Direction.UP, 1)
        // which does return n == 0 ? this : new BlockPos(this.getX() + facing.getXOffset() * n, this.getY() + facing.getYOffset() * n, this.getZ() + facing.getZOffset() * n);

        // how many function calls is that? up(), up(int), offset(Direction, int), new BlockPos, getX, getXOffset, getY, getYOffset, getZ, getZOffset
        // that's ten.
        // this is one function call.
        return new BetterBlockPos(x, y + 1, z);
    }

    public BetterBlockPos up() {
        return above();
    }

    public BetterBlockPos up(int amt) {
        return above(amt);
    }

    public BetterBlockPos above(int amt) {
        // see comment in up()
        return amt == 0 ? this : new BetterBlockPos(x, y + amt, z);
    }

    public BetterBlockPos below() {
        // see comment in up()
        return new BetterBlockPos(x, y - 1, z);
    }

    public BetterBlockPos down() {
        return below();
    }

    public BetterBlockPos down(int amt) {
        return below(amt);
    }

    public BetterBlockPos below(int amt) {
        // see comment in up()
        return amt == 0 ? this : new BetterBlockPos(x, y - amt, z);
    }

    public BetterBlockPos relative(ForgeDirection dir) {
        return relative(dir, 1);
    }

    public BetterBlockPos relative(ForgeDirection dir, int dist) {
        if (dist == 0) {
            return this;
        }
        return new BetterBlockPos(x + dir.offsetX * dist, y + dir.offsetY * dist, z + dir.offsetZ * dist);
    }

    public BetterBlockPos north() {
        return new BetterBlockPos(x, y, z - 1);
    }

    public BetterBlockPos north(int amt) {
        return amt == 0 ? this : new BetterBlockPos(x, y, z - amt);
    }

    public BetterBlockPos south() {
        return new BetterBlockPos(x, y, z + 1);
    }

    public BetterBlockPos south(int amt) {
        return amt == 0 ? this : new BetterBlockPos(x, y, z + amt);
    }

    public BetterBlockPos east() {
        return new BetterBlockPos(x + 1, y, z);
    }

    public BetterBlockPos east(int amt) {
        return amt == 0 ? this : new BetterBlockPos(x + amt, y, z);
    }

    public BetterBlockPos west() {
        return new BetterBlockPos(x - 1, y, z);
    }

    public BetterBlockPos west(int amt) {
        return amt == 0 ? this : new BetterBlockPos(x - amt, y, z);
    }

    public double distanceSq(final BetterBlockPos to) {
        double dx = (double) this.x - to.x;
        double dy = (double) this.y - to.y;
        double dz = (double) this.z - to.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distanceTo(final BetterBlockPos to) {
        double dx = (double) this.x - to.x;
        double dy = (double) this.y - to.y;
        double dz = (double) this.z - to.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    @Nonnull
    public String toString() {
        return String.format(
                "BetterBlockPos{x=%s,y=%s,z=%s}",
                SettingsUtil.maybeCensor(x),
                SettingsUtil.maybeCensor(y),
                SettingsUtil.maybeCensor(z)
        );
    }

    public static long serializeToLong(final int x, final int y, final int z) {
        return ((long) x & X_MASK) << X_SHIFT | ((long) y & Y_MASK) << Y_SHIFT | ((long) z & Z_MASK);
    }

    public static BetterBlockPos deserializeFromLong(final long serialized) {
        final int x = (int) (serialized << 64 - X_SHIFT - NUM_X_BITS >> 64 - NUM_X_BITS);
        final int y = (int) (serialized << 64 - Y_SHIFT - NUM_Y_BITS >> 64 - NUM_Y_BITS);
        final int z = (int) (serialized << 64 - NUM_Z_BITS >> 64 - NUM_Z_BITS);
        return new BetterBlockPos(x, y, z);
    }
}
