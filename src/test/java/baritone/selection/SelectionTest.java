/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.selection;

import baritone.api.selection.ISelection;
import baritone.api.selection.ISelectionManager;
import baritone.api.utils.BetterBlockPos;
import net.minecraftforge.common.util.ForgeDirection;
import org.junit.Test;

import static org.junit.Assert.*;

public class SelectionTest {

    @Test
    public void testSelectionMinMaxAndSize() {
        BetterBlockPos p1 = new BetterBlockPos(10, 60, 20);
        BetterBlockPos p2 = new BetterBlockPos(15, 64, 25);

        Selection sel = new Selection(p1, p2);
        assertEquals(p1, sel.pos1());
        assertEquals(p2, sel.pos2());
        assertEquals(new BetterBlockPos(10, 60, 20), sel.min());
        assertEquals(new BetterBlockPos(15, 64, 25), sel.max());
        assertEquals(new BetterBlockPos(6, 5, 6), sel.size());

        // Reverse order of corners gives same min, max, size
        Selection selReverse = new Selection(p2, p1);
        assertEquals(new BetterBlockPos(10, 60, 20), selReverse.min());
        assertEquals(new BetterBlockPos(15, 64, 25), selReverse.max());
        assertEquals(new BetterBlockPos(6, 5, 6), selReverse.size());
    }

    @Test
    public void testExpandContractShift() {
        BetterBlockPos p1 = new BetterBlockPos(10, 10, 10);
        BetterBlockPos p2 = new BetterBlockPos(20, 20, 20);

        ISelection sel = new Selection(p1, p2);

        // Expand UP by 2
        ISelection expandedUp = sel.expand(ForgeDirection.UP, 2);
        assertEquals(new BetterBlockPos(10, 10, 10), expandedUp.min());
        assertEquals(new BetterBlockPos(20, 22, 20), expandedUp.max());

        // Shift EAST by 5
        ISelection shiftedEast = sel.shift(ForgeDirection.EAST, 5);
        assertEquals(new BetterBlockPos(15, 10, 10), shiftedEast.min());
        assertEquals(new BetterBlockPos(25, 20, 20), shiftedEast.max());

        // Contract UP by 1 (shifts bottom up, per javadoc)
        ISelection contractedUp = sel.contract(ForgeDirection.UP, 1);
        assertEquals(new BetterBlockPos(10, 11, 10), contractedUp.min());
        assertEquals(new BetterBlockPos(20, 20, 20), contractedUp.max());
    }

    @Test
    public void testSelectionManager() {
        ISelectionManager manager = new SelectionManager(null);
        assertEquals(0, manager.getSelections().length);
        assertNull(manager.getOnlySelection());
        assertNull(manager.getLastSelection());

        ISelection s1 = manager.addSelection(new BetterBlockPos(0, 0, 0), new BetterBlockPos(5, 5, 5));
        assertEquals(1, manager.getSelections().length);
        assertEquals(s1, manager.getOnlySelection());
        assertEquals(s1, manager.getLastSelection());

        ISelection s2 = manager.addSelection(new BetterBlockPos(10, 10, 10), new BetterBlockPos(15, 15, 15));
        assertEquals(2, manager.getSelections().length);
        assertNull(manager.getOnlySelection()); // multiple selections -> null
        assertEquals(s2, manager.getLastSelection());

        manager.removeAllSelections();
        assertEquals(0, manager.getSelections().length);
    }
}
