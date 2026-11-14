/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.utils;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.client.renderer.entity.RenderManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public interface IRenderer {

    Settings settings = BaritoneAPI.getSettings();

    static void glColor(Color color, float alpha) {
        float[] c = color.getColorComponents(null);
        GL11.glColor4f(c[0], c[1], c[2], alpha);
    }

    static void drawAABB(AxisAlignedBB aabb, Color color, float opacity, float lineWidth, boolean ignoreDepth) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glPushMatrix();
        GL11.glTranslated(-RenderManager.renderPosX, -RenderManager.renderPosY, -RenderManager.renderPosZ);
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
        GL11.glEnd();

        if (ignoreDepth) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}
