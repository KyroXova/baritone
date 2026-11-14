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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/**
 * @author Brady
 * @since 8/25/2018
 */
public final class RayTraceUtils {

    private RayTraceUtils() {}

    /**
     * Performs a block raytrace with the specified rotations. This should only be used when
     * any entity collisions can be ignored, because this method will not recognize if an
     * entity is in the way or not. The local player's block reach distance will be used.
     *
     * @param entity             The entity representing the raytrace source
     * @param rotation           The rotation to raytrace towards
     * @param blockReachDistance The block reach distance of the entity
     * @return The calculated raytrace result
     */
    public static MovingObjectPosition rayTraceTowards(Entity entity, Rotation rotation, double blockReachDistance) {
        return rayTraceTowards(entity, rotation, blockReachDistance, false);
    }

    public static MovingObjectPosition rayTraceTowards(Entity entity, Rotation rotation, double blockReachDistance, boolean wouldSneak) {
        Vec3 start;
        if (wouldSneak) {
            start = inferSneakingEyePosition(entity);
        } else {
            start = eyePosition(entity);
        }
        
        Vec3 direction = lookDirection(rotation);
        Vec3 end = start.addVector(
                direction.xCoord * blockReachDistance,
                direction.yCoord * blockReachDistance,
                direction.zCoord * blockReachDistance
        );
        return entity.worldObj.func_147447_a(start, end, false, false, true);
    }

    public static Vec3 inferSneakingEyePosition(Entity entity) {
        Vec3 eye = eyePosition(entity);
        if (entity != null && !entity.isSneaking()) {
            return eye.addVector(0, -0.08D, 0);
        }
        return eye;
    }

    private static Vec3 eyePosition(Entity entity) {
        if (entity instanceof EntityLivingBase) {
            return ((EntityLivingBase) entity).getPosition(1.0F);
        }
        return Vec3.createVectorHelper(entity.posX, entity.posY, entity.posZ);
    }

    static Vec3 lookDirection(Rotation rotation) {
        float radians = (float) (Math.PI / 180.0);
        float flatZ = MathHelper.cos(-rotation.getYaw() * radians - (float) Math.PI);
        float flatX = MathHelper.sin(-rotation.getYaw() * radians - (float) Math.PI);
        float pitchBase = -MathHelper.cos(-rotation.getPitch() * radians);
        float pitchHeight = MathHelper.sin(-rotation.getPitch() * radians);
        return Vec3.createVectorHelper(flatX * pitchBase, pitchHeight, flatZ * pitchBase);
    }
}
