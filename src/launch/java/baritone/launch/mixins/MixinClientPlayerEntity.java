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
import baritone.api.event.events.ChatEvent;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.SprintStateEvent;
import baritone.api.event.events.type.EventState;
import net.minecraft.client.entity.EntityClientPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Brady
 * @since 8/1/2018
 */
@Mixin(EntityClientPlayerMP.class)
public class MixinClientPlayerEntity {

    @Unique
    private boolean baritone$ridingUpdate;

    @Inject(
            method = "onUpdate",
            at = @At("HEAD")
    )
    private void onPreUpdate(CallbackInfo ci) {
        this.baritone$ridingUpdate = false;
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityClientPlayerMP) (Object) this);
        if (baritone != null) {
            SprintStateEvent sprintEvent = new SprintStateEvent();
            baritone.getGameEventHandler().onPlayerSprintState(sprintEvent);
            if (sprintEvent.getState() != null) {
                ((EntityClientPlayerMP) (Object) this).setSprinting(sprintEvent.getState());
            }
        }
    }

    @Inject(method = "onUpdate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/entity/EntityClientPlayerMP;isRiding()Z"))
    private void onPreRidingPackets(CallbackInfo ci) {
        EntityClientPlayerMP player = (EntityClientPlayerMP) (Object) this;
        if (!player.isRiding()) {
            return;
        }
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(player);
        if (baritone != null) {
            this.baritone$ridingUpdate = true;
            baritone.getGameEventHandler().onPlayerUpdate(new PlayerUpdateEvent(EventState.PRE));
        }
    }

    @Inject(
            method = "onUpdate",
            at = @At("RETURN")
    )
    private void onPostUpdate(CallbackInfo ci) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityClientPlayerMP) (Object) this);
        if (baritone != null && this.baritone$ridingUpdate) {
            baritone.getGameEventHandler().onPlayerUpdate(new PlayerUpdateEvent(EventState.POST));
        }
        this.baritone$ridingUpdate = false;
    }

    @Inject(method = "sendMotionUpdates", at = @At("HEAD"))
    private void onPreMotionUpdate(CallbackInfo ci) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityClientPlayerMP) (Object) this);
        if (baritone != null) {
            baritone.getGameEventHandler().onPlayerUpdate(new PlayerUpdateEvent(EventState.PRE));
        }
    }

    @Inject(method = "sendMotionUpdates", at = @At("RETURN"))
    private void onPostMotionUpdate(CallbackInfo ci) {
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityClientPlayerMP) (Object) this);
        if (baritone != null) {
            baritone.getGameEventHandler().onPlayerUpdate(new PlayerUpdateEvent(EventState.POST));
        }
    }

    @Inject(
            method = "sendChatMessage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onSendChatMessage(String message, CallbackInfo ci) {
        ChatEvent event = new ChatEvent(message);
        IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityClientPlayerMP) (Object) this);
        if (baritone == null) {
            return;
        }
        baritone.getGameEventHandler().onSendChatMessage(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

}
