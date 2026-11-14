/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.command.defaults;

import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BetterBlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class SurfaceCommand extends Command {

    protected SurfaceCommand(IBaritone baritone) {
        super(baritone, "surface", "top");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        final BetterBlockPos playerPos = ctx.playerFeet();
        final int surfaceLevel = 63;
        final int worldHeight = ctx.world().getActualHeight();

        // Ensure this command will not run if you are above the surface level and the block above you is air
        // As this would imply that your are already on the open surface
        if (playerPos.y > surfaceLevel && ctx.world().isAirBlock(playerPos.x, playerPos.y + 1, playerPos.z)) {
            logDirect("Already at surface");
            return;
        }

        final int startingYPos = Math.max(playerPos.y, surfaceLevel);

        for (int currentIteratedY = startingYPos; currentIteratedY < worldHeight; currentIteratedY++) {
            final BetterBlockPos newPos = new BetterBlockPos(playerPos.x, currentIteratedY, playerPos.z);

            if (!ctx.world().isAirBlock(newPos.x, newPos.y, newPos.z) && newPos.y > playerPos.y) {
                Goal goal = new GoalBlock(newPos.up());
                logDirect(String.format("Going to: %s", goal.toString()));
                baritone.getCustomGoalProcess().setGoalAndPath(goal);
                return;
            }
        }

        logDirect("Couldn't find the surface");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Escape to the surface";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "The surface command tells Baritone to head towards the surface.",
                "",
                "Usage:",
                "> surface - Head towards the surface."
        );
    }
}
