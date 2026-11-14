/* This file is part of Baritone, distributed under the LGPL-3.0-or-later. */
package baritone.utils;

import baritone.api.utils.IInventoryControl;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.IBlockInteraction;
import baritone.api.utils.BetterBlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;
import java.util.ArrayList;
import java.util.List;

public final class InventoryControl implements IInventoryControl {
    private final IPlayerContext ctx;
    private final IBlockInteraction blocks;

    public InventoryControl(IPlayerContext ctx, IBlockInteraction blocks) { this.ctx = ctx; this.blocks = blocks; }

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

    @Override
    public Result useHotbarItem(int slot) {
        Result result = available();
        if (result != Result.ATTEMPTED) return result;
        if (slot < 0 || slot >= 9) return Result.INVALID_SLOT;
        if (ctx.player().inventory.getStackInSlot(slot) == null) return Result.NO_ITEM;
        ctx.player().inventory.currentItem = slot;
        ctx.playerController().syncHeldItem();
        ctx.playerController().processRightClick(ctx.player(), ctx.world());
        return Result.ATTEMPTED;
    }

    @Override
    public IBlockInteraction.Result openBlockContainer(BetterBlockPos pos, ForgeDirection face, Vec3 hit, boolean requireLineOfSight) {
        return blocks.useBlock(pos, face, hit, false, requireLineOfSight);
    }

    @Override
    public ContainerSnapshot snapshotOpenContainer() {
        Minecraft mc = ctx.minecraft();
        if (!mc.func_152345_ab()) throw new IllegalStateException("Must run on the client thread");
        EntityClientPlayerMP player = ctx.player();
        if (player == null || ctx.world() == null || mc.playerController == null
                || !(mc.currentScreen instanceof GuiContainer) || player.openContainer == null
                || ((GuiContainer) mc.currentScreen).inventorySlots != player.openContainer) return null;
        Container container = player.openContainer;
        List<ItemStack> slots = new ArrayList<>(container.inventorySlots.size());
        for (Object obj : container.inventorySlots) {
            ItemStack stack = ((Slot) obj).getStack();
            slots.add(stack == null ? null : stack.copy());
        }
        ItemStack cursor = player.inventory.getItemStack();
        return new ContainerSnapshot(container, slots, cursor == null ? null : cursor.copy());
    }

    @Override
    public Result clickContainerSlot(ContainerSnapshot snapshot, int slot, Click click, int button) {
        Minecraft mc = ctx.minecraft();
        if (!mc.func_152345_ab()) return Result.WRONG_THREAD;
        EntityClientPlayerMP player = ctx.player();
        if (player == null || ctx.world() == null || mc.playerController == null || player.sendQueue == null
                || !(mc.currentScreen instanceof GuiContainer) || player.openContainer == null
                || ((GuiContainer) mc.currentScreen).inventorySlots != player.openContainer) return Result.UNAVAILABLE;
        Container container = player.openContainer;
        if (snapshot == null || !snapshot.identifies(container)) return Result.STALE_WINDOW;
        if (slot < 0 || slot >= container.inventorySlots.size() || click == null
                || (click == Click.HOTBAR_SWAP && (button < 0 || button >= 9))) return Result.INVALID_SLOT;
        int mode = click == Click.QUICK_MOVE ? 1 : click == Click.HOTBAR_SWAP ? 2 : 0;
        int actualButton = click == Click.RIGHT ? 1 : click == Click.HOTBAR_SWAP ? button : 0;
        ctx.playerController().windowClick(container.windowId, slot, actualButton, mode, player);
        return Result.ATTEMPTED;
    }
}
