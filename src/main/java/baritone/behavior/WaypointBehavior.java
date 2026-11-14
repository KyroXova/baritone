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

package baritone.behavior;

import baritone.Baritone;
import baritone.api.cache.IWaypoint;
import baritone.api.cache.Waypoint;
import baritone.api.event.events.BlockInteractEvent;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockState;
import baritone.api.utils.Helper;
import baritone.utils.BlockStateInterface;
import net.minecraft.block.BlockBed;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.Direction;
import net.minecraft.util.EnumChatFormatting;

import java.util.Set;

import static baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class WaypointBehavior extends Behavior {

    public WaypointBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void onBlockInteract(BlockInteractEvent event) {
        if (!Baritone.settings().doBedWaypoints.value) {
            return;
        }
        if (event.getType() == BlockInteractEvent.Type.USE) {
            BetterBlockPos pos = BetterBlockPos.from(event.getPos());
            BlockState state = BlockStateInterface.get(ctx, pos);
            if (state.getBlock() instanceof BlockBed) {
                int meta = state.getMetadata();
                if (!BlockBed.isBlockHeadOfBed(meta)) {
                    int dir = meta & 3;
                    pos = new BetterBlockPos(pos.x + Direction.offsetX[dir], pos.y, pos.z + Direction.offsetZ[dir]);
                }
                Set<IWaypoint> waypoints = baritone.getWorldProvider().getCurrentWorld().getWaypoints().getByTag(IWaypoint.Tag.BED);
                boolean exists = waypoints.stream().map(IWaypoint::getLocation).anyMatch(pos::equals);
                if (!exists) {
                    baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(new Waypoint("bed", Waypoint.Tag.BED, pos));
                }
            }
        }
    }

    @Override
    public void onPlayerDeath() {
        if (!Baritone.settings().doDeathWaypoints.value) {
            return;
        }
        Waypoint deathWaypoint = new Waypoint("death", Waypoint.Tag.DEATH, ctx.playerFeet());
        baritone.getWorldProvider().getCurrentWorld().getWaypoints().addWaypoint(deathWaypoint);
        ChatComponentText component = new ChatComponentText("Death position saved.");
        ChatStyle style = new ChatStyle();
        style.setColor(EnumChatFormatting.WHITE);
        style.setChatHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ChatComponentText("Click to goto death")
        ));
        style.setChatClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                String.format(
                        "%s%s goto %s @ %d",
                        FORCE_COMMAND_PREFIX,
                        "wp",
                        deathWaypoint.getTag().getName(),
                        deathWaypoint.getCreationTimestamp()
                )
        ));
        component.setChatStyle(style);
        Helper.HELPER.logDirect(component);
    }
}
