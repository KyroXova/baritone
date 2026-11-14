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

package baritone.utils.schematic.format.defaults;

import baritone.api.utils.BlockState;
import baritone.utils.schematic.StaticSchematic;
import baritone.utils.type.VarInt;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Brady
 * @since 12/27/2019
 */
public final class SpongeSchematic extends StaticSchematic {

    public SpongeSchematic(NBTTagCompound nbt) {
        this.x = nbt.getShort("Width");
        this.y = nbt.getShort("Height");
        this.z = nbt.getShort("Length");
        this.states = new BlockState[this.x][this.z][this.y];

        Int2ObjectArrayMap<BlockState> palette = new Int2ObjectArrayMap<>();
        NBTTagCompound paletteTag = nbt.getCompoundTag("Palette");
        if (paletteTag != null) {
            for (Object keyObj : paletteTag.func_150296_c()) {
                String tag = (String) keyObj;
                int index = paletteTag.getInteger(tag);

                SerializedBlockState serializedState = SerializedBlockState.getFromString(tag);
                if (serializedState == null) {
                    throw new IllegalArgumentException("Unable to parse palette tag");
                }

                BlockState state = serializedState.deserialize();
                if (state == null) {
                    throw new IllegalArgumentException("Unable to deserialize palette tag");
                }

                palette.put(index, state);
            }
        }

        // BlockData is stored as an NBT byte[], however, the actual data that is represented is a varint[]
        byte[] rawBlockData = nbt.getByteArray("BlockData");
        int[] blockData = new int[this.x * this.y * this.z];
        int offset = 0;
        for (int i = 0; i < blockData.length; i++) {
            if (offset >= rawBlockData.length) {
                throw new IllegalArgumentException("No remaining bytes in BlockData for complete schematic");
            }

            VarInt varInt = VarInt.read(rawBlockData, offset);
            blockData[i] = varInt.getValue();
            offset += varInt.getSize();
        }

        for (int y = 0; y < this.y; y++) {
            for (int z = 0; z < this.z; z++) {
                for (int x = 0; x < this.x; x++) {
                    int index = (y * this.z + z) * this.x + x;
                    BlockState state = palette.get(blockData[index]);
                    if (state == null) {
                        throw new IllegalArgumentException("Invalid Palette Index " + index);
                    }

                    this.states[x][z][y] = state;
                }
            }
        }
    }

    private static final class SerializedBlockState {

        private static final Pattern REGEX = Pattern.compile("(?<location>(\\w+:)?\\w+)(\\[(?<properties>(\\w+=\\w+,?)+)])?");

        private final String location;
        private final Map<String, String> properties;
        private BlockState blockState;

        private SerializedBlockState(String location, Map<String, String> properties) {
            this.location = location;
            this.properties = properties;
        }

        private BlockState deserialize() {
            if (this.blockState == null) {
                Block block = Block.getBlockFromName(this.location);
                if (block == null) {
                    block = (Block) Block.blockRegistry.getObject(this.location);
                }
                if (block == null) {
                    block = Blocks.air;
                }
                this.blockState = new BlockState(block, 0);
            }
            return this.blockState;
        }

        private static SerializedBlockState getFromString(String s) {
            Matcher m = REGEX.matcher(s);
            if (!m.matches()) {
                return null;
            }

            try {
                String location = m.group("location");
                String properties = m.group("properties");

                Map<String, String> propertiesMap = new HashMap<>();
                if (properties != null) {
                    for (String property : properties.split(",")) {
                        String[] split = property.split("=");
                        propertiesMap.put(split[0], split[1]);
                    }
                }

                return new SerializedBlockState(location, propertiesMap);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}
