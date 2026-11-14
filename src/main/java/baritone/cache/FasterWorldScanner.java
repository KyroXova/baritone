/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.cache;

import baritone.api.cache.IWorldScanner;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.IPlayerContext;
import net.minecraft.block.Block;
import net.minecraft.world.ChunkCoordIntPair;

import java.util.List;

public enum FasterWorldScanner implements IWorldScanner {

    INSTANCE;

    @Override
    public List<BetterBlockPos> scanChunkRadius(IPlayerContext ctx, BlockOptionalMetaLookup filter, int max, int yLevelThreshold, int maxSearchRadius) {
        return WorldScanner.INSTANCE.scanChunkRadius(ctx, filter, max, yLevelThreshold, maxSearchRadius);
    }

    @Override
    public List<BetterBlockPos> scanChunkRadius(IPlayerContext ctx, List<Block> filter, int max, int yLevelThreshold, int maxSearchRadius) {
        return WorldScanner.INSTANCE.scanChunkRadius(ctx, filter, max, yLevelThreshold, maxSearchRadius);
    }

    @Override
    public List<BetterBlockPos> scanChunk(IPlayerContext ctx, BlockOptionalMetaLookup filter, ChunkCoordIntPair pos, int max, int yLevelThreshold) {
        return WorldScanner.INSTANCE.scanChunk(ctx, filter, pos, max, yLevelThreshold);
    }

    @Override
    public List<BetterBlockPos> scanChunk(IPlayerContext ctx, List<Block> blocks, ChunkCoordIntPair pos, int max, int yLevelThreshold) {
        return WorldScanner.INSTANCE.scanChunk(ctx, blocks, pos, max, yLevelThreshold);
    }

    @Override
    public int repack(IPlayerContext ctx) {
        return WorldScanner.INSTANCE.repack(ctx);
    }

    @Override
    public int repack(IPlayerContext ctx, int range) {
        return WorldScanner.INSTANCE.repack(ctx, range);
    }
}
