/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.cache;

import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.api.utils.IPlayerContext;
import net.minecraft.block.Block;
import net.minecraft.world.ChunkCoordIntPair;

import java.util.List;

public interface IWorldScanner {

    List<BetterBlockPos> scanChunkRadius(IPlayerContext ctx, BlockOptionalMetaLookup filter, int max, int yLevelThreshold, int maxSearchRadius);

    default List<BetterBlockPos> scanChunkRadius(IPlayerContext ctx, List<Block> filter, int max, int yLevelThreshold, int maxSearchRadius) {
        return scanChunkRadius(ctx, new BlockOptionalMetaLookup(filter.toArray(new Block[0])), max, yLevelThreshold, maxSearchRadius);
    }

    List<BetterBlockPos> scanChunk(IPlayerContext ctx, BlockOptionalMetaLookup filter, ChunkCoordIntPair pos, int max, int yLevelThreshold);

    default List<BetterBlockPos> scanChunk(IPlayerContext ctx, List<Block> blocks, ChunkCoordIntPair pos, int max, int yLevelThreshold) {
        return scanChunk(ctx, new BlockOptionalMetaLookup(blocks), pos, max, yLevelThreshold);
    }

    int repack(IPlayerContext ctx);

    int repack(IPlayerContext ctx, int range);
}
