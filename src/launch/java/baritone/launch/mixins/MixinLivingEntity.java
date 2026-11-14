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
import baritone.api.IBaritone;
import baritone.api.event.events.RotationMoveEvent;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class MixinLivingEntity extends Entity {

    @Unique
    private RotationMoveEvent jumpRotationEvent;

    private MixinLivingEntity(World worldIn) {
        super(worldIn);
    }

    @Inject(
            method = "jump",
            at = @At("HEAD")
    )
    private void preJump(CallbackInfo ci) {
        if (!EntityClientPlayerMP.class.isInstance(this)) {
            return;
        }
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityClientPlayerMP) (Object) this);
        if (baritone == null) {
            return;
        }
        this.jumpRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.JUMP, this.rotationYaw, this.rotationPitch);
        baritone.getGameEventHandler().onPlayerRotationMove(this.jumpRotationEvent);
        this.rotationYaw = this.jumpRotationEvent.getYaw();
        this.rotationPitch = this.jumpRotationEvent.getPitch();
    }

    @Inject(
            method = "jump",
            at = @At("RETURN")
    )
    private void postJump(CallbackInfo ci) {
        if (this.jumpRotationEvent != null) {
            this.rotationYaw = this.jumpRotationEvent.getOriginal().getYaw();
            this.rotationPitch = this.jumpRotationEvent.getOriginal().getPitch();
            this.jumpRotationEvent = null;
        }
    }
}
