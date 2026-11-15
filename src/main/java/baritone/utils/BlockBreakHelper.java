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

import baritone.api.BaritoneAPI;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.BetterBlockPos;
import baritone.utils.accessor.IPlayerControllerMP;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * @author Brady
 * @since 8/25/2018
 */
public final class BlockBreakHelper {
    // base ticks between block breaks caused by tick logic
    private static final int BASE_BREAK_DELAY = 1;

    private final IPlayerContext ctx;
    private boolean wasHitting;
    private int breakDelayTimer = 0;

    BlockBreakHelper(IPlayerContext ctx) {
        this.ctx = ctx;
    }

    public static boolean shouldPreventVanillaReset(boolean playerClick, boolean automatedClick) {
        return automatedClick && !playerClick;
    }

    public void stopBreakingBlock() {
        // The player controller will never be null, but the player can be
        if (ctx.player() != null && wasHitting) {
            ctx.playerController().setHittingBlock(false);
            ctx.playerController().resetBlockRemoving();
            wasHitting = false;
        }
    }

    public void tick(boolean isLeftClick) {
        if (breakDelayTimer > 0) {
            breakDelayTimer--;
            return;
        }
        MovingObjectPosition trace = ctx.objectMouseOver();
        boolean isBlockTrace = trace != null && trace.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;

        if (isLeftClick && isBlockTrace) {
            BetterBlockPos pos = new BetterBlockPos(trace.blockX, trace.blockY, trace.blockZ);
            ForgeDirection side = ForgeDirection.getOrientation(trace.sideHit);
            ctx.playerController().setHittingBlock(wasHitting);
            if (ctx.playerController().hasBrokenBlock()) {
                ctx.playerController().syncHeldItem();
                ctx.playerController().clickBlock(pos, side);
                ctx.player().swingItem();
            } else {
                ctx.playerController().onPlayerDamageBlock(pos, side);
                ctx.player().swingItem();
                if (ctx.playerController().hasBrokenBlock()) { // block broken this tick
                    // break delay timer only applies for multi-tick block breaks like vanilla
                    breakDelayTimer = BaritoneAPI.getSettings().blockBreakSpeed.value - BASE_BREAK_DELAY;
                    // must reset controller's destroy delay to prevent the client from delaying itself unnecessarily
                    ((IPlayerControllerMP) ctx.minecraft().playerController).setDestroyDelay(0);
                }
            }
            // if true, we're breaking a block. if false, we broke the block this tick
            wasHitting = !ctx.playerController().hasBrokenBlock();
            // Restore this state on the next tick; the Minecraft mixin preserves accumulated damage
            // while automated mining is active without forcing the player's attack key.
            ctx.playerController().setHittingBlock(false);
        } else {
            wasHitting = false;
        }
    }
}
