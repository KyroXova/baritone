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

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.Optional;

/**
 * @author Brady
 * @since 9/25/2018
 */
public final class RotationUtils {

    /**
     * Constant that a degree value is multiplied by to get the equivalent radian value
     */
    public static final double DEG_TO_RAD = Math.PI / 180.0;
    public static final float DEG_TO_RAD_F = (float) DEG_TO_RAD;

    /**
     * Constant that a radian value is multiplied by to get the equivalent degree value
     */
    public static final double RAD_TO_DEG = 180.0 / Math.PI;
    public static final float RAD_TO_DEG_F = (float) RAD_TO_DEG;

    /**
     * Offsets from the root block position to the center of each side.
     */
    private static final Vec3[] BLOCK_SIDE_MULTIPLIERS = new Vec3[]{
            Vec3.createVectorHelper(0.5, 0, 0.5),
            Vec3.createVectorHelper(0.5, 1, 0.5),
            Vec3.createVectorHelper(0.5, 0.5, 0),
            Vec3.createVectorHelper(0.5, 0.5, 1),
            Vec3.createVectorHelper(0, 0.5, 0.5),
            Vec3.createVectorHelper(1, 0.5, 0.5)
    };

    private RotationUtils() {}

    /**
     * Calculates the rotation from BetterBlockPos<sub>dest</sub> to BetterBlockPos<sub>orig</sub>
     *
     * @param orig The origin position
     * @param dest The destination position
     * @return The rotation from the origin to the destination
     */
    public static Rotation calcRotationFromCoords(BetterBlockPos orig, BetterBlockPos dest) {
        return calcRotationFromVec3d(Vec3.createVectorHelper(orig.x, orig.y, orig.z), Vec3.createVectorHelper(dest.x, dest.y, dest.z));
    }

    /**
     * Wraps the target angles to a relative value from the current angles. This is done by
     * subtracting the current from the target, normalizing it, and then adding the current
     * angles back to it.
     *
     * @param current The current angles
     * @param target  The target angles
     * @return The wrapped angles
     */
    public static Rotation wrapAnglesToRelative(Rotation current, Rotation target) {
        if (current.yawIsReallyClose(target)) {
            return new Rotation(current.getYaw(), target.getPitch());
        }
        return target.subtract(current).normalize().add(current);
    }

    /**
     * Calculates the rotation from Vec<sub>dest</sub> to Vec<sub>orig</sub> and makes the
     * return value relative to the specified current rotations.
     *
     * @param orig    The origin position
     * @param dest    The destination position
     * @param current The current rotations
     * @return The rotation from the origin to the destination
     * @see #wrapAnglesToRelative(Rotation, Rotation)
     */
    public static Rotation calcRotationFromVec3d(Vec3 orig, Vec3 dest, Rotation current) {
        return wrapAnglesToRelative(current, calcRotationFromVec3d(orig, dest));
    }

    /**
     * Calculates the rotation from Vec<sub>dest</sub> to Vec<sub>orig</sub>
     *
     * @param orig The origin position
     * @param dest The destination position
     * @return The rotation from the origin to the destination
     */
    private static Rotation calcRotationFromVec3d(Vec3 orig, Vec3 dest) {
        double[] delta = {orig.xCoord - dest.xCoord, orig.yCoord - dest.yCoord, orig.zCoord - dest.zCoord};
        double yaw = Math.atan2(delta[0], -delta[2]);
        double dist = Math.sqrt(delta[0] * delta[0] + delta[2] * delta[2]);
        double pitch = Math.atan2(delta[1], dist);
        return new Rotation(
                (float) (yaw * RAD_TO_DEG),
                (float) (pitch * RAD_TO_DEG)
        );
    }

    /**
     * Calculates the look vector for the specified yaw/pitch rotations.
     *
     * @param rotation The input rotation
     * @return Look vector for the rotation
     */
    public static Vec3 calcLookDirectionFromRotation(Rotation rotation) {
        return RayTraceUtils.lookDirection(rotation);
    }

    @Deprecated
    public static Vec3 calcVec3dFromRotation(Rotation rotation) {
        return calcLookDirectionFromRotation(rotation);
    }

    /**
     * @param ctx Context for the viewing entity
     * @param pos The target block position
     * @return The optional rotation
     * @see #reachable(IPlayerContext, BetterBlockPos, double)
     */
    public static Optional<Rotation> reachable(IPlayerContext ctx, BetterBlockPos pos) {
        return reachable(ctx, pos, false);
    }

    public static Optional<Rotation> reachable(IPlayerContext ctx, BetterBlockPos pos, boolean wouldSneak) {
        return reachable(ctx, pos, ctx.playerController().getBlockReachDistance(), wouldSneak);
    }

    /**
     * Determines if the specified entity is able to reach the center of any of the sides
     * of the specified block. It first checks if the block center is reachable, and if so,
     * that rotation will be returned. If not, it will return the first center of a given
     * side that is reachable. The return type will be {@link Optional#empty()} if the entity is
     * unable to reach any of the sides of the block.
     *
     * @param ctx                Context for the viewing entity
     * @param pos                The target block position
     * @param blockReachDistance The block reach distance of the entity
     * @return The optional rotation
     */
    public static Optional<Rotation> reachable(IPlayerContext ctx, BetterBlockPos pos, double blockReachDistance) {
        return reachable(ctx, pos, blockReachDistance, false);
    }

    public static Optional<Rotation> reachable(IPlayerContext ctx, BetterBlockPos pos, double blockReachDistance, boolean wouldSneak) {
        if (BaritoneAPI.getSettings().remainWithExistingLookDirection.value && ctx.isLookingAt(pos)) {
            /*
             * why add 0.0001?
             * to indicate that we actually have a desired pitch
             * the way we indicate that the pitch can be whatever and we only care about the yaw
             * is by setting the desired pitch to the current pitch
             * setting the desired pitch to the current pitch + 0.0001 means that we do have a desired pitch, it's
             * just what it currently is
             *
             * or if you're a normal person literally all this does it ensure that we don't nudge the pitch to a normal level
             */
            Rotation hypothetical = ctx.playerRotations().add(new Rotation(0, 0.0001F));
            if (wouldSneak) {
                // the concern here is: what if we're looking at it now, but as soon as we start sneaking we no longer are
                MovingObjectPosition result = RayTraceUtils.rayTraceTowards(ctx.player(), hypothetical, blockReachDistance, true);
                if (hits(result, pos)) {
                    return Optional.of(hypothetical); // yes, if we sneaked we would still be looking at the block
                }
            } else {
                return Optional.of(hypothetical);
            }
        }
        Optional<Rotation> possibleRotation = reachableCenter(ctx, pos, blockReachDistance, wouldSneak);
        //System.out.println("center: " + possibleRotation);
        if (possibleRotation.isPresent()) {
            return possibleRotation;
        }

        Block block = ctx.world().getBlock(pos.x, pos.y, pos.z);
        block.setBlockBoundsBasedOnState(ctx.world(), pos.x, pos.y, pos.z);
        for (Vec3 sideOffset : BLOCK_SIDE_MULTIPLIERS) {
            double xDiff = block.getBlockBoundsMinX() * sideOffset.xCoord + block.getBlockBoundsMaxX() * (1 - sideOffset.xCoord);
            double yDiff = block.getBlockBoundsMinY() * sideOffset.yCoord + block.getBlockBoundsMaxY() * (1 - sideOffset.yCoord);
            double zDiff = block.getBlockBoundsMinZ() * sideOffset.zCoord + block.getBlockBoundsMaxZ() * (1 - sideOffset.zCoord);
            possibleRotation = reachableOffset(ctx, pos, Vec3.createVectorHelper(pos.x + xDiff, pos.y + yDiff, pos.z + zDiff), blockReachDistance, wouldSneak);
            if (possibleRotation.isPresent()) {
                return possibleRotation;
            }
        }
        return Optional.empty();
    }

    /**
     * Determines if the specified entity is able to reach the specified block with
     * the given offsetted position. The return type will be {@link Optional#empty()} if
     * the entity is unable to reach the block with the offset applied.
     *
     * @param ctx                Context for the viewing entity
     * @param pos                The target block position
     * @param offsetPos          The position of the block with the offset applied.
     * @param blockReachDistance The block reach distance of the entity
     * @return The optional rotation
     */
    public static Optional<Rotation> reachableOffset(IPlayerContext ctx, BetterBlockPos pos, Vec3 offsetPos, double blockReachDistance, boolean wouldSneak) {
        Vec3 eyes = wouldSneak ? RayTraceUtils.inferSneakingEyePosition(ctx.player()) : ctx.playerHead();
        Rotation rotation = calcRotationFromVec3d(eyes, offsetPos, ctx.playerRotations());
        Rotation actualRotation = BaritoneAPI.getProvider().getBaritoneForPlayer(ctx.player()).getLookBehavior().getAimProcessor().peekRotation(rotation);
        MovingObjectPosition result = RayTraceUtils.rayTraceTowards(ctx.player(), actualRotation, blockReachDistance, wouldSneak);
        //System.out.println(result);
        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            if (hits(result, pos)) {
                return Optional.of(rotation);
            }
            if (ctx.world().getBlock(pos.x, pos.y, pos.z) instanceof BlockFire && hits(result, pos.below())) {
                return Optional.of(rotation);
            }
        }
        return Optional.empty();
    }

    /**
     * Determines if the specified entity is able to reach the specified block where it is
     * looking at the direct center of it's hitbox.
     *
     * @param ctx                Context for the viewing entity
     * @param pos                The target block position
     * @param blockReachDistance The block reach distance of the entity
     * @return The optional rotation
     */
    public static Optional<Rotation> reachableCenter(IPlayerContext ctx, BetterBlockPos pos, double blockReachDistance, boolean wouldSneak) {
        return reachableOffset(ctx, pos, VecUtils.calculateBlockCenter(ctx.world(), pos), blockReachDistance, wouldSneak);
    }

    @Deprecated
    public static Optional<Rotation> reachable(EntityClientPlayerMP entity, BetterBlockPos pos, double blockReachDistance) {
        return reachable(entity, pos, blockReachDistance, false);
    }

    @Deprecated
    public static Optional<Rotation> reachable(EntityClientPlayerMP entity, BetterBlockPos pos, double blockReachDistance, boolean wouldSneak) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(entity);
        IPlayerContext ctx = baritone.getPlayerContext();
        return reachable(ctx, pos, blockReachDistance, wouldSneak);
    }

    @Deprecated
    public static Optional<Rotation> reachableOffset(Entity entity, BetterBlockPos pos, Vec3 offsetPos, double blockReachDistance, boolean wouldSneak) {
        Vec3 eyes = RayTraceUtils.inferSneakingEyePosition(entity);
        Rotation rotation = calcRotationFromVec3d(eyes, offsetPos, new Rotation(entity.rotationYaw, entity.rotationPitch));
        MovingObjectPosition result = RayTraceUtils.rayTraceTowards(entity, rotation, blockReachDistance, wouldSneak);
        //System.out.println(result);
        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            if (hits(result, pos)) {
                return Optional.of(rotation);
            }
            if (entity.worldObj.getBlock(pos.x, pos.y, pos.z) instanceof BlockFire && hits(result, pos.below())) {
                return Optional.of(rotation);
            }
        }
        return Optional.empty();
    }

    @Deprecated
    public static Optional<Rotation> reachableCenter(Entity entity, BetterBlockPos pos, double blockReachDistance, boolean wouldSneak) {
        return reachableOffset(entity, pos, VecUtils.calculateBlockCenter(entity.worldObj, pos), blockReachDistance, wouldSneak);
    }

    private static boolean hits(MovingObjectPosition result, BetterBlockPos pos) {
        return result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && result.blockX == pos.x && result.blockY == pos.y && result.blockZ == pos.z;
    }
}
