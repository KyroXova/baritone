/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package baritone.utils;

import baritone.Baritone;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockState;
import baritone.api.utils.IPlayerContext;
import baritone.cache.CachedRegion;
import baritone.cache.WorldData;
import baritone.utils.accessor.IClientChunkProvider;
import baritone.utils.pathing.BetterWorldBorder;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.init.Blocks;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/** Access to loaded client chunks and Baritone's cached regions. */
public class BlockStateInterface {
    private final ChunkProviderClient provider;
    private final Map<Long, Chunk> loadedSnapshot;
    private final WorldData worldData;
    protected final World world;
    public final IBlockAccess access;
    public final BetterWorldBorder worldBorder;
    private final boolean useTheRealWorld;
    private Chunk previousChunk;
    private CachedRegion previousRegion;
    private static final BlockState AIR = new BlockState(Blocks.air, 0);

    public BlockStateInterface(IPlayerContext ctx) {
        this(ctx, false);
    }

    public BlockStateInterface(IPlayerContext ctx, boolean copyLoadedChunks) {
        if (!ctx.minecraft().func_152345_ab()) {
            throw new IllegalStateException("BlockStateInterface must be constructed on the client thread");
        }
        this.world = ctx.world();
        this.worldData = (WorldData) ctx.worldData();
        this.provider = (ChunkProviderClient) world.getChunkProvider();
        this.loadedSnapshot = copyLoadedChunks
                ? ((IClientChunkProvider) provider).snapshotLoadedChunks() : null;
        this.useTheRealWorld = !Baritone.settings().pathThroughCachedOnly.value;
        this.worldBorder = new BetterWorldBorder();
        this.access = new BlockStateInterfaceAccessWrapper(this);
    }

    private Chunk loadedChunk(int chunkX, int chunkZ) {
        if (loadedSnapshot != null) {
            return loadedSnapshot.get(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ));
        }
        Chunk chunk = provider.provideChunk(chunkX, chunkZ);
        return chunk.isEmpty() ? null : chunk;
    }

    public boolean worldContainsLoadedChunk(int blockX, int blockZ) {
        return loadedChunk(blockX >> 4, blockZ >> 4) != null;
    }

    public static Block getBlock(IPlayerContext ctx, BetterBlockPos pos) {
        return get(ctx, pos).getBlock();
    }

    public static BlockState get(IPlayerContext ctx, BetterBlockPos pos) {
        return new BlockStateInterface(ctx).get0(pos);
    }

    public BlockState get0(BetterBlockPos pos) {
        return get0(pos.x, pos.y, pos.z);
    }

    public BlockState get0(int x, int y, int z) {
        if (y < 0 || y >= 256) return AIR;
        if (useTheRealWorld) {
            Chunk chunk = previousChunk;
            if (loadedSnapshot == null || chunk == null || chunk.xPosition != x >> 4 || chunk.zPosition != z >> 4) {
                chunk = loadedChunk(x >> 4, z >> 4);
                previousChunk = chunk;
            }
            if (chunk != null) return getFromChunk(chunk, x, y, z);
        }
        CachedRegion region = cachedRegion(x, z);
        if (region == null) return AIR;
        BlockState state = region.getBlock(x & 511, y, z & 511);
        return state == null ? AIR : state;
    }

    public boolean isLoaded(int x, int z) {
        if (loadedChunk(x >> 4, z >> 4) != null) return true;
        CachedRegion region = cachedRegion(x, z);
        return region != null && region.isCached(x & 511, z & 511);
    }

    private CachedRegion cachedRegion(int x, int z) {
        CachedRegion region = previousRegion;
        if (region == null || region.getX() != x >> 9 || region.getZ() != z >> 9) {
            region = worldData == null ? null : worldData.cache.getRegion(x >> 9, z >> 9);
            previousRegion = region;
        }
        return region;
    }

    public static BlockState getFromChunk(Chunk chunk, int x, int y, int z) {
        if (y < 0 || y >= 256) return AIR;
        return new BlockState(chunk.getBlock(x & 15, y, z & 15), chunk.getBlockMetadata(x & 15, y, z & 15));
    }
}
