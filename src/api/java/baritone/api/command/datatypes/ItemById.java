/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.command.datatypes;

import baritone.api.command.exception.CommandException;
import baritone.api.command.helpers.TabCompleteHelper;
import net.minecraft.item.Item;

import java.util.Set;
import java.util.stream.Stream;

public enum ItemById implements IDatatypeFor<Item> {
    INSTANCE;

    @Override
    public Item get(IDatatypeContext ctx) throws CommandException {
        String name = ctx.getConsumer().getString();
        Item item = (Item) Item.itemRegistry.getObject(name);
        if (item == null && !name.contains(":")) {
            item = (Item) Item.itemRegistry.getObject("minecraft:" + name);
        }
        if (item == null) {
            throw new IllegalArgumentException("No item found by that id: " + name);
        }
        return item;
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        @SuppressWarnings("unchecked")
        Set<String> keys = (Set<String>) Item.itemRegistry.getKeys();

        return new TabCompleteHelper()
                .append(keys.stream())
                .filterPrefix(ctx.getConsumer().getString())
                .sortAlphabetically()
                .stream();
    }
}
