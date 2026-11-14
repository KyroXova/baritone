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

package baritone.launch.mixins;

import baritone.api.BaritoneAPI;
import baritone.api.event.events.RotationMoveEvent;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntity {

    @Shadow
    public float rotationYaw;

    @Shadow
    public float rotationPitch;

    @Unique
    private RotationMoveEvent motionUpdateRotationEvent;

    @Inject(
            method = "moveFlying",
            at = @At("HEAD")
    )
    private void moveFlyingHead(float strafe, float forward, float friction, CallbackInfo info) {
        if (!EntityClientPlayerMP.class.isInstance(this) || BaritoneAPI.getProvider().getBaritoneForPlayer((EntityClientPlayerMP) (Object) this) == null) {
            return;
        }
        this.motionUpdateRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.MOTION_UPDATE, this.rotationYaw, this.rotationPitch);
        BaritoneAPI.getProvider().getBaritoneForPlayer((EntityClientPlayerMP) (Object) this).getGameEventHandler().onPlayerRotationMove(motionUpdateRotationEvent);
        this.rotationYaw = this.motionUpdateRotationEvent.getYaw();
        this.rotationPitch = this.motionUpdateRotationEvent.getPitch();
    }

    @Inject(
            method = "moveFlying",
            at = @At("RETURN")
    )
    private void moveFlyingReturn(float strafe, float forward, float friction, CallbackInfo info) {
        if (this.motionUpdateRotationEvent != null) {
            this.rotationYaw = this.motionUpdateRotationEvent.getOriginal().getYaw();
            this.rotationPitch = this.motionUpdateRotationEvent.getOriginal().getPitch();
            this.motionUpdateRotationEvent = null;
        }
    }
}
