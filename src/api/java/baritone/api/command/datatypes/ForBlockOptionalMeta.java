/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.command.datatypes;

import baritone.api.command.exception.CommandException;
import baritone.api.utils.BlockOptionalMeta;

import java.util.stream.Stream;

public enum ForBlockOptionalMeta implements IDatatypeFor<BlockOptionalMeta> {
    INSTANCE;

    @Override
    public BlockOptionalMeta get(IDatatypeContext ctx) throws CommandException {
        return new BlockOptionalMeta(ctx.getConsumer().getString());
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        String arg = ctx.getConsumer().peekString();

        if (arg.contains(":") || arg.contains("[")) {
            // Optional metadata, leave to user input
            ctx.getConsumer().getString();
            return Stream.empty();
        }

        return ctx.getConsumer().tabCompleteDatatype(BlockById.INSTANCE);
    }
}
