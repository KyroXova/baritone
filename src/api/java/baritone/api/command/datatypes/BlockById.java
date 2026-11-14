/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.command.datatypes;

import baritone.api.command.exception.CommandException;
import baritone.api.command.helpers.TabCompleteHelper;
import baritone.api.utils.BlockUtils;
import net.minecraft.block.Block;

import java.util.stream.Stream;

public enum BlockById implements IDatatypeFor<Block> {
    INSTANCE;

    @Override
    public Block get(IDatatypeContext ctx) throws CommandException {
        String name = ctx.getConsumer().getString();
        try {
            return BlockUtils.stringToBlockRequired(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("no block found by that id: " + name);
        }
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        String arg = ctx.getConsumer().getString();

        @SuppressWarnings("unchecked")
        java.util.Set<String> keys = (java.util.Set<String>) Block.blockRegistry.getKeys();

        return new TabCompleteHelper()
                .append(keys.stream())
                .filterPrefix(arg)
                .sortAlphabetically()
                .stream();
    }
}
