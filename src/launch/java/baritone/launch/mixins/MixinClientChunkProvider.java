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

@Mixin(ChunkProviderClient.class)
public abstract class MixinClientChunkProvider implements IClientChunkProvider {
    @Shadow private List<Chunk> chunkListing;

    @Override
    public Map<Long, Chunk> snapshotLoadedChunks() {
        Map<Long, Chunk> result = new HashMap<>();
        for (Chunk chunk : chunkListing) {
            result.put(ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition), chunk);
        }
        return Collections.unmodifiableMap(result);
    }
}
