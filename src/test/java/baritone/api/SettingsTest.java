/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */
package baritone.api;

import baritone.api.utils.SettingsUtil;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SettingsTest {

    @Test
    public void testSettingsDefaults() {
        Settings settings = new Settings();
        assertTrue(settings.allowPlace.value);
        assertTrue(settings.allowBreak.value);
        assertTrue(settings.allowSprint.value);
        assertFalse(settings.censorCoordinates.value);
    }

    @Test
    public void testMaybeCensor() {
        assertFalse(BaritoneAPI.getSettings().censorCoordinates.value);
        assertEquals("123", SettingsUtil.maybeCensor(123));

        BaritoneAPI.getSettings().censorCoordinates.value = true;
        assertEquals("<censored>", SettingsUtil.maybeCensor(123));

        // Restore
        BaritoneAPI.getSettings().censorCoordinates.value = false;
        assertEquals("123", SettingsUtil.maybeCensor(123));
    }

    @Test
    public void testModifiedSettings() {
        Settings settings = new Settings();
        List<Settings.Setting> modified = SettingsUtil.modifiedSettings(settings);
        assertTrue(modified.isEmpty());

        settings.allowSprint.value = false;
        modified = SettingsUtil.modifiedSettings(settings);
        assertEquals(1, modified.size());
        assertEquals("allowSprint", modified.get(0).getName());
    }
}
