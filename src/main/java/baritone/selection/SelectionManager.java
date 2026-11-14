/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.selection;

import baritone.Baritone;
import baritone.api.selection.ISelection;
import baritone.api.selection.ISelectionManager;
import baritone.api.utils.BetterBlockPos;
import java.util.LinkedList;
import java.util.ListIterator;
import net.minecraftforge.common.util.ForgeDirection;

public class SelectionManager implements ISelectionManager {

    private final LinkedList<ISelection> selections = new LinkedList<>();
    private ISelection[] selectionsArr = new ISelection[0];

    public SelectionManager(Baritone baritone) {
        // SelectionRenderer initialized when rendering is ported
    }

    private void resetSelectionsArr() {
        selectionsArr = selections.toArray(new ISelection[0]);
    }

    @Override
    public synchronized ISelection addSelection(ISelection selection) {
        selections.add(selection);
        resetSelectionsArr();
        return selection;
    }

    @Override
    public ISelection addSelection(BetterBlockPos pos1, BetterBlockPos pos2) {
        return addSelection(new Selection(pos1, pos2));
    }

    @Override
    public synchronized ISelection removeSelection(ISelection selection) {
        selections.remove(selection);
        resetSelectionsArr();
        return selection;
    }

    @Override
    public synchronized ISelection[] removeAllSelections() {
        ISelection[] selectionsArr = getSelections();
        selections.clear();
        resetSelectionsArr();
        return selectionsArr;
    }

    @Override
    public ISelection[] getSelections() {
        return selectionsArr;
    }

    @Override
    public synchronized ISelection getOnlySelection() {
        if (selections.size() == 1) {
            return selections.peekFirst();
        }

        return null;
    }

    @Override
    public ISelection getLastSelection() {
        return selections.peekLast();
    }

    @Override
    public synchronized ISelection expand(ISelection selection, ForgeDirection direction, int blocks) {
        for (ListIterator<ISelection> it = selections.listIterator(); it.hasNext(); ) {
            ISelection current = it.next();

            if (current == selection) {
                it.remove();
                it.add(current.expand(direction, blocks));
                resetSelectionsArr();
                return it.previous();
            }
        }

        return null;
    }

    @Override
    public synchronized ISelection contract(ISelection selection, ForgeDirection direction, int blocks) {
        for (ListIterator<ISelection> it = selections.listIterator(); it.hasNext(); ) {
            ISelection current = it.next();

            if (current == selection) {
                it.remove();
                it.add(current.contract(direction, blocks));
                resetSelectionsArr();
                return it.previous();
            }
        }

        return null;
    }

    @Override
    public synchronized ISelection shift(ISelection selection, ForgeDirection direction, int blocks) {
        for (ListIterator<ISelection> it = selections.listIterator(); it.hasNext(); ) {
            ISelection current = it.next();

            if (current == selection) {
                it.remove();
                it.add(current.shift(direction, blocks));
                resetSelectionsArr();
                return it.previous();
            }
        }

        return null;
    }
}
