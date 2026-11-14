/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.command.datatypes;

import baritone.api.command.exception.CommandException;
import baritone.api.command.helpers.TabCompleteHelper;
import baritone.api.schematic.Axis;

import java.util.Locale;
import java.util.stream.Stream;

public enum ForAxis implements IDatatypeFor<Axis> {
    INSTANCE;

    @Override
    public Axis get(IDatatypeContext ctx) throws CommandException {
        return Axis.valueOf(ctx.getConsumer().getString().toUpperCase(Locale.US));
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        return new TabCompleteHelper()
                .append(Stream.of(Axis.values())
                        .map(a -> a.name().toLowerCase(Locale.US)))
                .filterPrefix(ctx.getConsumer().getString())
                .stream();
    }
}
