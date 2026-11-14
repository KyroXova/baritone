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

import baritone.api.cache.IWorldData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.block.BlockSlab;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Brady
 * @since 11/12/2018
 */
public interface IPlayerContext {

    Minecraft minecraft();

    EntityClientPlayerMP player();

    IPlayerController playerController();

    World world();

    default void disconnect() {
        if (world() != null) {
            world().sendQuittingDisconnectingPacket();
            minecraft().loadWorld(null);
        }
    }

    @SuppressWarnings("unchecked")
    default Iterable<Entity> entities() {
        return (Iterable<Entity>) world().loadedEntityList;
    }

    default Stream<Entity> entitiesStream() {
        return StreamSupport.stream(entities().spliterator(), false);
    }


    IWorldData worldData();

    MovingObjectPosition objectMouseOver();

    default BetterBlockPos playerFeet() {
        // TODO find a better way to deal with soul sand!!!!!
        BetterBlockPos feet = new BetterBlockPos(player().posX, player().boundingBox.minY + 0.1251, player().posZ);

        // sometimes when calling this from another thread or while world is null, it'll throw a NullPointerException
        // that causes the game to immediately crash
        //
        // so of course crashing on 2b is horribly bad due to queue times and logout spot
        // catch the NPE and ignore it if it does happen
        //
        // this does not impact performance at all since we're not null checking constantly
        // if there is an exception, the only overhead is Java generating the exception object... so we can ignore it
        try {
            if (world().getBlock(feet.x, feet.y, feet.z) instanceof BlockSlab) {
                return feet.above();
            }
        } catch (NullPointerException ignored) {}

        return feet;
    }

    default Vec3 playerFeetAsVec() {
        return Vec3.createVectorHelper(player().posX, player().boundingBox.minY, player().posZ);
    }

    default Vec3 playerHead() {
        return player().getPosition(1.0F);
    }

    default Vec3 playerMotion() {
        return Vec3.createVectorHelper(player().motionX, player().motionY, player().motionZ);
    }

    BetterBlockPos viewerPos();

    default Rotation playerRotations() {
        return new Rotation(player().rotationYaw, player().rotationPitch);
    }

    /**
     * Returns the player's eye height, taking into account whether or not the player is sneaking.
     *
     * @param ifSneaking Whether or not the player is sneaking
     * @return The player's eye height
     * @deprecated Use entity.getEyeHeight(Pose.CROUCHING) instead
     */
    @Deprecated
    static double eyeHeight(boolean ifSneaking) {
        return ifSneaking ? 1.27 : 1.62;
    }

    /**
     * Returns the block that the crosshair is currently placed over. Updated once per tick.
     *
     * @return The position of the highlighted block
     */
    default Optional<BetterBlockPos> getSelectedBlock() {
        MovingObjectPosition result = objectMouseOver();
        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return Optional.of(new BetterBlockPos(result.blockX, result.blockY, result.blockZ));
        }
        return Optional.empty();
    }

    default boolean isLookingAt(BetterBlockPos pos) {
        return getSelectedBlock().equals(Optional.of(pos));
    }
}
