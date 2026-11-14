/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.utils;

import baritone.api.utils.accessor.IItemStack;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class BlockOptionalMetaLookup {
    private final ImmutableSet<Block> blockSet;
    private final ImmutableSet<BlockState> blockStateSet;
    private final ImmutableSet<Integer> stackHashes;
    private final BlockOptionalMeta[] boms;

    public BlockOptionalMetaLookup(BlockOptionalMeta... boms) {
        this.boms = boms;
        Set<Block> blocks = new HashSet<>();
        Set<BlockState> blockStates = new HashSet<>();
        Set<Integer> stacks = new HashSet<>();
        for (BlockOptionalMeta bom : boms) {
            blocks.add(bom.getBlock());
            blockStates.addAll(bom.getAllBlockStates());
            stacks.addAll(bom.stackHashes());
        }
        this.blockSet = ImmutableSet.copyOf(blocks);
        this.blockStateSet = ImmutableSet.copyOf(blockStates);
        this.stackHashes = ImmutableSet.copyOf(stacks);
    }

    public BlockOptionalMetaLookup(Block... blocks) {
        this(Stream.of(blocks)
                .map(BlockOptionalMeta::new)
                .toArray(BlockOptionalMeta[]::new));
    }

    public BlockOptionalMetaLookup(List<Block> blocks) {
        this(blocks.stream()
                .map(BlockOptionalMeta::new)
                .toArray(BlockOptionalMeta[]::new));
    }

    public BlockOptionalMetaLookup(String... blocks) {
        this(Stream.of(blocks)
                .map(BlockOptionalMeta::new)
                .toArray(BlockOptionalMeta[]::new));
    }

    public boolean has(Block block) {
        return blockSet.contains(block);
    }

    public boolean has(BlockState state) {
        return blockStateSet.contains(state);
    }

    public boolean has(ItemStack stack) {
        if (stack == null) return false;
        if ((Object) stack instanceof IItemStack) {
            int hash = ((IItemStack) (Object) stack).getBaritoneHash();
            if (stackHashes.contains(hash)) {
                return true;
            }
        }
        for (BlockOptionalMeta bom : boms) {
            if (bom.matches(stack)) return true;
        }
        return false;
    }

    public List<BlockOptionalMeta> blocks() {
        return Arrays.asList(boms);
    }

    @Override
    public String toString() {
        return String.format(
                "BlockOptionalMetaLookup{%s}",
                Arrays.toString(boms)
        );
    }
}
