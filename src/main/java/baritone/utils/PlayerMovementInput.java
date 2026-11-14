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

package baritone.utils;

import baritone.api.utils.input.Input;
import net.minecraft.util.MovementInput;
import java.util.function.Predicate;

public class PlayerMovementInput extends MovementInput {

    private final Predicate<Input> forced;

    PlayerMovementInput(InputOverrideHandler handler) {
        this(handler::isInputForcedDown);
    }

    PlayerMovementInput(Predicate<Input> forced) {
        this.forced = forced;
    }

    @Override
    public void updatePlayerMoveState() {
        float leftImpulse = 0.0F;
        float forwardImpulse = 0.0F;
        boolean jumping = forced.test(Input.JUMP);

        boolean up = forced.test(Input.MOVE_FORWARD);
        if (up) {
            forwardImpulse++;
        }

        boolean down = forced.test(Input.MOVE_BACK);
        if (down) {
            forwardImpulse--;
        }

        boolean left = forced.test(Input.MOVE_LEFT);
        if (left) {
            leftImpulse++;
        }

        boolean right = forced.test(Input.MOVE_RIGHT);
        if (right) {
            leftImpulse--;
        }

        boolean sneaking = forced.test(Input.SNEAK);
        if (sneaking) {
            leftImpulse *= 0.3D;
            forwardImpulse *= 0.3D;
        }
        this.moveStrafe = leftImpulse;
        this.moveForward = forwardImpulse;
        this.jump = jumping;
        this.sneak = sneaking;
    }
}
