/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.api.utils;

import net.minecraft.block.Block;

/** A 1.7.10 block and its four-bit metadata. */
public final class BlockState {
    private final Block block;
    private final int metadata;

    public BlockState(Block block, int metadata) {
        if (block == null || metadata < 0 || metadata > 15) {
            throw new IllegalArgumentException("Expected a block and metadata in the range 0..15");
        }
        this.block = block;
        this.metadata = metadata;
    }

    public Block getBlock() { return block; }

    public int getMetadata() { return metadata; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BlockState)) return false;
        BlockState state = (BlockState) other;
        return block == state.block && metadata == state.metadata;
    }

    @Override
    public int hashCode() {
        return 31 * System.identityHashCode(block) + metadata;
    }
}
