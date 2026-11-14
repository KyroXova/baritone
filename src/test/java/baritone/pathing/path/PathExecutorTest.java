/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.path;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.movement.IMovement;
import baritone.api.utils.BetterBlockPos;
import baritone.behavior.PathingBehavior;
import baritone.utils.pathing.PathBase;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class PathExecutorTest {

    private static class SimplePath extends PathBase {
        private final List<BetterBlockPos> positions;

        SimplePath(List<BetterBlockPos> positions) {
            this.positions = positions;
        }

        @Override
        public List<IMovement> movements() {
            return Collections.emptyList();
        }

        @Override
        public List<BetterBlockPos> positions() {
            return positions;
        }

        @Override
        public BetterBlockPos getSrc() {
            return positions.get(0);
        }

        @Override
        public BetterBlockPos getDest() {
            return positions.get(positions.size() - 1);
        }

        @Override
        public Goal getGoal() {
            return null;
        }

        @Override
        public int getNumNodesConsidered() {
            return 0;
        }
    }

    @Test
    public void testPathExecutorInitialization() {
        SimplePath path = new SimplePath(Arrays.asList(
                new BetterBlockPos(0, 64, 0),
                new BetterBlockPos(0, 64, 1)
        ));

        PathingBehavior behavior = new PathingBehavior(null);
        PathExecutor executor = new PathExecutor(behavior, path);

        assertSame(path, executor.getPath());
        assertEquals(0, executor.getPosition());
        assertFalse(executor.failed());
        assertNotNull(executor.toBreak());
        assertNotNull(executor.toPlace());
        assertNotNull(executor.toWalkInto());
        assertTrue(executor.toBreak().isEmpty());
        assertTrue(executor.toPlace().isEmpty());
        assertTrue(executor.toWalkInto().isEmpty());
    }
}
