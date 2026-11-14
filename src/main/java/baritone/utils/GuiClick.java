/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.utils;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.Helper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;

import java.awt.Color;

import static baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class GuiClick extends GuiScreen implements Helper {

    private BetterBlockPos clickStart;
    private BetterBlockPos currentMouseOver;

    public GuiClick() {
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            currentMouseOver = new BetterBlockPos(mc.objectMouseOver.blockX, mc.objectMouseOver.blockY, mc.objectMouseOver.blockZ);
        } else {
            currentMouseOver = null;
        }

        if (currentMouseOver != null) {
            AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(currentMouseOver.x, currentMouseOver.y, currentMouseOver.z, currentMouseOver.x + 1, currentMouseOver.y + 1, currentMouseOver.z + 1);
            IRenderer.drawAABB(aabb, Color.CYAN, 0.4f, Baritone.settings().pathRenderLineWidthPixels.value, true);

            if (clickStart != null && !clickStart.equals(currentMouseOver)) {
                AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                        Math.min(clickStart.x, currentMouseOver.x),
                        Math.min(clickStart.y, currentMouseOver.y),
                        Math.min(clickStart.z, currentMouseOver.z),
                        Math.max(clickStart.x, currentMouseOver.x) + 1,
                        Math.max(clickStart.y, currentMouseOver.y) + 1,
                        Math.max(clickStart.z, currentMouseOver.z) + 1
                );
                IRenderer.drawAABB(box, Color.RED, 0.4f, Baritone.settings().pathRenderLineWidthPixels.value, true);
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        clickStart = currentMouseOver;
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int which) {
        if (which != -1 && currentMouseOver != null) {
            if (which == 0) { // left button
                if (clickStart != null && !clickStart.equals(currentMouseOver)) {
                    BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().removeAllSelections();
                    BaritoneAPI.getProvider().getPrimaryBaritone().getSelectionManager().addSelection(clickStart, currentMouseOver);
                    ChatComponentText component = new ChatComponentText("Selection made! For usage: " + Baritone.settings().prefix.value + "help sel");
                    ChatStyle style = new ChatStyle();
                    style.setColor(EnumChatFormatting.WHITE);
                    style.setChatClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            FORCE_COMMAND_PREFIX + "help sel"
                    ));
                    component.setChatStyle(style);
                    Helper.HELPER.logDirect(component);
                    clickStart = null;
                } else {
                    BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(currentMouseOver));
                }
            } else if (which == 1) { // right button
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalBlock(currentMouseOver.above()));
            }
        }
        clickStart = null;
        super.mouseMovedOrUp(mouseX, mouseY, which);
    }
}
