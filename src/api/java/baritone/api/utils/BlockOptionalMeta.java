/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.utils;

import baritone.api.utils.accessor.IItemStack;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlockOptionalMeta {
    // Matches "stone", "minecraft:stone", "stone:1", "stone[1]", etc.
    private static final Pattern PATTERN = Pattern.compile("^(?<id>[^:\\[\\]]+(?::[^:\\[\\]]+)?)(?:\\[(?<meta>\\d+)\\]|:(?<meta2>\\d+))?$");

    private final Block block;
    private final Integer meta; // null if any metadata
    private final Set<BlockState> blockstates;
    private final ImmutableSet<Integer> stateHashes;
    private final ImmutableSet<Integer> stackHashes;

    public BlockOptionalMeta(@Nonnull Block block) {
        this(block, null);
    }

    public BlockOptionalMeta(@Nonnull Block block, Integer meta) {
        this.block = Objects.requireNonNull(block, "block");
        this.meta = meta;

        Set<BlockState> states = new LinkedHashSet<>();
        if (meta != null) {
            states.add(new BlockState(block, meta));
        } else {
            for (int i = 0; i < 16; i++) {
                states.add(new BlockState(block, i));
            }
        }
        this.blockstates = Collections.unmodifiableSet(states);

        Set<Integer> hashes = new HashSet<>();
        for (BlockState state : states) {
            hashes.add(state.hashCode());
        }
        this.stateHashes = ImmutableSet.copyOf(hashes);

        Item item = Item.getItemFromBlock(block);
        if (item != null) {
            if (meta != null) {
                this.stackHashes = ImmutableSet.of(item.hashCode() + meta);
            } else {
                ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
                builder.add(item.hashCode());
                for (int i = 0; i < 16; i++) {
                    builder.add(item.hashCode() + i);
                }
                this.stackHashes = builder.build();
            }
        } else {
            this.stackHashes = ImmutableSet.of();
        }
    }

    public BlockOptionalMeta(@Nonnull String selector) {
        Matcher matcher = PATTERN.matcher(selector);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid block selector: " + selector);
        }

        this.block = BlockUtils.stringToBlockRequired(matcher.group("id"));
        String metaStr = matcher.group("meta") != null ? matcher.group("meta") : matcher.group("meta2");
        this.meta = metaStr != null ? Integer.parseInt(metaStr) : null;

        Set<BlockState> states = new LinkedHashSet<>();
        if (meta != null) {
            states.add(new BlockState(block, meta));
        } else {
            for (int i = 0; i < 16; i++) {
                states.add(new BlockState(block, i));
            }
        }
        this.blockstates = Collections.unmodifiableSet(states);

        Set<Integer> hashes = new HashSet<>();
        for (BlockState state : states) {
            hashes.add(state.hashCode());
        }
        this.stateHashes = ImmutableSet.copyOf(hashes);

        Item item = Item.getItemFromBlock(block);
        if (item != null) {
            if (meta != null) {
                this.stackHashes = ImmutableSet.of(item.hashCode() + meta);
            } else {
                ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
                builder.add(item.hashCode());
                for (int i = 0; i < 16; i++) {
                    builder.add(item.hashCode() + i);
                }
                this.stackHashes = builder.build();
            }
        } else {
            this.stackHashes = ImmutableSet.of();
        }
    }

    public Block getBlock() {
        return block;
    }

    public Integer getMeta() {
        return meta;
    }

    public boolean matches(Block block) {
        return block != null && block == this.block;
    }

    public boolean matches(BlockState blockstate) {
        return blockstate != null && blockstate.getBlock() == this.block && (meta == null || meta == blockstate.getMetadata());
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        if ((Object) stack instanceof IItemStack) {
            int hash = ((IItemStack) (Object) stack).getBaritoneHash();
            if (stackHashes.contains(hash)) {
                return true;
            }
        }
        Item blockItem = Item.getItemFromBlock(block);
        if (blockItem == null || stack.getItem() != blockItem) {
            return false;
        }
        if (meta == null) {
            return true;
        }
        return stack.getItemDamage() == meta;
    }

    public BlockState getAnyBlockState() {
        return new BlockState(block, meta != null ? meta : 0);
    }

    public Set<BlockState> getAllBlockStates() {
        return blockstates;
    }

    public Set<Integer> stackHashes() {
        return stackHashes;
    }

    @Override
    public String toString() {
        return String.format("BlockOptionalMeta{block=%s,meta=%s}", BlockUtils.blockToString(block), meta == null ? "*" : meta);
    }
}
