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

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.ChunkEvent;
import baritone.api.event.events.type.EventState;
import baritone.cache.CachedChunk;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S23PacketBlockChange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Brady
 * @since 8/3/2018
 */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinClientPlayNetHandler {

    @Inject(
            method = "handleChunkData",
            at = @At("RETURN")
    )
    private void postHandleChunkData(S21PacketChunkData packetIn, CallbackInfo ci) {
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            EntityClientPlayerMP player = ibaritone.getPlayerContext().player();
            if (player != null && player.sendQueue == (NetHandlerPlayClient) (Object) this) {
                ibaritone.getGameEventHandler().onChunkEvent(
                        new ChunkEvent(
                                EventState.POST,
                                packetIn.func_149274_i() ? ChunkEvent.Type.POPULATE_FULL : ChunkEvent.Type.POPULATE_PARTIAL,
                                packetIn.func_149273_e(),
                                packetIn.func_149271_f()
                        )
                );
            }
        }
    }

    @Inject(
            method = "handleBlockChange",
            at = @At("RETURN")
    )
    private void postHandleBlockChange(S23PacketBlockChange packetIn, CallbackInfo ci) {
        if (!Baritone.settings().repackOnAnyBlockChange.value) {
            return;
        }
        if (!CachedChunk.BLOCKS_TO_KEEP_TRACK_OF.contains(packetIn.func_148880_c())) {
            return;
        }
        for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
            EntityClientPlayerMP player = ibaritone.getPlayerContext().player();
            if (player != null && player.sendQueue == (NetHandlerPlayClient) (Object) this) {
                ibaritone.getGameEventHandler().onChunkEvent(
                        new ChunkEvent(
                                EventState.POST,
                                ChunkEvent.Type.POPULATE_FULL,
                                packetIn.func_148879_d() >> 4,
                                packetIn.func_148877_f() >> 4
                        )
                );
            }
        }
    }

    @Inject(
            method = "handleUpdateHealth",
            at = @At("RETURN")
    )
    private void onUpdateHealth(S06PacketUpdateHealth packetIn, CallbackInfo ci) {
        if (packetIn.func_149332_c() <= 0.0F) {
            for (IBaritone ibaritone : BaritoneAPI.getProvider().getAllBaritones()) {
                EntityClientPlayerMP player = ibaritone.getPlayerContext().player();
                if (player != null && player.sendQueue == (NetHandlerPlayClient) (Object) this) {
                    ibaritone.getGameEventHandler().onPlayerDeath();
                }
            }
        }
    }
}
