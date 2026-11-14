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

package baritone.pathing.precompute;

import baritone.pathing.movement.LegacyMovementRules;
import baritone.utils.BlockStateInterface;
import baritone.api.utils.BlockState;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PrecomputedData {

    private final ConcurrentMap<BlockState, Byte> data = new ConcurrentHashMap<>();

    /**
     * byte layout
     *
     *          7              6             5              4              3              2              1             0
     *          |              |             |              |              |              |              |             |
     *      unused         canWalkOn       maybe       canWalkThrough    maybe        fullyPassable    maybe       completed
     */

    private static final byte COMPLETED_MASK = (byte) 1 << 0;
    private static final byte FULLY_PASSABLE_MAYBE_MASK = (byte) 1 << 1;
    private static final byte FULLY_PASSABLE_MASK = (byte) 1 << 2;
    private static final byte CAN_WALK_THROUGH_MAYBE_MASK = (byte) 1 << 3;
    private static final byte CAN_WALK_THROUGH_MASK = (byte) 1 << 4;
    private static final byte CAN_WALK_ON_MAYBE_MASK = (byte) 1 << 5;
    private static final byte CAN_WALK_ON_MASK = (byte) 1 << 6;

    private byte fillData(BlockState state) {
        byte blockData = 0;

        Ternary canWalkOnState = LegacyMovementRules.canWalkOnBlockState(state);
        switch (canWalkOnState) {
            case YES: blockData |= CAN_WALK_ON_MASK; break;
            case MAYBE: blockData |= CAN_WALK_ON_MAYBE_MASK; break;
            default: break;
        }

        Ternary canWalkThroughState = LegacyMovementRules.canWalkThroughBlockState(state);
        switch (canWalkThroughState) {
            case YES: blockData |= CAN_WALK_THROUGH_MASK; break;
            case MAYBE: blockData |= CAN_WALK_THROUGH_MAYBE_MASK; break;
            default: break;
        }

        Ternary fullyPassableState = LegacyMovementRules.fullyPassableBlockState(state);
        switch (fullyPassableState) {
            case YES: blockData |= FULLY_PASSABLE_MASK; break;
            case MAYBE: blockData |= FULLY_PASSABLE_MAYBE_MASK; break;
            default: break;
        }

        blockData |= COMPLETED_MASK;

        return blockData;
    }

    public boolean canWalkOn(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        int blockData = data.computeIfAbsent(state, this::fillData);

        if ((blockData & CAN_WALK_ON_MAYBE_MASK) != 0) {
            return LegacyMovementRules.canWalkOnPosition(bsi, x, y, z, state);
        } else {
            return (blockData & CAN_WALK_ON_MASK) != 0;
        }
    }

    public boolean canWalkThrough(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        int blockData = data.computeIfAbsent(state, this::fillData);

        if ((blockData & CAN_WALK_THROUGH_MAYBE_MASK) != 0) {
            return LegacyMovementRules.canWalkThroughPosition(bsi, x, y, z, state);
        } else {
            return (blockData & CAN_WALK_THROUGH_MASK) != 0;
        }
    }

    public boolean fullyPassable(BlockStateInterface bsi, int x, int y, int z, BlockState state) {
        int blockData = data.computeIfAbsent(state, this::fillData);

        if ((blockData & FULLY_PASSABLE_MAYBE_MASK) != 0) {
            return LegacyMovementRules.fullyPassablePosition(bsi, x, y, z, state);
        } else {
            return (blockData & FULLY_PASSABLE_MASK) != 0;
        }
    }
}
