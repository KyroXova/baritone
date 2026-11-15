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

package baritone.pathing.movement;

import baritone.api.utils.input.Input;
import java.util.Objects;
import java.util.stream.Stream;

public final class MovementOption {
    private static final float SPRINT_MULTIPLIER = 1.3f;
    private final Input input1;
    private final Input input2;
    private final float motionX;
    private final float motionZ;

    public MovementOption(Input input1, Input input2, float motionX, float motionZ) {
        this.input1 = input1;
        this.input2 = input2;
        this.motionX = motionX;
        this.motionZ = motionZ;
    }

    public Input input1() { return input1; }
    public Input input2() { return input2; }
    public float motionX() { return motionX; }
    public float motionZ() { return motionZ; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof MovementOption)) return false;
        MovementOption that = (MovementOption) other;
        return input1 == that.input1 && input2 == that.input2
                && Float.compare(motionX, that.motionX) == 0
                && Float.compare(motionZ, that.motionZ) == 0;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(input1);
        result = 31 * result + Objects.hashCode(input2);
        result = 31 * result + Float.hashCode(motionX);
        return 31 * result + Float.hashCode(motionZ);
    }

    @Override
    public String toString() {
        return "MovementOption[input1=" + input1 + ", input2=" + input2
                + ", motionX=" + motionX + ", motionZ=" + motionZ + "]";
    }

    public MovementOption(Input input1, float motionX, float motionZ) {
        this(input1, null, motionX, motionZ);
    }

    public void setInputs(MovementState movementState) {
        if (input1 != null) {
            movementState.setInput(input1, true);
        }
        if (input2 != null) {
            movementState.setInput(input2, true);
        }
    }

    public float distanceToSq(float otherX, float otherZ) {
        return Math.abs(motionX() - otherX) + Math.abs(motionZ() - otherZ);
    }
    
    public static Stream<MovementOption> getOptions(float motionX, float motionZ, boolean canSprint) {
        return Stream.of(
                new MovementOption(Input.MOVE_FORWARD, canSprint ? Input.SPRINT : null, canSprint ? motionX * SPRINT_MULTIPLIER : motionX, canSprint ? motionZ * SPRINT_MULTIPLIER : motionZ),
                new MovementOption(Input.MOVE_BACK, -motionX, -motionZ),
                new MovementOption(Input.MOVE_LEFT, -motionZ, motionX),
                new MovementOption(Input.MOVE_RIGHT, motionZ, -motionX),
                new MovementOption(Input.MOVE_FORWARD, Input.MOVE_LEFT, (canSprint ? motionX * SPRINT_MULTIPLIER : motionX) - motionZ, (canSprint ? motionZ * SPRINT_MULTIPLIER : motionZ) + motionX),
                new MovementOption(Input.MOVE_FORWARD, Input.MOVE_RIGHT, (canSprint ? motionX * SPRINT_MULTIPLIER : motionX) + motionZ, (canSprint ? motionZ * SPRINT_MULTIPLIER : motionZ) - motionX),
                new MovementOption(Input.MOVE_BACK, Input.MOVE_LEFT, -motionX - motionZ, -motionZ + motionX),
                new MovementOption(Input.MOVE_BACK, Input.MOVE_RIGHT, -motionX + motionZ, -motionZ - motionX)
        );
    }
}
