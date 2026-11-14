/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.api.event;

import baritone.api.event.events.PathEvent;
import baritone.api.event.events.RenderEvent;
import baritone.api.event.events.SprintStateEvent;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import org.junit.Test;

import static org.junit.Assert.*;

public class EventAndProcessTest {

    @Test
    public void testRenderEvent() {
        RenderEvent event = new RenderEvent(0.75f);
        assertEquals(0.75f, event.getPartialTicks(), 0.0001f);
    }

    @Test
    public void testSprintStateEvent() {
        SprintStateEvent event = new SprintStateEvent();
        assertNull(event.getState());

        event.setState(true);
        assertTrue(event.getState());

        event.setState(false);
        assertFalse(event.getState());
    }

    @Test
    public void testPathEvents() {
        assertTrue(PathEvent.values().length > 0);
        assertNotNull(PathEvent.CALC_STARTED);
        assertNotNull(PathEvent.CALC_FINISHED_NOW_EXECUTING);
        assertNotNull(PathEvent.CALC_FAILED);
        assertNotNull(PathEvent.AT_GOAL);
        assertNotNull(PathEvent.CANCELED);
    }

    @Test
    public void testPathingCommand() {
        GoalBlock goal = new GoalBlock(10, 64, 10);
        PathingCommand cmd = new PathingCommand(goal, PathingCommandType.SET_GOAL_AND_PATH);

        assertSame(goal, cmd.goal);
        assertEquals(PathingCommandType.SET_GOAL_AND_PATH, cmd.commandType);
    }
}
