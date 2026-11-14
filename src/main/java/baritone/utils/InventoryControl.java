/* This file is part of Baritone, distributed under the LGPL-3.0-or-later. */
package baritone.utils;

import baritone.api.utils.IInventoryControl;
import baritone.api.utils.IPlayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;

public final class InventoryControl implements IInventoryControl {
    private final IPlayerContext ctx;

    public InventoryControl(IPlayerContext ctx) { this.ctx = ctx; }

    private Result available() {
        Minecraft mc = ctx.minecraft();
        if (!mc.func_152345_ab()) return Result.WRONG_THREAD;
        EntityClientPlayerMP player = ctx.player();
        if (player == null || ctx.world() == null || mc.playerController == null || player.sendQueue == null
                || mc.currentScreen != null || player.openContainer != player.inventoryContainer) return Result.UNAVAILABLE;
        return Result.ATTEMPTED;
    }

    @Override
    public Result selectHotbarSlot(int slot) {
        Result result = available();
        if (result != Result.ATTEMPTED) return result;
        if (slot < 0 || slot >= 9) return Result.INVALID_SLOT;
        ctx.player().inventory.currentItem = slot;
        ctx.playerController().syncHeldItem();
        return Result.ATTEMPTED;
    }

    @Override
    public Result swapWithHotbar(int inventorySlot, int hotbarSlot) {
        Result result = available();
        if (result != Result.ATTEMPTED) return result;
        if (inventorySlot < 0 || inventorySlot >= 36 || hotbarSlot < 0 || hotbarSlot >= 9) return Result.INVALID_SLOT;
        EntityClientPlayerMP player = ctx.player();
        if (player.inventory.getItemStack() != null) return Result.CURSOR_OCCUPIED;
        int containerSlot = inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
        ctx.playerController().windowClick(player.inventoryContainer.windowId, containerSlot, hotbarSlot, 2, player);
        return Result.ATTEMPTED;
    }
}
