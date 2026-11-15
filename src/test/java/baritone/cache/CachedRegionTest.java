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

package baritone.cache;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

public class CachedRegionTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void failedWritePreservesExistingRegion() throws Exception {
        Path region = temp.newFile("region.bcr").toPath();
        Files.write(region, "old".getBytes(StandardCharsets.UTF_8));
        try {
            CachedRegion.writeAtomically(region, path -> {
                Files.write(path, "partial".getBytes(StandardCharsets.UTF_8));
                throw new IOException("write failed");
            });
            fail("Expected write failure");
        } catch (IOException expected) {
            assertEquals("old", new String(Files.readAllBytes(region), StandardCharsets.UTF_8));
        }
        CachedRegion.writeAtomically(region, path -> Files.write(path, "new".getBytes(StandardCharsets.UTF_8)));
        assertEquals("new", new String(Files.readAllBytes(region), StandardCharsets.UTF_8));
    }

    @Test
    public void readsLegacyAndExtendedMetadata() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(146);
        out.writeInt(1024);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(146, CachedRegion.readMetadata(in, false));
        assertEquals(1024, CachedRegion.readMetadata(in, true));
    }

    @Test
    public void blockPosSaving() {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    byte part1 = (byte) (z << 4 | x);
                    byte part2 = (byte) (y);
                    byte xz = part1;
                    int X = xz & 0x0f;
                    int Z = (xz >>> 4) & 0x0f;
                    int Y = part2 & 0xff;
                    if (x != X || y != Y || z != Z) {
                        System.out.println(x + " " + X + " " + y + " " + Y + " " + z + " " + Z);
                    }
                    assertEquals(x, X);
                    assertEquals(y, Y);
                    assertEquals(z, Z);
                }
            }
        }
    }
}
