/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.pathing.movement.movements;

import baritone.api.IBaritone;
import baritone.api.pathing.movement.MovementStatus;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.BlockState;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import baritone.api.utils.VecUtils;
import baritone.api.utils.input.Input;
import baritone.pathing.movement.CalculationContext;
import baritone.pathing.movement.Movement;
import baritone.pathing.movement.MovementHelper;
import baritone.pathing.movement.MovementState;
import baritone.pathing.movement.MovementState.MovementTarget;
import baritone.utils.BlockStateInterface;
import baritone.utils.pathing.MutableMoveResult;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashSet;
import java.util.Set;

public class MovementFall extends Movement {

    public MovementFall(IBaritone baritone, BetterBlockPos src, BetterBlockPos dest) {
        super(baritone, src, dest, MovementFall.buildPositionsToBreak(src, dest));
    }

    @Override
    public double calculateCost(CalculationContext context) {
        MutableMoveResult result = new MutableMoveResult();
        MovementDescend.cost(context, src.x, src.y, src.z, dest.x, dest.z, result);
        if (result.y != dest.y) {
            return COST_INF; // doesn't apply to us, this position is a descend not a fall
        }
        return result.cost;
    }

    @Override
    protected Set<BetterBlockPos> calculateValidPositions() {
        Set<BetterBlockPos> set = new HashSet<>();
        set.add(src);
        for (int y = src.y - dest.y; y >= 0; y--) {
            set.add(dest.above(y));
        }
        return set;
    }

    private boolean willPlaceBucket() {
        CalculationContext context = new CalculationContext(baritone);
        MutableMoveResult result = new MutableMoveResult();
        return MovementDescend.dynamicFallCost(context, src.x, src.y, src.z, dest.x, dest.z, 0, context.get(dest.x, src.y - 2, dest.z), result);
    }

    private int findSlotMatchingItem(Item item) {
        if (ctx.player() == null || ctx.player().inventory == null) {
            return -1;
        }
        ItemStack[] inv = ctx.player().inventory.mainInventory;
        for (int i = 0; i < inv.length; i++) {
            if (inv[i] != null && inv[i].getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot < 9;
    }

    @Override
    public MovementState updateState(MovementState state) {
        super.updateState(state);
        if (state.getStatus() != MovementStatus.RUNNING) {
            return state;
        }

        BetterBlockPos playerFeet = ctx.playerFeet();
        Rotation toDest = RotationUtils.calcRotationFromVec3d(ctx.playerHead(), VecUtils.getBlockPosCenter(dest), ctx.playerRotations());
        Rotation targetRotation = null;
        BlockState destState = BlockStateInterface.get(ctx, dest);
        Block destBlock = destState.getBlock();

        boolean isWater = destBlock == Blocks.water || destBlock == Blocks.flowing_water;
        if (!isWater && willPlaceBucket() && !playerFeet.equals(dest)) {
            int waterBucketSlot = findSlotMatchingItem(Items.water_bucket);
            if (!isHotbarSlot(waterBucketSlot) || (ctx.world().provider != null && ctx.world().provider.isHellWorld)) {
                return state.setStatus(MovementStatus.UNREACHABLE);
            }

            if (ctx.player().posY - dest.y < ctx.playerController().getBlockReachDistance() && !ctx.player().onGround) {
                ctx.player().inventory.currentItem = waterBucketSlot;

                targetRotation = new Rotation(toDest.getYaw(), 90.0F);

                if (ctx.isLookingAt(dest) || ctx.isLookingAt(dest.below())) {
                    state.setInput(Input.CLICK_RIGHT, true);
                }
            }
        }
        if (targetRotation != null) {
            state.setTarget(new MovementTarget(targetRotation, true));
        } else {
            state.setTarget(new MovementTarget(toDest, false));
        }
        if (playerFeet.equals(dest) && (ctx.player().posY - playerFeet.y < 0.094 || isWater)) { // 0.094 because lilypads
            if (isWater) { // only match water, not flowing water (which we cannot pick up with a bucket)
                int bucketSlot = findSlotMatchingItem(Items.bucket);
                if (isHotbarSlot(bucketSlot)) {
                    ctx.player().inventory.currentItem = bucketSlot;
                    if (ctx.player().motionY >= 0) {
                        return state.setInput(Input.CLICK_RIGHT, true);
                    } else {
                        return state;
                    }
                } else {
                    if (ctx.player().motionY >= 0) {
                        return state.setStatus(MovementStatus.SUCCESS);
                    }
                }
            } else {
                return state.setStatus(MovementStatus.SUCCESS);
            }
        }
        Vec3 destCenter = VecUtils.getBlockPosCenter(dest);
        if (Math.abs(ctx.player().posX + ctx.player().motionX - destCenter.xCoord) > 0.1 || Math.abs(ctx.player().posZ + ctx.player().motionZ - destCenter.zCoord) > 0.1) {
            if (!ctx.player().onGround && Math.abs(ctx.player().motionY) > 0.4) {
                state.setInput(Input.SNEAK, true);
            }
            state.setInput(Input.MOVE_FORWARD, true);
        }
        ForgeDirection avoidDir = avoid();
        BetterBlockPos avoid;
        if (avoidDir == null) {
            avoid = src.subtract(dest);
        } else {
            avoid = new BetterBlockPos(avoidDir.offsetX, avoidDir.offsetY, avoidDir.offsetZ);
            double dist = Math.abs(avoid.x * (destCenter.xCoord - avoid.x / 2.0 - ctx.player().posX)) + Math.abs(avoid.z * (destCenter.zCoord - avoid.z / 2.0 - ctx.player().posZ));
            if (dist < 0.6) {
                state.setInput(Input.MOVE_FORWARD, true);
            } else if (!ctx.player().onGround) {
                state.setInput(Input.SNEAK, false);
            }
        }
        if (targetRotation == null) {
            Vec3 destCenterOffset = Vec3.createVectorHelper(destCenter.xCoord + 0.125 * avoid.x, destCenter.yCoord, destCenter.zCoord + 0.125 * avoid.z);
            state.setTarget(new MovementTarget(RotationUtils.calcRotationFromVec3d(ctx.playerHead(), destCenterOffset, ctx.playerRotations()), false));
        }
        return state;
    }

    private ForgeDirection avoid() {
        for (int i = 0; i < 15; i++) {
            BlockState state = BlockStateInterface.get(ctx, ctx.playerFeet().below(i));
            if (state.getBlock() == Blocks.ladder) {
                return ForgeDirection.getOrientation(state.getMetadata());
            }
        }
        return null;
    }

    @Override
    public boolean safeToCancel(MovementState state) {
        return ctx.playerFeet().equals(src) || state.getStatus() != MovementStatus.RUNNING;
    }

    private static BetterBlockPos[] buildPositionsToBreak(BetterBlockPos src, BetterBlockPos dest) {
        BetterBlockPos[] toBreak;
        int diffX = src.x - dest.x;
        int diffZ = src.z - dest.z;
        int diffY = Math.abs(src.y - dest.y);
        toBreak = new BetterBlockPos[diffY + 2];
        for (int i = 0; i < toBreak.length; i++) {
            toBreak[i] = new BetterBlockPos(src.x - diffX, src.y + 1 - i, src.z - diffZ);
        }
        return toBreak;
    }

    @Override
    protected boolean prepared(MovementState state) {
        if (state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        for (int i = 0; i < 4 && i < positionsToBreak.length; i++) {
            if (!MovementHelper.canWalkThrough(ctx, positionsToBreak[i])) {
                return super.prepared(state);
            }
        }
        return true;
    }
}
