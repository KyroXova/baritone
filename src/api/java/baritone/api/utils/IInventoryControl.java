/* This file is part of Baritone, distributed under the LGPL-3.0-or-later. */
package baritone.api.utils;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Container;

/** Immediate client-thread inventory operations. Callers manage timing and server confirmation. */
public interface IInventoryControl {
    enum Result { ATTEMPTED, UNAVAILABLE, WRONG_THREAD, INVALID_SLOT, CURSOR_OCCUPIED, STALE_WINDOW }

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

    /** Returns copied item stacks, or null if there is no usable open container. Client thread only. */
    ContainerSnapshot snapshotOpenContainer();

    /**
     * Click a slot in the open container using a recent snapshot.
     * HOTBAR_SWAP requires button 0 through 8; other actions ignore button.
     * ATTEMPTED is not confirmation that the server accepted the transaction.
     */
    Result clickContainerSlot(ContainerSnapshot snapshot, int slot, Click click, int button);
}
