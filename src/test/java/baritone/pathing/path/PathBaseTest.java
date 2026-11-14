/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.pathing.path;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.movement.IMovement;
import baritone.api.utils.BetterBlockPos;
import baritone.utils.pathing.PathBase;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PathBaseTest {

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
            return positions.size();
        }
    }

    @Test
    public void testPathBasePositions() {
        BetterBlockPos start = new BetterBlockPos(0, 64, 0);
        BetterBlockPos end = new BetterBlockPos(5, 64, 0);
        SimplePath path = new SimplePath(Arrays.asList(start, end));

        assertEquals(start, path.getSrc());
        assertEquals(end, path.getDest());
        assertEquals(2, path.length());
        assertNotNull(path.positions());
    }
}
