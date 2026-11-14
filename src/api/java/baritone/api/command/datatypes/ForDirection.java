/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.command.datatypes;

import baritone.api.command.exception.CommandException;
import baritone.api.command.helpers.TabCompleteHelper;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Locale;
import java.util.stream.Stream;

public enum ForDirection implements IDatatypeFor<ForgeDirection> {
    INSTANCE;

    @Override
    public ForgeDirection get(IDatatypeContext ctx) throws CommandException {
        return ForgeDirection.valueOf(ctx.getConsumer().getString().toUpperCase(Locale.US));
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        return new TabCompleteHelper()
                .append(Stream.of(ForgeDirection.VALID_DIRECTIONS)
                        .map(d -> d.name().toLowerCase(Locale.US)))
                .filterPrefix(ctx.getConsumer().getString())
                .stream();
    }
}
