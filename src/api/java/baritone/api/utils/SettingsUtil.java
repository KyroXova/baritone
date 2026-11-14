/*
 * This file is part of Baritone, distributed under the LGPL-3.0-or-later.
 */

package baritone.api.utils;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import baritone.api.schematic.Mirror;
import baritone.api.schematic.Rotation;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SettingsUtil {

    public static final String SETTINGS_DEFAULT_NAME = "settings.txt";
    private static final Pattern SETTING_PATTERN = Pattern.compile("^(?<setting>[^ ]+) +(?<value>.+)"); // key and value split by the first space

    private static boolean isComment(String line) {
        return line.startsWith("#") || line.startsWith("//");
    }

    private static void forEachLine(Path file, Consumer<String> consumer) throws IOException {
        try (BufferedReader scan = Files.newBufferedReader(file)) {
            String line;
            while ((line = scan.readLine()) != null) {
                if (line.isEmpty() || isComment(line)) {
                    continue;
                }
                consumer.accept(line);
            }
        }
    }

    public static void readAndApply(Settings settings, String settingsName) {
        try {
            forEachLine(settingsByName(settingsName), line -> {
                Matcher matcher = SETTING_PATTERN.matcher(line);
                if (!matcher.matches()) {
                    Helper.HELPER.logDirect("Invalid syntax in setting file: " + line);
                    return;
                }

                String settingName = matcher.group("setting").toLowerCase();
                String settingValue = matcher.group("value");
                if ("allowjumpat256".equals(settingName)) {
                    settingName = "allowjumpatbuildlimit";
                }
                try {
                    parseAndApply(settings, settingName, settingValue);
                } catch (Exception ex) {
                    Helper.HELPER.logDirect("Unable to parse line " + line);
                    ex.printStackTrace();
                }
            });
        } catch (NoSuchFileException ignored) {
            Helper.HELPER.logDirect("Baritone settings file not found, resetting.");
        } catch (Exception ex) {
            Helper.HELPER.logDirect("Exception while reading Baritone settings, some settings may be reset to default values!");
            ex.printStackTrace();
        }
    }

    public static synchronized void save(Settings settings) {
        try (BufferedWriter out = Files.newBufferedWriter(settingsByName(SETTINGS_DEFAULT_NAME))) {
            for (Settings.Setting setting : modifiedSettings(settings)) {
                out.write(settingToString(setting) + "\n");
            }
        } catch (Exception ex) {
            Helper.HELPER.logDirect("Exception thrown while saving Baritone settings!");
            ex.printStackTrace();
        }
    }

    private static Path settingsByName(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        Path base = (mc != null && mc.mcDataDir != null) ? mc.mcDataDir.toPath() : java.nio.file.Paths.get(".");
        return base.resolve("baritone").resolve(name);
    }

    public static List<Settings.Setting> modifiedSettings(Settings settings) {
        List<Settings.Setting> modified = new ArrayList<>();
        for (Settings.Setting setting : settings.allSettings) {
            if (setting.value == null) {
                System.out.println("NULL SETTING?" + setting.getName());
                continue;
            }
            if (setting.isJavaOnly()) {
                continue;
            }
            if (setting.value == setting.defaultValue) {
                continue;
            }
            modified.add(setting);
        }
        return modified;
    }

    /**
     * Gets the type of a setting and returns it as a string, with package names stripped.
     *
     * @param setting The setting
     * @return The type
     */
    public static String settingTypeToString(Settings.Setting setting) {
        return setting.getType().getTypeName()
                .replaceAll("(?:\\w+\\.)+(\\w+)", "$1");
    }

    public static <T> String settingValueToString(Settings.Setting<T> setting, T value) throws IllegalArgumentException {
        Parser io = Parser.getParser(setting.getType());

        if (io == null) {
            throw new IllegalStateException("Missing " + setting.getValueClass() + " " + setting.getName());
        }

        return io.toString(setting.getType(), value);
    }

    public static String settingValueToString(Settings.Setting setting) throws IllegalArgumentException {
        return settingValueToString(setting, setting.value);
    }

    public static String settingDefaultToString(Settings.Setting setting) throws IllegalArgumentException {
        return settingValueToString(setting, setting.defaultValue);
    }

    public static String maybeCensor(int coord) {
        if (BaritoneAPI.getSettings().censorCoordinates.value) {
            return "<censored>";
        }

        return Integer.toString(coord);
    }

    public static String settingToString(Settings.Setting setting) throws IllegalStateException {
        if (setting.isJavaOnly()) {
            return setting.getName();
        }

        return setting.getName() + " " + settingValueToString(setting);
    }

    @Deprecated
    public static boolean javaOnlySetting(Settings.Setting setting) {
        return setting.isJavaOnly();
    }

    public static void parseAndApply(Settings settings, String settingName, String settingValue) throws IllegalStateException, NumberFormatException {
        Settings.Setting setting = settings.byLowerName.get(settingName);
        if (setting == null) {
            throw new IllegalStateException("No setting by that name");
        }
        Class intendedType = setting.getValueClass();
        ISettingParser ioMethod = Parser.getParser(setting.getType());
        Object parsed = ioMethod.parse(setting.getType(), settingValue);
        if (!intendedType.isInstance(parsed)) {
            throw new IllegalStateException(ioMethod + " parser returned incorrect type, expected " + intendedType + " got " + parsed + " which is " + parsed.getClass());
        }
        setting.value = parsed;
    }

    private interface ISettingParser<T> {

        T parse(Type type, String raw);

        String toString(Type type, T value);

        boolean accepts(Type type);
    }

    private enum Parser implements ISettingParser {

        DOUBLE(Double.class, Double::parseDouble),
        BOOLEAN(Boolean.class, Boolean::parseBoolean),
        INTEGER(Integer.class, Integer::parseInt),
        FLOAT(Float.class, Float::parseFloat),
        LONG(Long.class, Long::parseLong),
        STRING(String.class, String::new),
        MIRROR(Mirror.class, Mirror::valueOf, Mirror::name),
        ROTATION(Rotation.class, Rotation::valueOf, Rotation::name),
        COLOR(
                Color.class,
                str -> new Color(Integer.parseInt(str.split(",")[0]), Integer.parseInt(str.split(",")[1]), Integer.parseInt(str.split(",")[2])),
                color -> color.getRed() + "," + color.getGreen() + "," + color.getBlue()
        ),
        BETTER_BLOCK_POS(
                BetterBlockPos.class,
                str -> new BetterBlockPos(Integer.parseInt(str.split(",")[0]), Integer.parseInt(str.split(",")[1]), Integer.parseInt(str.split(",")[2])),
                pos -> ((BetterBlockPos) pos).x + "," + ((BetterBlockPos) pos).y + "," + ((BetterBlockPos) pos).z
        ),
        BLOCK(
                Block.class,
                str -> (Block) Block.blockRegistry.getObject(str.trim()),
                block -> Block.blockRegistry.getNameForObject(block)
        ),
        ITEM(
                Item.class,
                str -> (Item) Item.itemRegistry.getObject(str.trim()),
                item -> Item.itemRegistry.getNameForObject(item)
        ),
        LIST() {
            @Override
            public Object parse(Type type, String raw) {
                Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
                Parser parser = Parser.getParser(elementType);
                return Stream.of(raw.split(","))
                        .map(s -> parser.parse(elementType, s))
                        .collect(Collectors.toList());
            }

            @Override
            public String toString(Type type, Object value) {
                Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
                Parser parser = Parser.getParser(elementType);

                return ((List<?>) value).stream()
                        .map(o -> parser.toString(elementType, o))
                        .collect(Collectors.joining(","));
            }

            @Override
            public boolean accepts(Type type) {
                return List.class.isAssignableFrom(TypeUtils.resolveBaseClass(type));
            }
        },
        MAPPING() {
            @Override
            public Object parse(Type type, String raw) {
                Type keyType = ((ParameterizedType) type).getActualTypeArguments()[0];
                Type valueType = ((ParameterizedType) type).getActualTypeArguments()[1];
                Parser keyParser = Parser.getParser(keyType);
                Parser valueParser = Parser.getParser(valueType);

                return Stream.of(raw.split(",(?=[^,]*->)"))
                        .map(s -> s.split("->"))
                        .collect(Collectors.toMap(s -> keyParser.parse(keyType, s[0]), s -> valueParser.parse(valueType, s[1])));
            }

            @Override
            public String toString(Type type, Object value) {
                Type keyType = ((ParameterizedType) type).getActualTypeArguments()[0];
                Type valueType = ((ParameterizedType) type).getActualTypeArguments()[1];
                Parser keyParser = Parser.getParser(keyType);
                Parser valueParser = Parser.getParser(valueType);

                return ((Map<?, ?>) value).entrySet().stream()
                        .map(o -> keyParser.toString(keyType, o.getKey()) + "->" + valueParser.toString(valueType, o.getValue()))
                        .collect(Collectors.joining(","));
            }

            @Override
            public boolean accepts(Type type) {
                return Map.class.isAssignableFrom(TypeUtils.resolveBaseClass(type));
            }
        };

        private final Class<?> cla$$;
        private final Function<String, Object> parser;
        private final Function<Object, String> toString;

        Parser() {
            this.cla$$ = null;
            this.parser = null;
            this.toString = null;
        }

        <T> Parser(Class<T> cla$$, Function<String, T> parser) {
            this(cla$$, parser, Object::toString);
        }

        <T> Parser(Class<T> cla$$, Function<String, T> parser, Function<T, String> toString) {
            this.cla$$ = cla$$;
            this.parser = parser::apply;
            this.toString = x -> toString.apply((T) x);
        }

        @Override
        public Object parse(Type type, String raw) {
            Object parsed = this.parser.apply(raw);
            Objects.requireNonNull(parsed);
            return parsed;
        }

        @Override
        public String toString(Type type, Object value) {
            return this.toString.apply(value);
        }

        @Override
        public boolean accepts(Type type) {
            return type instanceof Class && this.cla$$.isAssignableFrom((Class) type);
        }

        public static Parser getParser(Type type) {
            return Stream.of(values())
                    .filter(parser -> parser.accepts(type))
                    .findFirst().orElse(null);
        }
    }
}
