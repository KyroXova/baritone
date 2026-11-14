/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package baritone.utils;

import baritone.Baritone;
import baritone.api.utils.BlockState;
import net.minecraft.block.Block;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.potion.Potion;
import net.minecraft.world.World;

/** Selects tools and estimates legacy block-breaking speed. */
public class ToolSet {
    private final EntityClientPlayerMP player;
    private final double potionMultiplier;

    public ToolSet(EntityClientPlayerMP player) {
        this.player = player;
        this.potionMultiplier = Baritone.settings().considerPotionEffects.value ? potionAmplifier(player) : 1;
    }

    public double getStrVsBlock(BlockState state, int x, int y, int z) {
        int slot = getBestSlot(state, false, true);
        double speed = calculateSpeedVsBlock(player.inventory.mainInventory[slot], state, player.worldObj, x, y, z);
        if (Baritone.settings().blocksToAvoidBreaking.value.contains(state.getBlock())) {
            speed *= Baritone.settings().avoidBreakingMultiplier.value;
        }
        return speed * potionMultiplier;
    }

    public boolean hasSilkTouch(ItemStack stack) {
        return stack != null && EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, stack) > 0;
    }

    public int getBestSlot(Block block, boolean preferSilkTouch) {
        return getBestSlot(new BlockState(block, 0), preferSilkTouch, false);
    }

    public int getBestSlot(BlockState state, boolean preferSilkTouch) {
        return getBestSlot(state, preferSilkTouch, false);
    }

    public int getBestSlot(BlockState state, boolean preferSilkTouch, boolean pathingCalculation) {
        if (!Baritone.settings().autoTool.value && pathingCalculation) return player.inventory.currentItem;
        int best = player.inventory.currentItem;
        double highest = Double.NEGATIVE_INFINITY;
        int lowestCost = Integer.MAX_VALUE;
        boolean bestSilk = false;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.inventory.mainInventory[slot];
            if (stack != null && !Baritone.settings().useSwordToMine.value && stack.getItem() instanceof ItemSword) continue;
            if (stack != null && Baritone.settings().itemSaver.value && stack.getMaxDamage() > 1
                    && stack.getItemDamage() + Baritone.settings().itemSaverThreshold.value >= stack.getMaxDamage()) continue;
            double speed = toolStrength(stack, state);
            boolean silk = hasSilkTouch(stack);
            int cost = materialCost(stack);
            if (speed > highest || (speed == highest && ((cost < lowestCost && (silk || !bestSilk))
                    || (preferSilkTouch && silk && !bestSilk)))) {
                best = slot;
                highest = speed;
                lowestCost = cost;
                bestSilk = silk;
            }
        }
        return best;
    }

    /** Returns mining progress per tick, or a negative value for unbreakable blocks. */
    public static double calculateSpeedVsBlock(ItemStack stack, BlockState state, World world, int x, int y, int z) {
        float hardness;
        try {
            hardness = state.getBlock().getBlockHardness(world, x, y, z);
        } catch (NullPointerException ex) {
            return -1;
        }
        return miningProgress(rawDigSpeed(stack, state), efficiencyLevel(stack),
                canHarvest(stack, state.getBlock(), state.getMetadata()), hardness);
    }

    private static double toolStrength(ItemStack stack, BlockState state) {
        return miningProgress(rawDigSpeed(stack, state), efficiencyLevel(stack),
                canHarvest(stack, state.getBlock(), state.getMetadata()), 1);
    }

    private static float rawDigSpeed(ItemStack stack, BlockState state) {
        return stack == null ? 1 : stack.getItem().getDigSpeed(stack, state.getBlock(), state.getMetadata());
    }

    private static int efficiencyLevel(ItemStack stack) {
        return stack == null ? 0 : EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);
    }

    static double miningProgress(float speed, int efficiency, boolean canHarvest, float hardness) {
        if (hardness < 0) return -1;
        if (speed > 1 && efficiency > 0) speed += efficiency * efficiency + 1;
        return speed / hardness / (canHarvest ? 30.0 : 100.0);
    }

    private static boolean canHarvest(ItemStack stack, Block block, int metadata) {
        if (block.getMaterial().isToolNotRequired()) return true;
        if (stack == null) return false;
        String tool = block.getHarvestTool(metadata);
        int level = tool == null ? -1 : stack.getItem().getHarvestLevel(stack, tool);
        return level >= 0 ? level >= block.getHarvestLevel(metadata) : stack.func_150998_b(block);
    }

    private static int materialCost(ItemStack stack) {
        if (stack == null) return -1;
        Item item = stack.getItem();
        String name = item instanceof ItemTool ? ((ItemTool) item).getToolMaterialName()
                : item instanceof ItemSword ? ((ItemSword) item).getToolMaterialName() : "";
        if ("WOOD".equals(name)) return 0;
        if ("STONE".equals(name)) return 1;
        if ("IRON".equals(name)) return 2;
        if ("GOLD".equals(name)) return 3;
        if ("EMERALD".equals(name)) return 4;
        return -1;
    }

    private static double potionAmplifier(EntityClientPlayerMP player) {
        double speed = 1;
        if (player.isPotionActive(Potion.digSpeed)) {
            speed *= 1 + (player.getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2;
        }
        if (player.isPotionActive(Potion.digSlowdown)) {
            speed *= Math.max(0, 1 - (player.getActivePotionEffect(Potion.digSlowdown).getAmplifier() + 1) * 0.2);
        }
        return speed;
    }
}
