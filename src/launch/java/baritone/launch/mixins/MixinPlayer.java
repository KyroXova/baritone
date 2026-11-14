/* This file is part of Baritone, distributed under the LGPL-3.0-or-later. */
package baritone.launch.mixins;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.behavior.LookBehavior;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayer.class)
public abstract class MixinPlayer {
    @Inject(method = "updateRidden", at = @At("HEAD"))
    private void baritone$updateRidden(CallbackInfo callback) {
        if ((Object) this instanceof EntityClientPlayerMP) {
            IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((EntityClientPlayerMP) (Object) this);
            if (baritone != null) {
                ((LookBehavior) baritone.getLookBehavior()).pig();
            }
        }
    }
}
