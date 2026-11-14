/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package baritone.cache;

import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockState;
import baritone.api.utils.BlockUtils;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.pathing.PathingBlockType;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import static baritone.utils.BlockStateInterface.getFromChunk;

/** Packs legacy chunk block and metadata data for long-range pathing. */
public final class ChunkPacker {
    private static final int HEIGHT = 256;

    private ChunkPacker() {}

    public static CachedChunk pack(Chunk chunk) {
        Map<String, List<BetterBlockPos>> specialBlocks = new HashMap<>();
        Map<Integer, Integer> specialMetadata = new HashMap<>();
        BitSet bits = new BitSet(CachedChunk.size(HEIGHT));
        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        for (int sectionY = 0; sectionY < sections.length; sectionY++) {
            ExtendedBlockStorage section = sections[sectionY];
            if (section == null) continue;
            for (int localY = 0; localY < 16; localY++) {
                int y = (sectionY << 4) | localY;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = new BlockState(section.getBlockByExtId(x, localY, z),
                                section.getExtBlockMetadata(x, localY, z));
                        int index = CachedChunk.getPositionIndex(x, y, z);
                        boolean[] packed = getPathingBlockType(state, chunk, x, y, z).getBits();
                        bits.set(index, packed[0]);
                        bits.set(index + 1, packed[1]);
                        if (CachedChunk.BLOCKS_TO_KEEP_TRACK_OF.contains(state.getBlock())) {
                            String name = BlockUtils.blockToString(state.getBlock());
                            specialBlocks.computeIfAbsent(name, ignored -> new ArrayList<>()).add(new BetterBlockPos(x, y, z));
                            specialMetadata.put(index, state.getMetadata());
                        }
                    }
                }
            }
        }
        BlockState[] overview = new BlockState[256];
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                BlockState top = new BlockState(Blocks.air, 0);
                for (int y = HEIGHT - 1; y >= 0; y--) {
                    int index = CachedChunk.getPositionIndex(x, y, z);
                    if (bits.get(index) || bits.get(index + 1)) {
                        top = getFromChunk(chunk, x, y, z);
                        break;
                    }
                }
                overview[z << 4 | x] = top;
            }
        }
        return new CachedChunk(chunk.xPosition, chunk.zPosition, HEIGHT, bits, overview,
                specialBlocks, specialMetadata, System.currentTimeMillis());
    }

    private static PathingBlockType getPathingBlockType(BlockState state, Chunk chunk, int x, int y, int z) {
        Block block = state.getBlock();
        if (MovementHelper.isWater(state)) {
            if (MovementHelper.possiblyFlowing(state)
                    || (x != 15 && MovementHelper.possiblyFlowing(getFromChunk(chunk, x + 1, y, z)))
                    || (x != 0 && MovementHelper.possiblyFlowing(getFromChunk(chunk, x - 1, y, z)))
                    || (z != 15 && MovementHelper.possiblyFlowing(getFromChunk(chunk, x, y, z + 1)))
                    || (z != 0 && MovementHelper.possiblyFlowing(getFromChunk(chunk, x, y, z - 1)))) {
                return PathingBlockType.AVOID;
            }
            if (x == 0 || x == 15 || z == 0 || z == 15) {
                double flow = BlockLiquid.getFlowDirection(chunk.worldObj, x + (chunk.xPosition << 4), y,
                        z + (chunk.zPosition << 4), Material.water);
                return flow != -1000.0D ? PathingBlockType.WATER : PathingBlockType.AVOID;
            }
            return PathingBlockType.WATER;
        }
        if (MovementHelper.avoidWalkingInto(state) || MovementHelper.isBottomSlab(state)) return PathingBlockType.AVOID;
        if (block instanceof BlockAir || block instanceof BlockTallGrass || block instanceof BlockDoublePlant
                || block instanceof BlockFlower) return PathingBlockType.AIR;
        return PathingBlockType.SOLID;
    }

    public static BlockState pathingTypeToBlock(PathingBlockType type, int dimensionId) {
        switch (type) {
            case AIR: return new BlockState(Blocks.air, 0);
            case WATER: return new BlockState(Blocks.water, 0);
            case AVOID: return new BlockState(Blocks.lava, 0);
            case SOLID: return new BlockState(dimensionId == -1 ? Blocks.netherrack
                    : dimensionId == 1 ? Blocks.end_stone : Blocks.stone, 0);
            default: throw new IllegalArgumentException("Unknown pathing block type " + type);
        }
    }
}
