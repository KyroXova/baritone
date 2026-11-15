/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package baritone.launch.mixins;

import baritone.utils.accessor.IClientChunkProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkProviderClient.class)
public abstract class MixinClientChunkProvider implements IClientChunkProvider {
    @Shadow private List<Chunk> chunkListing;
    @Unique private final Map<Long, Chunk> baritone$loadedChunks = new HashMap<>();

    @Inject(method = "loadChunk", at = @At("RETURN"))
    private void baritone$trackLoadedChunk(int x, int z, CallbackInfoReturnable<Chunk> callback) {
        Chunk chunk = callback.getReturnValue();
        if (chunk != null && !chunk.isEmpty()) {
            baritone$loadedChunks.put(ChunkCoordIntPair.chunkXZ2Int(x, z), chunk);
        }
    }

    @Inject(method = "unloadChunk", at = @At("HEAD"))
    private void baritone$trackUnloadedChunk(int x, int z, CallbackInfo callback) {
        baritone$loadedChunks.remove(ChunkCoordIntPair.chunkXZ2Int(x, z));
    }

    @Override
    public Map<Long, Chunk> snapshotLoadedChunks() {
        Map<Long, Chunk> result = new HashMap<>();
        if (chunkListing == null) {
            result.putAll(baritone$loadedChunks);
            return Collections.unmodifiableMap(result);
        }
        for (Chunk chunk : chunkListing) {
            result.put(ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition), chunk);
        }
        return Collections.unmodifiableMap(result);
    }
}
