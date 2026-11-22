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
import baritone.api.event.events.TickEvent;
import baritone.api.utils.Helper;
import baritone.api.utils.BlockState;
import baritone.utils.ToolSet;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import java.util.ArrayList;
import java.util.OptionalInt;
import java.util.function.Predicate;

public final class InventoryBehavior extends Behavior implements Helper {

    int ticksSinceLastInventoryMove;
    int[] lastTickRequestedMove; // not everything asks every tick, so remember the request while coming to a halt
    private boolean lastTickRequestedTemporaryBlock;
    private int temporaryBlockInventorySlot = -1;
    private int temporaryBlockHotbarSlot = -1;
    private int temporaryBlockLastUsedTick;
    private ItemStack temporaryDisplacedStack;
    private Object temporaryBlockWorld;
    private int tickCounter;

    public InventoryBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void onTick(TickEvent event) {
        if (event.getType() == TickEvent.Type.OUT) return;
        tickCounter++;
        ticksSinceLastInventoryMove++;
        if (!Baritone.settings().allowInventory.value) {
            if (lastTickRequestedTemporaryBlock) {
                lastTickRequestedMove = null;
                lastTickRequestedTemporaryBlock = false;
            }
            restoreTemporaryBlock();
            return;
        }
        if (temporaryBlockHotbarSlot >= 0 && tickCounter - temporaryBlockLastUsedTick > 3) restoreTemporaryBlock();
        if (ctx.player().openContainer != ctx.player().inventoryContainer) {
            // we have a crafting table or a chest or something open
            return;
        }
        int pick = bestToolAgainst(Blocks.stone);
        if (pick >= 9) {
            requestSwapWithHotBar(pick, 0);
        }
        int shovel = bestToolAgainst(Blocks.dirt);
        if (shovel >= 9 && hasToolEfficiencyBetterThanHotbar(Blocks.dirt, shovel)) {
            attemptToPutOnHotbar(shovel, slot -> slot == 0 || slot == 8);
        }
        int axe = bestToolAgainst(Blocks.log);
        if (axe >= 9 && hasToolEfficiencyBetterThanHotbar(Blocks.log, axe)) {
            attemptToPutOnHotbar(axe, slot -> slot == 0 || slot == 8);
        }
        if (lastTickRequestedMove != null) {
            logDebug("Remembering to move " + lastTickRequestedMove[0] + " " + lastTickRequestedMove[1] + " from a previous tick");
            int[] pendingMove = lastTickRequestedMove;
            boolean temporaryBlock = lastTickRequestedTemporaryBlock;
            requestSwapWithHotBar(pendingMove[0], pendingMove[1], temporaryBlock);
        }
    }

    public boolean attemptToPutOnHotbar(int inMainInvy, Predicate<Integer> disallowedHotbar) {
        OptionalInt destination = getTempHotbarSlot(disallowedHotbar);
        if (!destination.isPresent()) return false;
        ItemStack moving = ctx.player().inventory.mainInventory[inMainInvy];
        boolean moved = moving != null && moving.getItem() instanceof ItemBlock
                ? moveBlockTemporarily(inMainInvy, destination.getAsInt())
                : requestSwapWithHotBar(inMainInvy, destination.getAsInt());
        return moved;
    }

    public OptionalInt getTempHotbarSlot(Predicate<Integer> disallowedHotbar) {
        // Keep Baritone's tool slots clear and never displace tools or non-block items.
        ArrayList<Integer> candidates = new ArrayList<>();
        for (int i = 1; i < 8; i++) {
            if (ctx.player().inventory.mainInventory[i] == null && !disallowedHotbar.test(i)) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            for (int i = 1; i < 8; i++) {
                ItemStack stack = ctx.player().inventory.mainInventory[i];
                if (!disallowedHotbar.test(i) && isSafelyDisplaceable(stack)) {
                    candidates.add(i);
                }
            }
        }
        if (candidates.isEmpty() && !disallowedHotbar.test(8)
                && isSafelyDisplaceable(ctx.player().inventory.mainInventory[8])) {
            candidates.add(8);
        }
        if (candidates.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(candidates.get(0));
    }

    private boolean isSafelyDisplaceable(ItemStack stack) {
        if (stack == null) return true;
        if (!(stack.getItem() instanceof ItemBlock)) return false;
        Block block = ((ItemBlock) stack.getItem()).field_150939_a;
        return Baritone.settings().acceptableThrowawayItems.value.contains(stack.getItem())
                || block == Blocks.stone || block == Blocks.cobblestone || block == Blocks.dirt
                || block == Blocks.netherrack;
    }

    private boolean moveBlockTemporarily(int inventorySlot, int hotbarSlot) {
        ItemStack source = ctx.player().inventory.mainInventory[inventorySlot];
        if (source == null || !(source.getItem() instanceof ItemBlock)) return false;
        if (temporaryBlockHotbarSlot >= 0) {
            if (temporaryBlockInventorySlot == inventorySlot && temporaryBlockHotbarSlot == hotbarSlot) {
                temporaryBlockLastUsedTick = tickCounter;
                return true;
            }
            restoreTemporaryBlock();
            if (temporaryBlockHotbarSlot >= 0) return false;
        }
        return requestSwapWithHotBar(inventorySlot, hotbarSlot, true);
    }

    public void markTemporaryBlockUsed(int hotbarSlot) {
        if (temporaryBlockHotbarSlot == hotbarSlot) temporaryBlockLastUsedTick = tickCounter;
    }

    private void restoreTemporaryBlock() {
        if (temporaryBlockHotbarSlot < 0) return;
        if (ctx.player() == null || ctx.world() == null || temporaryBlockWorld != ctx.world()) {
            clearTemporaryBlock();
            return;
        }
        if (ctx.player().openContainer != ctx.player().inventoryContainer) return;
        int inventorySlot = temporaryBlockInventorySlot;
        int hotbarSlot = temporaryBlockHotbarSlot;
        ItemStack source = ctx.player().inventory.mainInventory[inventorySlot];
        // Reverse only while the original displaced stack is still in its expected slot.
        if (sameStack(source, temporaryDisplacedStack) && requestSwapWithHotBar(inventorySlot, hotbarSlot)) {
            clearTemporaryBlock();
        }
    }

    private boolean sameStack(ItemStack left, ItemStack right) {
        if (left == null || right == null) return left == right;
        return left.getItem() == right.getItem() && left.getItemDamage() == right.getItemDamage()
                && left.stackSize == right.stackSize;
    }

    private void clearTemporaryBlock() {
        temporaryBlockInventorySlot = -1;
        temporaryBlockHotbarSlot = -1;
        temporaryDisplacedStack = null;
        temporaryBlockWorld = null;
    }

    private boolean requestSwapWithHotBar(int inInventory, int inHotbar) {
        return requestSwapWithHotBar(inInventory, inHotbar, false);
    }

    private boolean requestSwapWithHotBar(int inInventory, int inHotbar, boolean temporaryBlock) {
        if (ctx.player() == null || ctx.player().openContainer != ctx.player().inventoryContainer
                || ctx.player().inventory.getItemStack() != null) return false;
        lastTickRequestedMove = new int[]{inInventory, inHotbar};
        lastTickRequestedTemporaryBlock = temporaryBlock;
        if (ticksSinceLastInventoryMove < Baritone.settings().ticksBetweenInventoryMoves.value) {
            logDebug("Inventory move requested but delaying " + ticksSinceLastInventoryMove + " " + Baritone.settings().ticksBetweenInventoryMoves.value);
            return false;
        }
        if (Baritone.settings().inventoryMoveOnlyIfStationary.value && !baritone.getInventoryPauserProcess().stationaryForInventoryMove()) {
            logDebug("Inventory move requested but delaying until stationary");
            return false;
        }
        ctx.playerController().windowClick(ctx.player().inventoryContainer.windowId, inInventory < 9 ? inInventory + 36 : inInventory, inHotbar, 2, ctx.player());
        ticksSinceLastInventoryMove = 0;
        lastTickRequestedMove = null;
        lastTickRequestedTemporaryBlock = false;
        if (temporaryBlock) {
            temporaryBlockInventorySlot = inInventory;
            temporaryBlockHotbarSlot = inHotbar;
            ItemStack displaced = ctx.player().inventory.mainInventory[inInventory];
            temporaryDisplacedStack = displaced == null ? null : displaced.copy();
            temporaryBlockWorld = ctx.world();
            temporaryBlockLastUsedTick = tickCounter;
        }
        return true;
    }

    private int bestToolAgainst(Block against) {
        ItemStack[] invy = ctx.player().inventory.mainInventory;
        int bestInd = -1;
        double bestSpeed = -1;
        for (int i = 0; i < invy.length; i++) {
            ItemStack stack = invy[i];
            if (stack == null) {
                continue;
            }
            if (Baritone.settings().itemSaver.value && (stack.getItemDamage() + Baritone.settings().itemSaverThreshold.value) >= stack.getMaxDamage() && stack.getMaxDamage() > 1) {
                continue;
            }
            if (stack.getItem() instanceof ItemTool || !stack.getItem().getToolClasses(stack).isEmpty()) {
                double speed = ToolSet.calculateSpeedVsBlock(stack, new BlockState(against, 0), ctx.world(), 0, 0, 0);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestInd = i;
                }
            }
        }
        return bestInd;
    }

    private boolean hasToolEfficiencyBetterThanHotbar(Block against, int inInventory) {
        ItemStack invStack = ctx.player().inventory.mainInventory[inInventory];
        if (invStack == null) return false;
        double invSpeed = ToolSet.calculateSpeedVsBlock(invStack, new BlockState(against, 0), ctx.world(), 0, 0, 0);
        if (invSpeed <= 1.0) return false;
        double bestHotbarSpeed = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = ctx.player().inventory.mainInventory[i];
            if (s != null) {
                double speed = ToolSet.calculateSpeedVsBlock(s, new BlockState(against, 0), ctx.world(), 0, 0, 0);
                if (speed > bestHotbarSpeed) bestHotbarSpeed = speed;
            }
        }
        return invSpeed > bestHotbarSpeed;
    }

    public boolean hasGenericThrowaway() {
        for (Item item : Baritone.settings().acceptableThrowawayItems.value) {
            if (throwaway(false, stack -> item.equals(stack.getItem()))) {
                return true;
            }
        }
        return false;
    }

    public boolean selectThrowawayForLocation(boolean select, int x, int y, int z) {
        BlockState maybe = baritone.getBuilderProcess().placeAt(x, y, z, baritone.bsi.get0(x, y, z));
        if (maybe != null && throwaway(select, stack -> {
            if (!(stack.getItem() instanceof ItemBlock)) return false;
            ItemBlock item = (ItemBlock) stack.getItem();
            Block block = item.field_150939_a;
            int metadata = block.onBlockPlaced(ctx.world(), x, y, z, 1, 0.5F, 0.5F, 0.5F, item.getMetadata(stack.getItemDamage()));
            return metadata >= 0 && maybe.equals(new BlockState(block, metadata));
        })) {
            return true;
        }
        if (maybe != null && throwaway(select, stack -> stack.getItem() instanceof ItemBlock && ((ItemBlock) stack.getItem()).field_150939_a == maybe.getBlock())) {
            return true;
        }
        for (Item item : Baritone.settings().acceptableThrowawayItems.value) {
            if (throwaway(select, stack -> item.equals(stack.getItem()))) {
                return true;
            }
        }
        return false;
    }

    public boolean throwaway(boolean select, Predicate<? super ItemStack> desired) {
        return throwaway(select, desired, Baritone.settings().allowInventory.value);
    }

    public boolean throwaway(boolean select, Predicate<? super ItemStack> desired, boolean allowInventory) {
        EntityClientPlayerMP p = ctx.player();
        ItemStack[] inv = p.inventory.mainInventory;
        for (int i = 0; i < 9; i++) {
            ItemStack item = inv[i];
            // this usage of settings() is okay because it's only called once during pathing
            // (while creating the CalculationContext at the very beginning)
            // and then it's called during execution
            // since this function is never called during cost calculation, we don't need to migrate
            // acceptableThrowawayItems to the CalculationContext
            if (item != null && desired.test(item)) {
                markTemporaryBlockUsed(i);
                if (select) {
                    p.inventory.currentItem = i;
                }
                return true;
            }
        }
        if (allowInventory) {
            for (int i = 9; i < 36; i++) {
                if (inv[i] != null && desired.test(inv[i])) {
                    if (select) {
                        OptionalInt destination = getTempHotbarSlot(slot -> false);
                        if (!destination.isPresent() || !moveBlockTemporarily(i, destination.getAsInt())) return false;
                        p.inventory.currentItem = destination.getAsInt();
                    }
                    if (temporaryBlockHotbarSlot >= 0) temporaryBlockLastUsedTick = tickCounter;
                    return true;
                }
            }
        }

        return false;
    }
}
