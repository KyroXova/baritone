/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.utils;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import net.minecraft.util.AxisAlignedBB;
import baritone.api.utils.BetterBlockPos;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Collection;

public interface IRenderer {

    Settings settings = BaritoneAPI.getSettings();

    static void glColor(Color color, float alpha) {
        float[] c = color.getColorComponents(null);
        GL11.glColor4f(c[0], c[1], c[2], alpha);
    }

    static void drawAABB(AxisAlignedBB aabb, Color color, float opacity, float lineWidth, boolean ignoreDepth) {
        beginBoxes(color, opacity, lineWidth, ignoreDepth);
        emitAABB(aabb);
        endBoxes();
    }

    static void drawBlockBoxes(Collection<BetterBlockPos> positions, Color color, float opacity, float lineWidth, boolean ignoreDepth) {
        if (positions.isEmpty()) return;
        beginBoxes(color, opacity, lineWidth, ignoreDepth);
        for (BetterBlockPos pos : positions) {
            emitAABB(AxisAlignedBB.getBoundingBox(pos.x, pos.y, pos.z, pos.x + 1, pos.y + 1, pos.z + 1));
        }
        endBoxes();
    }

    static void beginBoxes(Color color, float opacity, float lineWidth, boolean ignoreDepth) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushMatrix();
        GL11.glTranslated(-WorldRenderOrigin.x(), -WorldRenderOrigin.y(), -WorldRenderOrigin.z());
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        if (ignoreDepth) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        GL11.glLineWidth(lineWidth);
        glColor(color, opacity);

        GL11.glBegin(GL11.GL_LINES);
    }

    static void emitAABB(AxisAlignedBB aabb) {
        // bottom
        GL11.glVertex3d(aabb.minX, aabb.minY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.minY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.minY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.minY, aabb.maxZ);
        GL11.glVertex3d(aabb.maxX, aabb.minY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.minY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.minY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.minY, aabb.minZ);
        // top
        GL11.glVertex3d(aabb.minX, aabb.maxY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.maxY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.maxY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.maxY, aabb.maxZ);
        GL11.glVertex3d(aabb.maxX, aabb.maxY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.maxY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.maxY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.maxY, aabb.minZ);
        // vertical
        GL11.glVertex3d(aabb.minX, aabb.minY, aabb.minZ);
        GL11.glVertex3d(aabb.minX, aabb.maxY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.minY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.maxY, aabb.minZ);
        GL11.glVertex3d(aabb.maxX, aabb.minY, aabb.maxZ);
        GL11.glVertex3d(aabb.maxX, aabb.maxY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.minY, aabb.maxZ);
        GL11.glVertex3d(aabb.minX, aabb.maxY, aabb.maxZ);
    }

    static void endBoxes() {
        GL11.glEnd();
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}
