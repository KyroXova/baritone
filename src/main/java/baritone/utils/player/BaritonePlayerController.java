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

package baritone.utils.player;

import baritone.api.utils.IPlayerController;
import baritone.api.utils.BetterBlockPos;
import baritone.utils.accessor.IPlayerControllerMP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;


/**
 * Implementation of {@link IPlayerController} that chains to the primary player controller's methods
 *
 * @author Brady
 * @since 12/14/2018
 */
public final class BaritonePlayerController implements IPlayerController {

    private final Minecraft mc;

    public BaritonePlayerController(Minecraft mc) {
        this.mc = mc;
    }

    @Override
    public void syncHeldItem() {
        ((IPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
    }

    @Override
    public boolean hasBrokenBlock() {
        return !((IPlayerControllerMP) mc.playerController).isHittingBlock();
    }

    @Override
    public void onPlayerDamageBlock(BetterBlockPos pos, ForgeDirection side) {
        mc.playerController.onPlayerDamageBlock(pos.x, pos.y, pos.z, side.ordinal());
    }

    @Override
    public void resetBlockRemoving() {
        mc.playerController.resetBlockRemoving();
    }

    @Override
    public void windowClick(int windowId, int slotId, int mouseButton, int mode, EntityPlayer player) {
        mc.playerController.windowClick(windowId, slotId, mouseButton, mode, player);
    }

    @Override
    public GameType getGameType() {
        return ((IPlayerControllerMP) mc.playerController).getCurrentGameType();
    }

    @Override
    public boolean processRightClickBlock(EntityClientPlayerMP player, World world, MovingObjectPosition result) {
        return mc.playerController.onPlayerRightClick(player, world, player.getCurrentEquippedItem(),
                result.blockX, result.blockY, result.blockZ, result.sideHit, result.hitVec);
    }

    @Override
    public boolean processRightClick(EntityClientPlayerMP player, World world) {
        return mc.playerController.sendUseItem(player, world, player.getCurrentEquippedItem());
    }

    @Override
    public void clickBlock(BetterBlockPos loc, ForgeDirection face) {
        mc.playerController.clickBlock(loc.x, loc.y, loc.z, face.ordinal());
    }

    @Override
    public void setHittingBlock(boolean hittingBlock) {
        ((IPlayerControllerMP) mc.playerController).setIsHittingBlock(hittingBlock);
    }
}
