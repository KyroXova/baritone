/* This file is part of Baritone, distributed under the LGPL-3.0-or-later. */
package baritone.api.utils;

/** Immediate client-thread inventory operations. Callers manage timing and server confirmation. */
public interface IInventoryControl {
    enum Result { ATTEMPTED, UNAVAILABLE, WRONG_THREAD, INVALID_SLOT, CURSOR_OCCUPIED }

    /** Select and synchronize hotbar slot 0 through 8. */
    Result selectHotbarSlot(int slot);

    /** Swap a main inventory slot (0 through 35) with hotbar slot 0 through 8. */
    Result swapWithHotbar(int inventorySlot, int hotbarSlot);
}
