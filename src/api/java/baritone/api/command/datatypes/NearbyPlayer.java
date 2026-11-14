/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.command.datatypes;

import baritone.api.IBaritone;
import baritone.api.command.exception.CommandException;
import baritone.api.command.helpers.TabCompleteHelper;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * An {@link IDatatype} used to resolve nearby players, those within
 * render distance of the target {@link IBaritone} instance.
 */
public enum NearbyPlayer implements IDatatypeFor<EntityPlayer> {
    INSTANCE;

    @Override
    public EntityPlayer get(IDatatypeContext ctx) throws CommandException {
        final String username = ctx.getConsumer().getString();
        return getPlayers(ctx).stream()
                .filter(s -> s.getCommandSenderName().equalsIgnoreCase(username))
                .findFirst().orElse(null);
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        return new TabCompleteHelper()
                .append(getPlayers(ctx).stream().map(EntityPlayer::getCommandSenderName))
                .filterPrefix(ctx.getConsumer().getString())
                .sortAlphabetically()
                .stream();
    }

    @SuppressWarnings("unchecked")
    private static List<EntityPlayer> getPlayers(IDatatypeContext ctx) {
        if (ctx.getBaritone().getPlayerContext().world() == null) {
            return Collections.emptyList();
        }
        return (List<EntityPlayer>) ctx.getBaritone().getPlayerContext().world().playerEntities;
    }
}
