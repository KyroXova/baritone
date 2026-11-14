/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.selection;

import baritone.Baritone;
import baritone.api.event.events.RenderEvent;
import baritone.api.event.listener.AbstractGameEventListener;
import baritone.api.selection.ISelection;
import baritone.api.utils.BetterBlockPos;
import baritone.utils.IRenderer;
import net.minecraft.util.AxisAlignedBB;

public class SelectionRenderer implements IRenderer, AbstractGameEventListener {

    public static final double SELECTION_BOX_EXPANSION = .005D;

    private final SelectionManager manager;

    SelectionRenderer(Baritone baritone, SelectionManager manager) {
        this.manager = manager;
        baritone.getGameEventHandler().registerEventListener(this);
    }

    public static void renderSelections(ISelection[] selections) {
        float opacity = settings.selectionOpacity.value;
        boolean ignoreDepth = settings.renderSelectionIgnoreDepth.value;
        float lineWidth = settings.selectionLineWidth.value;

        if (!settings.renderSelection.value || selections.length == 0) {
            return;
        }

        for (ISelection selection : selections) {
            AxisAlignedBB box = selection.aabb().expand(SELECTION_BOX_EXPANSION, SELECTION_BOX_EXPANSION, SELECTION_BOX_EXPANSION);
            IRenderer.drawAABB(box, settings.colorSelection.value, opacity, lineWidth, ignoreDepth);
        }

        if (settings.renderSelectionCorners.value) {
            for (ISelection selection : selections) {
                BetterBlockPos pos1 = selection.pos1();
                AxisAlignedBB b1 = AxisAlignedBB.getBoundingBox(pos1.x, pos1.y, pos1.z, pos1.x + 1, pos1.y + 1, pos1.z + 1);
                IRenderer.drawAABB(b1, settings.colorSelectionPos1.value, opacity, lineWidth, ignoreDepth);

                BetterBlockPos pos2 = selection.pos2();
                AxisAlignedBB b2 = AxisAlignedBB.getBoundingBox(pos2.x, pos2.y, pos2.z, pos2.x + 1, pos2.y + 1, pos2.z + 1);
                IRenderer.drawAABB(b2, settings.colorSelectionPos2.value, opacity, lineWidth, ignoreDepth);
            }
        }
    }

    @Override
    public void onRenderPass(RenderEvent event) {
        renderSelections(manager.getSelections());
    }
}
