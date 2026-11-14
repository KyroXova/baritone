/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.utils;

import baritone.api.utils.input.Input;
import java.util.EnumSet;
import org.junit.Test;

import static org.junit.Assert.*;

public class PlayerMovementInputTest {
    @Test
    public void translatesAndResetsForcedMovement() {
        EnumSet<Input> held = EnumSet.of(Input.MOVE_FORWARD, Input.MOVE_LEFT, Input.JUMP, Input.SNEAK);
        PlayerMovementInput input = new PlayerMovementInput(held::contains);
        input.updatePlayerMoveState();
        assertEquals(0.3F, input.moveForward, 0.0001F);
        assertEquals(0.3F, input.moveStrafe, 0.0001F);
        assertTrue(input.jump);
        assertTrue(input.sneak);

        held.clear();
        held.add(Input.MOVE_FORWARD);
        held.add(Input.MOVE_BACK);
        input.updatePlayerMoveState();
        assertEquals(0.0F, input.moveForward, 0.0F);
        assertEquals(0.0F, input.moveStrafe, 0.0F);
        assertFalse(input.jump);
        assertFalse(input.sneak);
    }
}
