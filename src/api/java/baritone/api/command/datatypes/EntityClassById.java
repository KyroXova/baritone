/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.command.datatypes;

import baritone.api.command.exception.CommandException;
import baritone.api.command.helpers.TabCompleteHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;

import java.util.Map;
import java.util.stream.Stream;

public enum EntityClassById implements IDatatypeFor<Class<? extends Entity>> {
    INSTANCE;

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends Entity> get(IDatatypeContext ctx) throws CommandException {
        String name = ctx.getConsumer().getString();
        Class<? extends Entity> clazz = (Class<? extends Entity>) EntityList.stringToClassMapping.get(name);
        if (clazz == null) {
            throw new IllegalArgumentException("No entity found by that id: " + name);
        }
        return clazz;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        return new TabCompleteHelper()
                .append(((Map<String, Class<? extends Entity>>) EntityList.stringToClassMapping).keySet().stream())
                .filterPrefix(ctx.getConsumer().getString())
                .sortAlphabetically()
                .stream();
    }
}
