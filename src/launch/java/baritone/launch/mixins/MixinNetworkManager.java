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
import baritone.api.event.events.PacketEvent;
import baritone.api.event.events.type.EventState;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    @Shadow
    private Channel channel;

    @Inject(
            method = "scheduleOutboundPacket",
            at = @At("HEAD")
    )
    private void preDispatchPacket(Packet packet, GenericFutureListener[] listeners, CallbackInfo ci) {
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().sendQueue != null && ibaritone.getPlayerContext().player().sendQueue.getNetworkManager() == (NetworkManager) (Object) this) {
                ibaritone.getGameEventHandler().onSendPacket(new PacketEvent((NetworkManager) (Object) this, EventState.PRE, packet));
            }
        }
    }

    @Inject(
            method = "scheduleOutboundPacket",
            at = @At("RETURN")
    )
    private void postDispatchPacket(Packet packet, GenericFutureListener[] listeners, CallbackInfo ci) {
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().sendQueue != null && ibaritone.getPlayerContext().player().sendQueue.getNetworkManager() == (NetworkManager) (Object) this) {
                ibaritone.getGameEventHandler().onSendPacket(new PacketEvent((NetworkManager) (Object) this, EventState.POST, packet));
            }
        }
    }

    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V",
            at = @At("HEAD")
    )
    private void preProcessPacket(ChannelHandlerContext context, Packet packet, CallbackInfo ci) {
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            if (ibaritone.getPlayerContext().player() != null && ibaritone.getPlayerContext().player().sendQueue != null && ibaritone.getPlayerContext().player().sendQueue.getNetworkManager() == (NetworkManager) (Object) this) {
                ibaritone.getGameEventHandler().onReceivePacket(new PacketEvent((NetworkManager) (Object) this, EventState.PRE, packet));
            }
        }
    }
}
