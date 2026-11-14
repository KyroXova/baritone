/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.command;

import baritone.api.IBaritone;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.datatypes.*;
import baritone.api.command.helpers.Paginator;
import baritone.api.command.helpers.TabCompleteHelper;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.pathing.goals.GoalYLevel;
import baritone.api.schematic.Axis;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockOptionalMeta;
import baritone.command.argument.ArgConsumer;
import baritone.command.argument.CommandArguments;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CommandDatatypesTest {

    private static IDatatypeContext createContext(String input) {
        return createContext(null, input, false);
    }

    private static IDatatypeContext createContext(String input, boolean preserveEmptyLast) {
        return createContext(null, input, preserveEmptyLast);
    }

    private static IDatatypeContext createContext(IBaritone baritone, String input, boolean preserveEmptyLast) {
        IArgConsumer consumer = new ArgConsumer(null, CommandArguments.from(input, preserveEmptyLast));
        return new IDatatypeContext() {
            @Override
            public IBaritone getBaritone() {
                return baritone;
            }

            @Override
            public IArgConsumer getConsumer() {
                return consumer;
            }
        };
    }

    @Test
    public void testForAxis() throws Exception {
        IDatatypeContext ctxX = createContext("x");
        assertEquals(Axis.X, ForAxis.INSTANCE.get(ctxX));

        IDatatypeContext ctxY = createContext("Y");
        assertEquals(Axis.Y, ForAxis.INSTANCE.get(ctxY));

        IDatatypeContext ctxZ = createContext("z");
        assertEquals(Axis.Z, ForAxis.INSTANCE.get(ctxZ));

        IDatatypeContext ctxTab = createContext("x", true);
        List<String> completions = ForAxis.INSTANCE.tabComplete(ctxTab).collect(Collectors.toList());
        assertEquals(1, completions.size());
        assertEquals("x", completions.get(0));
    }

    @Test
    public void testForDirection() throws Exception {
        IDatatypeContext ctxUp = createContext("up");
        assertEquals(ForgeDirection.UP, ForDirection.INSTANCE.get(ctxUp));

        IDatatypeContext ctxDown = createContext("DOWN");
        assertEquals(ForgeDirection.DOWN, ForDirection.INSTANCE.get(ctxDown));

        IDatatypeContext ctxNorth = createContext("north");
        assertEquals(ForgeDirection.NORTH, ForDirection.INSTANCE.get(ctxNorth));

        IDatatypeContext ctxTab = createContext("u", true);
        List<String> completions = ForDirection.INSTANCE.tabComplete(ctxTab).collect(Collectors.toList());
        assertTrue(completions.contains("up"));
    }

    @Test
    public void testRelativeCoordinate() throws Exception {
        IDatatypeContext ctx1 = createContext("~5");
        assertEquals(15.0, RelativeCoordinate.INSTANCE.apply(ctx1, 10.0), 0.001);

        IDatatypeContext ctx2 = createContext("~");
        assertEquals(10.0, RelativeCoordinate.INSTANCE.apply(ctx2, 10.0), 0.001);

        IDatatypeContext ctx3 = createContext("25");
        assertEquals(25.0, RelativeCoordinate.INSTANCE.apply(ctx3, 10.0), 0.001);

        IDatatypeContext ctx4 = createContext("~-3.5");
        assertEquals(6.5, RelativeCoordinate.INSTANCE.apply(ctx4, 10.0), 0.001);

        IDatatypeContext ctx5 = createContext("~1k");
        assertEquals(1010.0, RelativeCoordinate.INSTANCE.apply(ctx5, 10.0), 0.001);
    }

    @Test
    public void testRelativeBlockPos() throws Exception {
        BetterBlockPos origin = new BetterBlockPos(10, 20, 30);

        IDatatypeContext ctxRel = createContext("~1 ~2 ~3");
        BetterBlockPos resultRel = RelativeBlockPos.INSTANCE.apply(ctxRel, origin);
        assertEquals(new BetterBlockPos(11, 22, 33), resultRel);

        IDatatypeContext ctxAbs = createContext("100 200 -50");
        BetterBlockPos resultAbs = RelativeBlockPos.INSTANCE.apply(ctxAbs, origin);
        assertEquals(new BetterBlockPos(100, 200, -50), resultAbs);
    }

    @Test
    public void testRelativeGoal() throws Exception {
        BetterBlockPos origin = new BetterBlockPos(10, 20, 30);

        IDatatypeContext ctxBlock = createContext("~1 ~2 ~3");
        Goal goalBlock = RelativeGoal.INSTANCE.apply(ctxBlock, origin);
        assertTrue(goalBlock instanceof GoalBlock);
        assertEquals(new BetterBlockPos(11, 22, 33), ((GoalBlock) goalBlock).getGoalPos());

        IDatatypeContext ctxXZ = createContext("~5 ~-5");
        Goal goalXZ = RelativeGoal.INSTANCE.apply(ctxXZ, origin);
        assertTrue(goalXZ instanceof GoalXZ);
        assertEquals(15, ((GoalXZ) goalXZ).getX());
        assertEquals(25, ((GoalXZ) goalXZ).getZ());

        IDatatypeContext ctxY = createContext("~10");
        Goal goalY = RelativeGoal.INSTANCE.apply(ctxY, origin);
        assertTrue(goalY instanceof GoalYLevel);
        assertEquals(30, ((GoalYLevel) goalY).level);

        IDatatypeContext ctxEmpty = createContext("");
        Goal goalEmpty = RelativeGoal.INSTANCE.apply(ctxEmpty, origin);
        assertTrue(goalEmpty instanceof GoalBlock);
        assertEquals(origin, ((GoalBlock) goalEmpty).getGoalPos());
    }

    @Test
    public void testTabCompleteHelper() {
        TabCompleteHelper helper = new TabCompleteHelper();
        helper.append("apple", "banana", "orange");
        helper.filterPrefix("ba");
        List<String> list = helper.stream().collect(Collectors.toList());
        assertEquals(1, list.size());
        assertEquals("banana", list.get(0));

        TabCompleteHelper namespaced = new TabCompleteHelper();
        namespaced.append("minecraft:stone", "minecraft:dirt", "custom:stone");
        namespaced.filterPrefixNamespaced("stone");
        List<String> matched = namespaced.stream().collect(Collectors.toList());
        assertEquals(2, matched.size());
        assertTrue(matched.contains("minecraft:stone"));
        assertTrue(matched.contains("custom:stone"));
    }

    @Test
    public void testPaginator() {
        Paginator<String> paginator = new Paginator<>("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        paginator.setPageSize(4);
        assertEquals(3, paginator.getMaxPage());
        assertTrue(paginator.validPage(1));
        assertTrue(paginator.validPage(3));
        assertFalse(paginator.validPage(0));
        assertFalse(paginator.validPage(4));
    }
}
