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

package baritone.process;

import baritone.Baritone;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalComposite;
import baritone.api.pathing.goals.GoalGetToBlock;
import baritone.api.process.IFarmProcess;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.selection.ISelection;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.RayTraceUtils;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.MovementHelper;
import baritone.utils.BaritoneProcessHelper;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class FarmProcess extends BaritoneProcessHelper implements IFarmProcess {

    private static final ForgeDirection[] HORIZONTALS = {
            ForgeDirection.NORTH,
            ForgeDirection.SOUTH,
            ForgeDirection.WEST,
            ForgeDirection.EAST
    };

    private boolean active;
    private List<BetterBlockPos> locations;
    private int tickCount;

    private int range;
    private BetterBlockPos center;

    private static final List<Item> FARMLAND_PLANTABLE = Arrays.asList(
            Items.melon_seeds,
            Items.wheat_seeds,
            Items.pumpkin_seeds,
            Items.potato,
            Items.carrot
    );

    public FarmProcess(Baritone baritone) {
        super(baritone);
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void farm(int range, BetterBlockPos pos) {
        if (pos == null) {
            center = baritone.getPlayerContext().playerFeet();
        } else {
            center = pos;
        }
        this.range = range;
        active = true;
        locations = null;
    }

    private enum Harvest {
        WHEAT(Blocks.wheat, meta -> meta >= 7),
        CARROTS(Blocks.carrots, meta -> meta >= 7),
        POTATOES(Blocks.potatoes, meta -> meta >= 7),
        PUMPKIN(Blocks.pumpkin, meta -> true),
        MELON(Blocks.melon_block, meta -> true),
        NETHERWART(Blocks.nether_wart, meta -> meta >= 3),
        COCOA(Blocks.cocoa, meta -> ((meta & 12) >> 2) >= 2),
        SUGARCANE(Blocks.reeds, null) {
            @Override
            public boolean readyToHarvest(IPlayerContext ctx, BetterBlockPos pos, int meta) {
                if (Baritone.settings().replantCrops.value) {
                    return ctx.world().getBlock(pos.x, pos.y - 1, pos.z) == Blocks.reeds;
                }
                return true;
            }
        },
        CACTUS(Blocks.cactus, null) {
            @Override
            public boolean readyToHarvest(IPlayerContext ctx, BetterBlockPos pos, int meta) {
                if (Baritone.settings().replantCrops.value) {
                    return ctx.world().getBlock(pos.x, pos.y - 1, pos.z) == Blocks.cactus;
                }
                return true;
            }
        };

        public final Block block;
        public final Predicate<Integer> readyToHarvestMeta;

        Harvest(Block block, Predicate<Integer> readyToHarvestMeta) {
            this.block = block;
            this.readyToHarvestMeta = readyToHarvestMeta;
        }

        public boolean readyToHarvest(IPlayerContext ctx, BetterBlockPos pos, int meta) {
            return readyToHarvestMeta != null && readyToHarvestMeta.test(meta);
        }
    }

    private boolean readyForHarvest(IPlayerContext ctx, BetterBlockPos pos) {
        Block block = ctx.world().getBlock(pos.x, pos.y, pos.z);
        int meta = ctx.world().getBlockMetadata(pos.x, pos.y, pos.z);
        for (Harvest harvest : Harvest.values()) {
            if (harvest.block == block) {
                return harvest.readyToHarvest(ctx, pos, meta);
            }
        }
        return false;
    }

    private boolean isPlantable(ItemStack stack) {
        return stack != null && FARMLAND_PLANTABLE.contains(stack.getItem());
    }

    private boolean isBoneMeal(ItemStack stack) {
        return stack != null && stack.getItem() == Items.dye && stack.getItemDamage() == 15;
    }

    private boolean isNetherWart(ItemStack stack) {
        return stack != null && stack.getItem() == Items.nether_wart;
    }

    private boolean isCocoa(ItemStack stack) {
        return stack != null && stack.getItem() == Items.dye && stack.getItemDamage() == 3;
    }

    private static boolean isPickupDropped(Item item, int damage) {
        if (item == Items.wheat_seeds || item == Items.wheat || item == Items.melon_seeds ||
                item == Items.melon || item == Item.getItemFromBlock(Blocks.melon_block) ||
                item == Items.pumpkin_seeds || item == Item.getItemFromBlock(Blocks.pumpkin) ||
                item == Items.potato || item == Items.carrot || item == Items.nether_wart ||
                item == Items.reeds || item == Item.getItemFromBlock(Blocks.cactus)) {
            return true;
        }
        return item == Items.dye && damage == 3;
    }

    private boolean isBonemealable(IPlayerContext ctx, BetterBlockPos pos) {
        Block block = ctx.world().getBlock(pos.x, pos.y, pos.z);
        if (block instanceof IGrowable) {
            IGrowable ig = (IGrowable) block;
            return ig.func_149851_a(ctx.world(), pos.x, pos.y, pos.z, ctx.world().isRemote)
                    && ig.func_149852_a(ctx.world(), ctx.world().rand, pos.x, pos.y, pos.z);
        }
        return false;
    }

    @Override
    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
        if (Baritone.settings().mineGoalUpdateInterval.value != 0 && tickCount++ % Baritone.settings().mineGoalUpdateInterval.value == 0) {
            ArrayList<Block> scan = new ArrayList<>();
            for (Harvest harvest : Harvest.values()) {
                scan.add(harvest.block);
            }
            if (Baritone.settings().replantCrops.value) {
                scan.add(Blocks.farmland);
                scan.add(Blocks.log);
                if (Baritone.settings().replantNetherWart.value) {
                    scan.add(Blocks.soul_sand);
                }
            }

            Baritone.getExecutor().execute(() -> locations = BaritoneAPI.getProvider().getWorldScanner().scanChunkRadius(ctx, scan, Baritone.settings().farmMaxScanSize.value, 10, 10));
        }
        if (locations == null) {
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }
        if (Baritone.settings().farmUsingSelection.value) {
            ISelection selection = baritone.getSelectionManager().getLastSelection();
            if (selection != null) {
                BetterBlockPos min = selection.min();
                BetterBlockPos max = selection.max();
                locations.removeIf(pos -> pos.x < min.x || pos.x > max.x || pos.y < min.y || pos.y > max.y || pos.z < min.z || pos.z > max.z);
            }
        }
        List<BetterBlockPos> toBreak = new ArrayList<>();
        List<BetterBlockPos> openFarmland = new ArrayList<>();
        List<BetterBlockPos> bonemealable = new ArrayList<>();
        List<BetterBlockPos> openSoulsand = new ArrayList<>();
        List<BetterBlockPos> openLog = new ArrayList<>();
        for (BetterBlockPos pos : locations) {
            // check if the target block is out of range.
            if (range != 0 && pos.distanceSq(center) > (double) range * range) {
                continue;
            }

            Block block = ctx.world().getBlock(pos.x, pos.y, pos.z);
            int meta = ctx.world().getBlockMetadata(pos.x, pos.y, pos.z);
            boolean airAbove = ctx.world().isAirBlock(pos.x, pos.y + 1, pos.z);
            if (block == Blocks.farmland) {
                if (airAbove) {
                    openFarmland.add(pos);
                }
                continue;
            }
            if (block == Blocks.soul_sand) {
                if (airAbove) {
                    openSoulsand.add(pos);
                }
                continue;
            }
            if (block == Blocks.log && (meta & 3) == 3) { // jungle log
                for (ForgeDirection dir : HORIZONTALS) {
                    BetterBlockPos adj = pos.relative(dir);
                    if (ctx.world().isAirBlock(adj.x, adj.y, adj.z)) {
                        openLog.add(pos);
                        break;
                    }
                }
                continue;
            }
            if (readyForHarvest(ctx, pos)) {
                toBreak.add(pos);
                continue;
            }
            if (isBonemealable(ctx, pos)) {
                bonemealable.add(pos);
            }
        }

        baritone.getInputOverrideHandler().clearAllKeys();
        BetterBlockPos playerPos = ctx.playerFeet();
        double blockReachDistance = ctx.playerController().getBlockReachDistance();
        for (BetterBlockPos pos : toBreak) {
            if (playerPos.distanceSq(pos) > blockReachDistance * blockReachDistance) {
                continue;
            }
            Optional<Rotation> rot = RotationUtils.reachable(ctx, pos);
            if (rot.isPresent() && isSafeToCancel) {
                baritone.getLookBehavior().updateTarget(rot.get(), true);
                MovementHelper.switchToBestToolFor(ctx, baritone.bsi.get0(pos));
                if (ctx.isLookingAt(pos)) {
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
                }
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
        }
        ArrayList<BetterBlockPos> both = new ArrayList<>(openFarmland);
        both.addAll(openSoulsand);
        for (BetterBlockPos pos : both) {
            if (playerPos.distanceSq(pos) > blockReachDistance * blockReachDistance) {
                continue;
            }
            boolean soulsand = openSoulsand.contains(pos);
            Optional<Rotation> rot = RotationUtils.reachableOffset(ctx, pos, Vec3.createVectorHelper(pos.x + 0.5, pos.y + 1.0, pos.z + 0.5), blockReachDistance, false);
            if (rot.isPresent() && isSafeToCancel && baritone.getInventoryBehavior().throwaway(true, soulsand ? this::isNetherWart : this::isPlantable)) {
                MovingObjectPosition result = RayTraceUtils.rayTraceTowards(ctx.player(), rot.get(), blockReachDistance);
                if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && result.sideHit == 1) { // 1 is UP
                    baritone.getLookBehavior().updateTarget(rot.get(), true);
                    if (ctx.isLookingAt(pos)) {
                        baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                    }
                    return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                }
            }
        }
        for (BetterBlockPos pos : openLog) {
            if (playerPos.distanceSq(pos) > blockReachDistance * blockReachDistance) {
                continue;
            }
            for (ForgeDirection dir : HORIZONTALS) {
                BetterBlockPos adj = pos.relative(dir);
                if (!ctx.world().isAirBlock(adj.x, adj.y, adj.z)) {
                    continue;
                }
                Vec3 faceCenter = Vec3.createVectorHelper(pos.x + 0.5 + dir.offsetX * 0.5, pos.y + 0.5, pos.z + 0.5 + dir.offsetZ * 0.5);
                Optional<Rotation> rot = RotationUtils.reachableOffset(ctx, pos, faceCenter, blockReachDistance, false);
                if (rot.isPresent() && isSafeToCancel && baritone.getInventoryBehavior().throwaway(true, this::isCocoa)) {
                    MovingObjectPosition result = RayTraceUtils.rayTraceTowards(ctx.player(), rot.get(), blockReachDistance);
                    if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && result.sideHit == dir.ordinal()) {
                        baritone.getLookBehavior().updateTarget(rot.get(), true);
                        if (ctx.isLookingAt(pos)) {
                            baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                        }
                        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                    }
                }
            }
        }
        for (BetterBlockPos pos : bonemealable) {
            if (playerPos.distanceSq(pos) > blockReachDistance * blockReachDistance) {
                continue;
            }
            Optional<Rotation> rot = RotationUtils.reachable(ctx, pos);
            if (rot.isPresent() && isSafeToCancel && baritone.getInventoryBehavior().throwaway(true, this::isBoneMeal)) {
                baritone.getLookBehavior().updateTarget(rot.get(), true);
                if (ctx.isLookingAt(pos)) {
                    baritone.getInputOverrideHandler().setInputForceState(Input.CLICK_RIGHT, true);
                }
                return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
            }
        }

        if (calcFailed) {
            logDirect("Farm failed");
            if (Baritone.settings().notificationOnFarmFail.value) {
                logNotification("Farm failed", true);
            }
            onLostControl();
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }

        List<Goal> goalz = new ArrayList<>();
        for (BetterBlockPos pos : toBreak) {
            goalz.add(new BuilderProcess.GoalBreak(pos));
        }
        if (baritone.getInventoryBehavior().throwaway(false, this::isPlantable)) {
            for (BetterBlockPos pos : openFarmland) {
                goalz.add(new GoalBlock(pos.above()));
            }
        }
        if (baritone.getInventoryBehavior().throwaway(false, this::isNetherWart)) {
            for (BetterBlockPos pos : openSoulsand) {
                goalz.add(new GoalBlock(pos.above()));
            }
        }
        if (baritone.getInventoryBehavior().throwaway(false, this::isCocoa)) {
            for (BetterBlockPos pos : openLog) {
                for (ForgeDirection dir : HORIZONTALS) {
                    BetterBlockPos adj = pos.relative(dir);
                    if (ctx.world().isAirBlock(adj.x, adj.y, adj.z)) {
                        goalz.add(new GoalGetToBlock(adj));
                    }
                }
            }
        }
        if (baritone.getInventoryBehavior().throwaway(false, this::isBoneMeal)) {
            for (BetterBlockPos pos : bonemealable) {
                goalz.add(new GoalBlock(pos));
            }
        }
        for (Entity entity : ctx.entities()) {
            if (entity instanceof EntityItem && entity.onGround) {
                EntityItem ei = (EntityItem) entity;
                ItemStack stack = ei.getEntityItem();
                if (stack != null && isPickupDropped(stack.getItem(), stack.getItemDamage())) {
                    // +0.1 because of farmland's 0.9375 dummy height
                    goalz.add(new GoalBlock(new BetterBlockPos(entity.posX, entity.posY + 0.1, entity.posZ)));
                }
            }
        }
        if (goalz.isEmpty()) {
            logDirect("Farm failed");
            if (Baritone.settings().notificationOnFarmFail.value) {
                logNotification("Farm failed", true);
            }
            onLostControl();
            return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        }
        return new PathingCommand(new GoalComposite(goalz.toArray(new Goal[0])), PathingCommandType.SET_GOAL_AND_PATH);
    }

    @Override
    public void onLostControl() {
        active = false;
    }

    @Override
    public String displayName0() {
        return "Farming";
    }
}
