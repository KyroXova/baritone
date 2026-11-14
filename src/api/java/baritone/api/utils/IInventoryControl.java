/* This file is part of Baritone, distributed under the LGPL-3.0-or-later. */
package baritone.api.utils;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Container;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

/** Immediate client-thread inventory operations. Callers manage timing and server confirmation. */
public interface IInventoryControl {
    enum Result { ATTEMPTED, UNAVAILABLE, WRONG_THREAD, INVALID_SLOT, CURSOR_OCCUPIED, STALE_WINDOW, NO_ITEM }

    enum Click { LEFT, RIGHT, QUICK_MOVE, HOTBAR_SWAP }

    /** Snapshot of an open container; slot indices are container indices, not player inventory indices. */
    final class ContainerSnapshot {
        public final int windowId;
        public final List<ItemStack> slots;
        public final ItemStack cursor;
        private final Container container;

        public ContainerSnapshot(Container container, List<ItemStack> slots, ItemStack cursor) {
            this.container = container;
            this.windowId = container.windowId;
            this.slots = java.util.Collections.unmodifiableList(slots);
            this.cursor = cursor;
        }

        public boolean identifies(Container current) { return container == current && windowId == current.windowId; }
    }

    /** Select and synchronize hotbar slot 0 through 8. */
    Result selectHotbarSlot(int slot);

    /** Swap a main inventory slot (0 through 35) with hotbar slot 0 through 8. */
    Result swapWithHotbar(int inventorySlot, int hotbarSlot);

    /**
     * Select a nonempty hotbar slot and use its held item once, such as a portable bag.
     * Observe {@link #snapshotOpenContainer()} on subsequent client ticks to detect opening.
     * ATTEMPTED does not imply the item opened a container or that the server accepted it.
     */
    Result useHotbarItem(int slot);

    /** Attempt to use a placed container. Observe subsequent ticks for an opened container. */
    IBlockInteraction.Result openBlockContainer(BetterBlockPos pos, ForgeDirection face, Vec3 hit, boolean requireLineOfSight);

    /** Returns copied item stacks, or null if there is no usable open container. Client thread only. */
    ContainerSnapshot snapshotOpenContainer();

    /**
     * Click a slot in the open container using a recent snapshot.
     * HOTBAR_SWAP requires button 0 through 8; other actions ignore button.
     * ATTEMPTED is not confirmation that the server accepted the transaction.
     */
    Result clickContainerSlot(ContainerSnapshot snapshot, int slot, Click click, int button);
}
