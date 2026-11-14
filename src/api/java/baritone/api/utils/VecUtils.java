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

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * @author Brady
 * @since 10/13/2018
 */
public final class VecUtils {

    private VecUtils() {}

    /**
     * Calculates the center of the block at the specified position's bounding box
     *
     * @param world The world that the block is in, used to provide the bounding box
     * @param pos   The block position
     * @return The center of the block's bounding box
     * @see #getBlockPosCenter(BetterBlockPos)
     */
    public static Vec3 calculateBlockCenter(World world, BetterBlockPos pos) {
        Block block = world.getBlock(pos.x, pos.y, pos.z);
        AxisAlignedBB shape = block.getCollisionBoundingBoxFromPool(world, pos.x, pos.y, pos.z);
        if (shape == null) {
            return getBlockPosCenter(pos);
        }
        double x = (shape.minX + shape.maxX) / 2;
        double y = (shape.minY + shape.maxY) / 2;
        double z = (shape.minZ + shape.maxZ) / 2;
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            throw new IllegalStateException(block + " " + pos + " " + shape);
        }
        if (block instanceof BlockFire) {
            y = pos.y;
        }
        return Vec3.createVectorHelper(x, y, z);
    }

    /**
     * Gets the assumed center position of the given block position.
     * This is done by adding 0.5 to the X, Y, and Z axes.
     * <p>
     * TODO: Consider using the actual collision bounds when available.
     *
     * @param pos The block position
     * @return The assumed center of the position
     * @see #calculateBlockCenter(World, BetterBlockPos)
     */
    public static Vec3 getBlockPosCenter(BetterBlockPos pos) {
        return Vec3.createVectorHelper(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    /**
     * Gets the distance from the specified position to the assumed center of the specified block position.
     *
     * @param pos The block position
     * @param x   The x pos
     * @param y   The y pos
     * @param z   The z pos
     * @return The distance from the assumed block center to the position
     * @see #getBlockPosCenter(BetterBlockPos)
     */
    public static double distanceToCenter(BetterBlockPos pos, double x, double y, double z) {
        double xdiff = pos.getX() + 0.5 - x;
        double ydiff = pos.getY() + 0.5 - y;
        double zdiff = pos.getZ() + 0.5 - z;
        return Math.sqrt(xdiff * xdiff + ydiff * ydiff + zdiff * zdiff);
    }

    /**
     * Gets the distance from the specified entity's position to the assumed
     * center of the specified block position.
     *
     * @param entity The entity
     * @param pos    The block position
     * @return The distance from the entity to the block's assumed center
     * @see #getBlockPosCenter(BetterBlockPos)
     */
    public static double entityDistanceToCenter(Entity entity, BetterBlockPos pos) {
        return distanceToCenter(pos, entity.posX, entity.posY, entity.posZ);
    }

    /**
     * Gets the distance from the specified entity's position to the assumed
     * center of the specified block position, ignoring the Y axis.
     *
     * @param entity The entity
     * @param pos    The block position
     * @return The horizontal distance from the entity to the block's assumed center
     * @see #getBlockPosCenter(BetterBlockPos)
     */
    public static double entityFlatDistanceToCenter(Entity entity, BetterBlockPos pos) {
        return distanceToCenter(pos, entity.posX, pos.getY() + 0.5, entity.posZ);
    }
}
