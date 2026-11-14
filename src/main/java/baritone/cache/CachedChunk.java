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

package baritone.cache;

import baritone.api.utils.BlockUtils;
import baritone.utils.pathing.PathingBlockType;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockState;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * @author Brady
 * @since 8/3/2018
 */
public final class CachedChunk {

    public static final ImmutableSet<Block> BLOCKS_TO_KEEP_TRACK_OF = new ImmutableSet.Builder<Block>()
        .add(
            Blocks.ender_chest, Blocks.furnace, Blocks.lit_furnace, Blocks.chest,
            Blocks.trapped_chest, Blocks.end_portal, Blocks.end_portal_frame,
            Blocks.mob_spawner, Blocks.portal, Blocks.hopper, Blocks.beacon,
            Blocks.brewing_stand, Blocks.skull, Blocks.enchanting_table,
            Blocks.anvil, Blocks.dragon_egg, Blocks.jukebox, Blocks.web,
            Blocks.nether_wart, Blocks.ladder, Blocks.vine, Blocks.bed
        ).build();

    public final int height;

    /**
     * The size of the chunk data in bits. Equal to 16 KiB.
     * <p>
     * Chunks are 16x16xH, each block requires 2 bits.
     */
    public final int size;

    /**
     * The size of the chunk data in bytes. Equal to 16 KiB for 256 height.
     */
    public final int sizeInBytes;

    /**
     * The chunk x coordinate
     */
    public final int x;

    /**
     * The chunk z coordinate
     */
    public final int z;

    /**
     * The actual raw data of this packed chunk.
     * <p>
     * Each block is expressed as 2 bits giving a total of 16 KiB
     */
    private final BitSet data;

    private final Int2ObjectOpenHashMap<String> special;

    /**
     * The block names of each surface level block for generating an overview
     */
    private final BlockState[] overview;

    private final int[] heightMap;

    private final Map<String, List<BetterBlockPos>> specialBlockLocations;
    private final Map<Integer, Integer> specialMetadata;

    public final long cacheTimestamp;

    CachedChunk(int x, int z, int height, BitSet data, BlockState[] overview, Map<String, List<BetterBlockPos>> specialBlockLocations, Map<Integer, Integer> specialMetadata, long cacheTimestamp) {
        this.size = size(height);
        this.sizeInBytes = sizeInBytes(size);
        validateSize(data);

        this.x = x;
        this.z = z;
        this.height = height;
        this.data = data;
        this.overview = overview;
        this.heightMap = new int[256];
        this.specialBlockLocations = specialBlockLocations;
        this.specialMetadata = specialMetadata;
        this.cacheTimestamp = cacheTimestamp;
        if (specialBlockLocations.isEmpty()) {
            this.special = null;
        } else {
            this.special = new Int2ObjectOpenHashMap<>();
            setSpecial();
        }
        calculateHeightMap();
    }

    public static int size(int dimension_height) {
        return 2 * 16 * 16 * dimension_height;
    }

    public static int sizeInBytes(int size) {
        return size / 8;
    }

    private final void setSpecial() {
        for (Map.Entry<String, List<BetterBlockPos>> entry : specialBlockLocations.entrySet()) {
            for (BetterBlockPos pos : entry.getValue()) {
                special.put(getPositionIndex(pos.getX(), pos.getY(), pos.getZ()), entry.getKey());
            }
        }
    }

    public final BlockState getBlock(int x, int y, int z, int dimensionId) {
        int index = getPositionIndex(x, y, z);
        PathingBlockType type = getType(index);
        int internalPos = z << 4 | x;
        if (heightMap[internalPos] == y && type != PathingBlockType.AVOID) {
            // if the top block in a column is water, we cache it as AVOID but we don't want to just return default state water (which is not flowing) beacuse then it would try to path through it

            // we have this exact block, it's a surface block
            /*System.out.println("Saying that " + x + "," + y + "," + z + " is " + state);
            if (!Minecraft.getInstance().world.getBlockState(new BlockPos(x + this.x * 16, y, z + this.z * 16)).getBlock().equals(state.getBlock())) {
                throw new IllegalStateException("failed " + Minecraft.getInstance().world.getBlockState(new BlockPos(x + this.x * 16, y, z + this.z * 16)).getBlock() + " " + state.getBlock() + " " + (x + this.x * 16) + " " + y + " " + (z + this.z * 16));
            }*/
            return overview[internalPos];
        }
        if (special != null) {
            String str = special.get(index);
            if (str != null) {
                return new BlockState(BlockUtils.stringToBlockRequired(str), specialMetadata.getOrDefault(index, 0));
            }
        }

        if (type == PathingBlockType.SOLID) {
            if (y == 127 && dimensionId == -1) {
                // nether roof is always unbreakable
                return new BlockState(Blocks.bedrock, 0);
            }

            if ((dimensionId == 0 || dimensionId == -1) && y < 5) {
                // solid blocks below 5 are commonly bedrock
                // however, returning bedrock always would be a little yikes
                // discourage paths that include breaking blocks below 5 a little more heavily just so that it takes paths breaking what's known to be stone (at 5 or above) instead of what could maybe be bedrock (below 5)
                return new BlockState(Blocks.obsidian, 0);
            }
        }
        return ChunkPacker.pathingTypeToBlock(type, dimensionId);
    }

    private PathingBlockType getType(int index) {
        return PathingBlockType.fromBits(data.get(index), data.get(index + 1));
    }

    private void calculateHeightMap() {
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int index = z << 4 | x;
                heightMap[index] = 0;
                for (int y = height; y >= 0; y--) {
                    int i = getPositionIndex(x, y, z);
                    if (data.get(i) || data.get(i + 1)) {
                        heightMap[index] = y;
                        break;
                    }
                }
            }
        }
    }

    public final BlockState[] getOverview() {
        return overview;
    }

    public final Map<String, List<BetterBlockPos>> getRelativeBlocks() {
        return specialBlockLocations;
    }

    public int getSpecialMetadata(BetterBlockPos pos) {
        return specialMetadata.getOrDefault(getPositionIndex(pos.x, pos.y, pos.z), 0);
    }

    public final ArrayList<BetterBlockPos> getAbsoluteBlocks(String blockType) {
        if (specialBlockLocations.get(blockType) == null) {
            return null;
        }
        ArrayList<BetterBlockPos> res = new ArrayList<>();
        for (BetterBlockPos pos : specialBlockLocations.get(blockType)) {
            res.add(new BetterBlockPos(pos.getX() + x * 16, pos.getY(), pos.getZ() + z * 16));
        }
        return res;
    }

    /**
     * @return Returns the raw packed chunk data as a byte array
     */
    public final byte[] toByteArray() {
        return this.data.toByteArray();
    }

    /**
     * Returns the raw bit index of the specified position
     *
     * @param x The x position
     * @param y The y position
     * @param z The z position
     * @return The bit index
     */
    public static int getPositionIndex(int x, int y, int z) {
        return (x << 1) | (z << 5) | (y << 9);
    }

    /**
     * Validates the size of an input {@link BitSet} containing the raw
     * packed chunk data. Sizes that exceed {@link CachedChunk#size} are
     * considered invalid, and thus, an exception will be thrown.
     *
     * @param data The raw data
     * @throws IllegalArgumentException if the bitset size exceeds the maximum size
     */
    private void validateSize(BitSet data) {
        if (data.size() > size) {
            throw new IllegalArgumentException("BitSet of invalid length provided");
        }
    }
}
