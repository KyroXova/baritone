/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.cache;

import baritone.api.cache.ICachedWorld;
import baritone.api.cache.IWorldScanner;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.BlockState;
import baritone.api.utils.IPlayerContext;
import net.minecraft.block.Block;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.ArrayList;
import java.util.List;

public enum WorldScanner implements IWorldScanner {

    INSTANCE;

    @Override
    public List<BetterBlockPos> scanChunkRadius(IPlayerContext ctx, BlockOptionalMetaLookup filter, int max, int yLevelThreshold, int maxSearchRadius) {
        List<BetterBlockPos> res = new ArrayList<>();
        if (filter.blocks().isEmpty() || ctx.world() == null) {
            return res;
        }

        int playerChunkX = ctx.playerFeet().x >> 4;
        int playerChunkZ = ctx.playerFeet().z >> 4;

        int maxSearchRadiusSq = maxSearchRadius * maxSearchRadius;

        for (int r = 0; r <= maxSearchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    if (dx * dx + dz * dz > maxSearchRadiusSq) {
                        continue;
                    }

                    int cx = playerChunkX + dx;
                    int cz = playerChunkZ + dz;

                    if (!ctx.world().getChunkProvider().chunkExists(cx, cz)) {
                        continue;
                    }

                    Chunk chunk = ctx.world().getChunkFromChunkCoords(cx, cz);
                    if (chunk == null || !chunk.isChunkLoaded) {
                        continue;
                    }

                    scanChunk(chunk, filter, res, max, yLevelThreshold);
                    if (max > 0 && res.size() >= max) {
                        return res;
                    }
                }
            }
        }

        return res;
    }

    @Override
    public List<BetterBlockPos> scanChunk(IPlayerContext ctx, BlockOptionalMetaLookup filter, ChunkCoordIntPair pos, int max, int yLevelThreshold) {
        List<BetterBlockPos> res = new ArrayList<>();
        if (filter.blocks().isEmpty() || ctx.world() == null) {
            return res;
        }
        if (!ctx.world().getChunkProvider().chunkExists(pos.chunkXPos, pos.chunkZPos)) {
            return res;
        }
        Chunk chunk = ctx.world().getChunkFromChunkCoords(pos.chunkXPos, pos.chunkZPos);
        if (chunk == null || !chunk.isChunkLoaded) {
            return res;
        }

        scanChunk(chunk, filter, res, max, yLevelThreshold);
        return res;
    }

    private void scanChunk(Chunk chunk, BlockOptionalMetaLookup filter, List<BetterBlockPos> res, int max, int yLevelThreshold) {
        ExtendedBlockStorage[] storageArrays = chunk.getBlockStorageArray();
        int chunkXBase = chunk.xPosition << 4;
        int chunkZBase = chunk.zPosition << 4;

        for (ExtendedBlockStorage storage : storageArrays) {
            if (storage == null) {
                continue;
            }
            int baseY = storage.getYLocation();
            if (yLevelThreshold >= 0 && baseY > yLevelThreshold) {
                continue;
            }

            for (int y = 0; y < 16; y++) {
                int worldY = baseY + y;
                if (yLevelThreshold >= 0 && worldY > yLevelThreshold) {
                    continue;
                }
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        Block block = storage.getBlockByExtId(x, y, z);
                        if (block != null && filter.has(block)) {
                            int meta = storage.getExtBlockMetadata(x, y, z);
                            if (filter.has(new BlockState(block, meta))) {
                                res.add(new BetterBlockPos(chunkXBase + x, worldY, chunkZBase + z));
                                if (max > 0 && res.size() >= max) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public int repack(IPlayerContext ctx) {
        return repack(ctx, 40);
    }

    @Override
    public int repack(IPlayerContext ctx, int range) {
        if (ctx.worldData() == null || ctx.worldData().getCachedWorld() == null || ctx.world() == null) {
            return 0;
        }
        ICachedWorld cachedWorld = ctx.worldData().getCachedWorld();
        int playerChunkX = ctx.playerFeet().x >> 4;
        int playerChunkZ = ctx.playerFeet().z >> 4;
        int count = 0;

        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;
                if (ctx.world().getChunkProvider().chunkExists(cx, cz)) {
                    Chunk chunk = ctx.world().getChunkFromChunkCoords(cx, cz);
                    if (chunk != null && chunk.isChunkLoaded) {
                        cachedWorld.queueForPacking(chunk);
                        count++;
                    }
                }
            }
        }
        return count;
    }
}
