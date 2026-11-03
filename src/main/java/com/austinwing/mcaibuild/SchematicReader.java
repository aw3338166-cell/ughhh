package com.austinwing.mcaibuild;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Read-only NBT parser for Sponge .schem v2/v3 and Litematica .litematic files. */
public final class SchematicReader {
    private static final int MAX_BLOCKS = 1_500_000;
    private SchematicReader() {}

    public static SchematicData read(Path path) throws IOException {
        Object parsed;
        try (InputStream raw = new BufferedInputStream(Files.newInputStream(path))) {
            raw.mark(2);
            int a = raw.read(), b = raw.read();
            raw.reset();
            InputStream decoded = (a == 0x1f && b == 0x8b) ? new GZIPInputStream(raw) : raw;
            try (DataInputStream in = new DataInputStream(decoded)) {
                int rootType = in.readUnsignedByte();
                if (rootType != 10) throw new IOException("File is not an NBT compound.");
                in.readUTF();
                parsed = readPayload(in, rootType, 0);
            }
        }
        if (!(parsed instanceof Map<?, ?> root)) throw new IOException("Invalid NBT root.");
        if (root.containsKey("Regions")) return readLitematic(path, root);
        Object nested = root.get("Schematic");
        if (nested instanceof Map<?, ?> map) root = map;
        return readSponge(path, root);
    }

    private static SchematicData readSponge(Path path, Map<?, ?> root) throws IOException {
        int width = number(root.get("Width"), -1);
        int height = number(root.get("Height"), -1);
        int length = number(root.get("Length"), -1);
        checkVolume(width, height, length);
        Map<?, ?> blockContainer = root.get("Blocks") instanceof Map<?, ?> nestedBlocks ? nestedBlocks : root;
        Object paletteObject = blockContainer.get("Palette");
        Object dataObject = blockContainer.get("BlockData");
        if (dataObject == null) dataObject = blockContainer.get("Data");
        if (!(paletteObject instanceof Map<?, ?> palette) || !(dataObject instanceof byte[] data))
            throw new IOException("This .schem file is missing Sponge Palette or BlockData data.");

        Map<Integer, String> ids = new HashMap<>();
        for (Map.Entry<?, ?> entry : palette.entrySet()) {
            if (entry.getKey() instanceof String name && entry.getValue() instanceof Number n)
                ids.put(n.intValue(), name);
        }
        int volume = width * height * length;
        List<SchematicData.BlockEntry> blocks = new ArrayList<>();
        int[] cursor = {0};
        for (int i = 0; i < volume; i++) {
            int paletteId = readVarInt(data, cursor);
            String id = ids.get(paletteId);
            if (id == null || isAir(id)) continue;
            int x = i % width;
            int z = (i / width) % length;
            int y = i / (width * length);
            ParsedBlockState parsedState = parseBlockState(id);
            blocks.add(new SchematicData.BlockEntry(x, y, z, parsedState.id(), parsedState.properties()));
        }
        return new SchematicData(path.getFileName().toString(), width, height, length, List.copyOf(blocks));
    }

    private static SchematicData readLitematic(Path path, Map<?, ?> root) throws IOException {
        Object regionsObject = root.get("Regions");
        if (!(regionsObject instanceof Map<?, ?> regions) || regions.isEmpty())
            throw new IOException("The Litematica file contains no regions.");

        record Raw(int x, int y, int z, String id, Map<String, String> properties) {}
        List<Raw> raw = new ArrayList<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (Object value : regions.values()) {
            if (!(value instanceof Map<?, ?> region)) continue;
            if (!(region.get("Position") instanceof Map<?, ?> position) ||
                !(region.get("Size") instanceof Map<?, ?> size) ||
                !(region.get("BlockStatePalette") instanceof List<?> palette) ||
                !(region.get("BlockStates") instanceof long[] states)) continue;

            int px = number(position.get("x"), 0), py = number(position.get("y"), 0), pz = number(position.get("z"), 0);
            int sx = number(size.get("x"), 0), sy = number(size.get("y"), 0), sz = number(size.get("z"), 0);
            int ax = Math.abs(sx), ay = Math.abs(sy), az = Math.abs(sz);
            checkVolume(ax, ay, az);
            if (palette.isEmpty()) continue;

            List<String> paletteIds = new ArrayList<>();
            List<Map<String, String>> paletteProperties = new ArrayList<>();
            for (Object p : palette) {
                if (p instanceof Map<?, ?> state && state.get("Name") instanceof String name) {
                    paletteIds.add(name);
                    Map<String, String> props = new HashMap<>();
                    if (state.get("Properties") instanceof Map<?, ?> rawProps) {
                        for (Map.Entry<?, ?> prop : rawProps.entrySet()) {
                            if (prop.getKey() instanceof String key && prop.getValue() != null) props.put(key, String.valueOf(prop.getValue()));
                        }
                    }
                    paletteProperties.add(Map.copyOf(props));
                } else { paletteIds.add("minecraft:air"); paletteProperties.add(Map.of()); }
            }
            int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(paletteIds.size() - 1));
            int volume = ax * ay * az;
            if (states.length == 0 && volume > 0) continue;

            for (int i = 0; i < volume; i++) {
                int paletteIndex = readPacked(states, i, bits);
                if (paletteIndex < 0 || paletteIndex >= paletteIds.size()) continue;
                String id = paletteIds.get(paletteIndex);
                if (isAir(id)) continue;
                int x = i % ax;
                int z = (i / ax) % az;
                int y = i / (ax * az);
                int wx = px + (sx < 0 ? -x : x);
                int wy = py + (sy < 0 ? -y : y);
                int wz = pz + (sz < 0 ? -z : z);
                raw.add(new Raw(wx, wy, wz, id, paletteProperties.get(paletteIndex)));
                minX = Math.min(minX, wx); minY = Math.min(minY, wy); minZ = Math.min(minZ, wz);
                maxX = Math.max(maxX, wx); maxY = Math.max(maxY, wy); maxZ = Math.max(maxZ, wz);
                if (raw.size() > MAX_BLOCKS) throw new IOException("Schematic is too large (limit: " + MAX_BLOCKS + " blocks).");
            }
        }
        if (raw.isEmpty()) throw new IOException("No placeable blocks were found in the Litematica file.");
        int width = maxX - minX + 1, height = maxY - minY + 1, length = maxZ - minZ + 1;
        List<SchematicData.BlockEntry> blocks = new ArrayList<>(raw.size());
        for (Raw b : raw) blocks.add(new SchematicData.BlockEntry(b.x-minX, b.y-minY, b.z-minZ, b.id, b.properties));
        blocks.sort(Comparator.comparingInt(SchematicData.BlockEntry::y)
                .thenComparingInt(SchematicData.BlockEntry::z).thenComparingInt(SchematicData.BlockEntry::x));
        return new SchematicData(path.getFileName().toString(), width, height, length, List.copyOf(blocks));
    }

    /** Litematica packs floor(64 / bits) values into each long; values never straddle longs. */
    private static int readPacked(long[] data, int index, int bits) {
        int valuesPerLong = 64 / bits;
        int word = index / valuesPerLong;
        int slot = index % valuesPerLong;
        if (word >= data.length) return -1;
        int offset = slot * bits;
        long mask = (1L << bits) - 1L;
        return (int) ((data[word] >>> offset) & mask);
    }

    private static int readVarInt(byte[] bytes, int[] cursor) throws IOException {
        int result = 0, shift = 0;
        while (cursor[0] < bytes.length && shift < 35) {
            int b = bytes[cursor[0]++] & 0xff;
            result |= (b & 0x7f) << shift;
            if ((b & 0x80) == 0) return result;
            shift += 7;
        }
        throw new IOException("Invalid or truncated Sponge BlockData varint stream.");
    }

    private record ParsedBlockState(String id, Map<String, String> properties) {}

    private static ParsedBlockState parseBlockState(String state) {
        int open = state.indexOf('[');
        if (open < 0 || !state.endsWith("]")) return new ParsedBlockState(state, Map.of());
        String id = state.substring(0, open);
        Map<String, String> properties = new HashMap<>();
        for (String part : state.substring(open + 1, state.length() - 1).split(",")) {
            int equals = part.indexOf('=');
            if (equals > 0) properties.put(part.substring(0, equals), part.substring(equals + 1));
        }
        return new ParsedBlockState(id, Map.copyOf(properties));
    }

    private static boolean isAir(String id) {
        return id.equals("minecraft:air") || id.equals("minecraft:cave_air") || id.equals("minecraft:void_air");
    }

    private static int number(Object value, int fallback) {
        return value instanceof Number n ? n.intValue() : fallback;
    }

    private static void checkVolume(int width, int height, int length) throws IOException {
        if (width <= 0 || height <= 0 || length <= 0) throw new IOException("Invalid schematic dimensions.");
        long volume = (long) width * height * length;
        if (volume > MAX_BLOCKS) throw new IOException("Schematic is too large (limit: " + MAX_BLOCKS + " blocks).");
    }

    private static Object readPayload(DataInputStream in, int type, int depth) throws IOException {
        if (depth > 64) throw new IOException("NBT nesting is too deep.");
        return switch (type) {
            case 1 -> in.readByte();
            case 2 -> in.readShort();
            case 3 -> in.readInt();
            case 4 -> in.readLong();
            case 5 -> in.readFloat();
            case 6 -> in.readDouble();
            case 7 -> { int n = checkedLength(in.readInt()); byte[] a = new byte[n]; in.readFully(a); yield a; }
            case 8 -> in.readUTF();
            case 9 -> {
                int childType = in.readUnsignedByte();
                int n = checkedLength(in.readInt());
                List<Object> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) list.add(childType == 0 ? null : readPayload(in, childType, depth + 1));
                yield list;
            }
            case 10 -> {
                Map<String, Object> map = new HashMap<>();
                while (true) {
                    int childType = in.readUnsignedByte();
                    if (childType == 0) break;
                    String name = in.readUTF();
                    map.put(name, readPayload(in, childType, depth + 1));
                }
                yield map;
            }
            case 11 -> { int n = checkedLength(in.readInt()); int[] a = new int[n]; for (int i=0;i<n;i++) a[i]=in.readInt(); yield a; }
            case 12 -> { int n = checkedLength(in.readInt()); long[] a = new long[n]; for (int i=0;i<n;i++) a[i]=in.readLong(); yield a; }
            default -> throw new IOException("Unsupported NBT tag type: " + type);
        };
    }

    private static int checkedLength(int length) throws IOException {
        if (length < 0 || length > MAX_BLOCKS * 8) throw new IOException("Invalid NBT array/list length.");
        return length;
    }
}