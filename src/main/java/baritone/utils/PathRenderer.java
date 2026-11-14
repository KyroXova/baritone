/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.utils;

import baritone.api.event.events.RenderEvent;
import baritone.api.pathing.goals.*;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.interfaces.IGoalRenderPos;
import baritone.behavior.PathingBehavior;
import baritone.pathing.path.PathExecutor;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class PathRenderer implements IRenderer {

    private PathRenderer() {}

    public static double posX() {
        return RenderManager.renderPosX;
    }

    public static double posY() {
        return RenderManager.renderPosY;
    }

    public static double posZ() {
        return RenderManager.renderPosZ;
    }

    public static void render(RenderEvent event, PathingBehavior behavior) {
        final IPlayerContext ctx = behavior.ctx;
        if (ctx.world() == null) {
            return;
        }

        final float partialTicks = event.getPartialTicks();
        final Goal goal = behavior.getGoal();

        if (goal != null && settings.renderGoal.value) {
            drawGoal(ctx, goal, partialTicks, settings.colorGoalBox.value);
        }

        if (!settings.renderPath.value) {
            return;
        }

        PathExecutor current = behavior.getCurrent();
        PathExecutor next = behavior.getNext();
        if (current != null && settings.renderSelectionBoxes.value) {
            drawManySelectionBoxes(current.toBreak(), settings.colorBlocksToBreak.value);
            drawManySelectionBoxes(current.toPlace(), settings.colorBlocksToPlace.value);
            drawManySelectionBoxes(current.toWalkInto(), settings.colorBlocksToWalkInto.value);
        }

        if (current != null && current.getPath() != null) {
            int renderBegin = Math.max(current.getPosition() - 3, 0);
            drawPath(current.getPath().positions(), renderBegin, settings.colorCurrentPath.value, settings.fadePath.value, 10, 20);
        }

        if (next != null && next.getPath() != null) {
            drawPath(next.getPath().positions(), 0, settings.colorNextPath.value, settings.fadePath.value, 10, 20);
        }

        behavior.getInProgress().ifPresent(currentlyRunning -> {
            currentlyRunning.bestPathSoFar().ifPresent(p -> {
                drawPath(p.positions(), 0, settings.colorBestPathSoFar.value, settings.fadePath.value, 10, 20);
            });

            currentlyRunning.pathToMostRecentNodeConsidered().ifPresent(mr -> {
                drawPath(mr.positions(), 0, settings.colorMostRecentConsidered.value, settings.fadePath.value, 10, 20);
                drawManySelectionBoxes(Collections.singletonList(mr.getDest()), settings.colorMostRecentConsidered.value);
            });
        });
    }

    public static void drawPath(List<BetterBlockPos> positions, int startIndex, Color color, boolean fadeOut, int fadeStart0, int fadeEnd0) {
        drawPath(positions, startIndex, color, fadeOut, fadeStart0, fadeEnd0, 0.5D);
    }

    public static void drawPath(List<BetterBlockPos> positions, int startIndex, Color color, boolean fadeOut, int fadeStart0, int fadeEnd0, double offset) {
        if (positions.isEmpty() || startIndex >= positions.size() - 1) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        if (settings.renderPathIgnoreDepth.value) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        GL11.glLineWidth(settings.pathRenderLineWidthPixels.value);

        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        GL11.glColor4f(r, g, b, 0.4f);

        GL11.glBegin(GL11.GL_LINES);

        int fadeStart = fadeStart0 + startIndex;
        int fadeEnd = fadeEnd0 + startIndex;

        for (int i = startIndex, next; i < positions.size() - 1; i = next) {
            BetterBlockPos start = positions.get(i);
            BetterBlockPos end = positions.get(next = i + 1);

            int dirX = end.x - start.x;
            int dirY = end.y - start.y;
            int dirZ = end.z - start.z;

            while (next + 1 < positions.size() && (!fadeOut || next + 1 < fadeStart) &&
                    (dirX == positions.get(next + 1).x - end.x &&
                            dirY == positions.get(next + 1).y - end.y &&
                            dirZ == positions.get(next + 1).z - end.z)) {
                end = positions.get(++next);
            }

            if (fadeOut) {
                float alpha;
                if (i <= fadeStart) {
                    alpha = 0.4F;
                } else {
                    if (i > fadeEnd) {
                        break;
                    }
                    alpha = 0.4F * (1.0F - (float) (i - fadeStart) / (float) (fadeEnd - fadeStart));
                }
                GL11.glColor4f(r, g, b, alpha);
            }

            emitPathLine(start.x, start.y, start.z, end.x, end.y, end.z, offset);
        }

        GL11.glEnd();

        if (settings.renderPathIgnoreDepth.value) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private static void emitPathLine(double x1, double y1, double z1, double x2, double y2, double z2, double offset) {
        final double extraOffset = offset + 0.03D;

        double vpX = posX();
        double vpY = posY();
        double vpZ = posZ();
        boolean renderPathAsFrickinThingy = !settings.renderPathAsLine.value;

        GL11.glVertex3d(x1 + offset - vpX, y1 + offset - vpY, z1 + offset - vpZ);
        GL11.glVertex3d(x2 + offset - vpX, y2 + offset - vpY, z2 + offset - vpZ);

        if (renderPathAsFrickinThingy) {
            GL11.glVertex3d(x2 + offset - vpX, y2 + offset - vpY, z2 + offset - vpZ);
            GL11.glVertex3d(x2 + offset - vpX, y2 + extraOffset - vpY, z2 + offset - vpZ);

            GL11.glVertex3d(x2 + offset - vpX, y2 + extraOffset - vpY, z2 + offset - vpZ);
            GL11.glVertex3d(x1 + offset - vpX, y1 + extraOffset - vpY, z1 + offset - vpZ);

            GL11.glVertex3d(x1 + offset - vpX, y1 + extraOffset - vpY, z1 + offset - vpZ);
            GL11.glVertex3d(x1 + offset - vpX, y1 + offset - vpY, z1 + offset - vpZ);
        }
    }

    public static void drawManySelectionBoxes(Collection<BetterBlockPos> positions, Color color) {
        if (positions == null || positions.isEmpty()) {
            return;
        }
        float opacity = 0.4F;
        float lineWidth = settings.pathRenderLineWidthPixels.value;
        boolean ignoreDepth = settings.renderSelectionBoxesIgnoreDepth.value;

        positions.forEach(pos -> {
            AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(pos.x, pos.y, pos.z, pos.x + 1, pos.y + 1, pos.z + 1);
            IRenderer.drawAABB(aabb, color, opacity, lineWidth, ignoreDepth);
        });
    }

    public static void drawGoal(IPlayerContext ctx, Goal goal, float partialTicks, Color color) {
        float lineWidth = settings.goalRenderLineWidthPixels.value;
        boolean ignoreDepth = settings.renderGoalIgnoreDepth.value;

        if (goal instanceof IGoalRenderPos) {
            BetterBlockPos goalPos = ((IGoalRenderPos) goal).getGoalPos();
            double minY = goalPos.y;
            double maxY = minY + (goal instanceof GoalGetToBlock || goal instanceof GoalTwoBlocks ? 1 : 2);
            AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(goalPos.x, minY, goalPos.z, goalPos.x + 1, maxY, goalPos.z + 1);
            IRenderer.drawAABB(aabb, color, 0.4F, lineWidth, ignoreDepth);
        } else if (goal instanceof GoalXZ) {
            GoalXZ gxz = (GoalXZ) goal;
            AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(gxz.getX(), 0, gxz.getZ(), gxz.getX() + 1, 256, gxz.getZ() + 1);
            IRenderer.drawAABB(aabb, color, 0.2F, lineWidth, ignoreDepth);
        } else if (goal instanceof GoalComposite) {
            for (Goal g : ((GoalComposite) goal).goals()) {
                drawGoal(ctx, g, partialTicks, color);
            }
        } else if (goal instanceof GoalInverted) {
            drawGoal(ctx, ((GoalInverted) goal).origin, partialTicks, settings.colorInvertedGoalBox.value);
        } else if (goal instanceof GoalYLevel) {
            GoalYLevel gy = (GoalYLevel) goal;
            double px = ctx.player().posX;
            double pz = ctx.player().posZ;
            double sz = settings.yLevelBoxSize.value;
            AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(px - sz, gy.level, pz - sz, px + sz, gy.level + 2, pz + sz);
            IRenderer.drawAABB(aabb, color, 0.4F, lineWidth, ignoreDepth);
        }
    }
}
