/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.goals;

import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalStrictDirection;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import static org.junit.Assert.*;

public class LegacyGoalTest {
    @Test
    public void positionGoalsUseLegacyCoordinates() {
        BetterBlockPos target = new BetterBlockPos(-3, 64, 7);
        assertTrue(new GoalBlock(target).isInGoal(target));
        assertEquals(target, new GoalBlock(target).getGoalPos());
        assertTrue(new GoalTwoBlocks(target).isInGoal(target.below()));
        assertTrue(new GoalNear(target, 2).isInGoal(target.east(2)));
        assertFalse(new GoalNear(target, 2).isInGoal(target.east(3)));
    }

    @Test
    public void directionsUseForgeAndLegacyVectors() {
        GoalStrictDirection east = new GoalStrictDirection(new BetterBlockPos(0, 64, 0), ForgeDirection.EAST);
        assertTrue(east.heuristic(1, 64, 0) < east.heuristic(0, 64, 0));
        GoalXZ south = GoalXZ.fromDirection(Vec3.createVectorHelper(0, 64, 0), 0, 5);
        assertTrue(south.isInGoal(0, 64, 5));
    }
}
