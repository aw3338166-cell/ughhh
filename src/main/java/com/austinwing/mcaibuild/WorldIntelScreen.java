package com.austinwing.mcaibuild;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class WorldIntelScreen extends Screen {
    private enum Tab { BASES, CHUNKS }
    private static Tab tab = Tab.BASES;
    private static int radius = 2;
    private static int scannedChunks;
    private static int loadedChunks;
    private static int grownPlantClues;
    private static List<Candidate> candidates = new ArrayList<>();
    private static String scanMessage = "Scan nearby loaded chunks to look for possible player-built areas.";

    private record Candidate(int chunkX, int chunkZ, int y, int score,
                             int containers, int workstations, int redstone,
                             int doors, boolean underground) {}

    public WorldIntelScreen() {
        super(Text.literal("World Intel"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(560, width - 20);
        int panelHeight = Math.min(420, height - 20);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int buttonY = top + panelHeight - 30;
        int gap = 6;
        int buttonWidth = (panelWidth - 36 - gap * 2) / 3;
        int x = left + 12;

        addDrawableChild(ButtonWidget.builder(Text.literal("Base Finder"), b -> {
            tab = Tab.BASES;
            clearAndInit();
        }).dimensions(x, top + 68, 112, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Chunk Viewer"), b -> {
            tab = Tab.CHUNKS;
            clearAndInit();
        }).dimensions(x + 118, top + 68, 112, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Scan nearby"), b -> {
            scanNearby();
            clearAndInit();
        }).dimensions(x, buttonY, buttonWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Radius: " + radius + " chunks"), b -> {
            radius = radius == 3 ? 1 : radius + 1;
            clearAndInit();
        }).dimensions(x + buttonWidth + gap, buttonY, buttonWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(x + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20).build());
    }

    private void clearAndInit() {
        clearChildren();
        init();
    }

    private static void scanNearby() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            scanMessage = "Join a world before scanning.";
            return;
        }

        int centerX = client.player.getBlockX() >> 4;
        int centerZ = client.player.getBlockZ() >> 4;
        int bottomY = client.world.getBottomY();
        int topY = client.world.getTopY();
        int seaLevel = client.world.getSeaLevel();
        int checked = 0;
        int plantClues = 0;
        List<Candidate> found = new ArrayList<>();

        for (int cx = centerX - radius; cx <= centerX + radius; cx++) {
            for (int cz = centerZ - radius; cz <= centerZ + radius; cz++) {
                if (!client.world.getChunkManager().isChunkLoaded(cx, cz)) continue;
                Chunk chunk = client.world.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue;
                checked++;

                int containers = 0;
                int workstations = 0;
                int redstone = 0;
                int doors = 0;
                int lowestFeatureY = topY;
                boolean underground = false;
                BlockPos.Mutable pos = new BlockPos.Mutable();

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = bottomY; y < topY; y++) {
                            pos.set((cx << 4) + x, y, (cz << 4) + z);
                            BlockState state = chunk.getBlockState(pos);
                            if (state.isAir()) continue;
                            String id = Registries.BLOCK.getId(state.getBlock()).getPath();

                            if (isContainer(id)) {
                                containers++;
                                lowestFeatureY = Math.min(lowestFeatureY, y);
                                if (y < seaLevel - 8) underground = true;
                            }
                            if (isWorkstation(id)) {
                                workstations++;
                                lowestFeatureY = Math.min(lowestFeatureY, y);
                                if (y < seaLevel - 8) underground = true;
                            }
                            if (isRedstone(id)) {
                                redstone++;
                                lowestFeatureY = Math.min(lowestFeatureY, y);
                                if (y < seaLevel - 8) underground = true;
                            }
                            if (id.endsWith("_door") || id.endsWith("_trapdoor")) {
                                doors++;
                                lowestFeatureY = Math.min(lowestFeatureY, y);
                                if (y < seaLevel - 8) underground = true;
                            }
                            if (isCrop(id) && hasPositiveAge(state)) plantClues++;
                        }
                    }
                }

                int utilityCount = containers + workstations + redstone;
                int score = containers * 3 + workstations * 2 + redstone * 2 + doors;
                if (utilityCount >= 4) score += 2;
                if (underground) score += 3;

                if (score >= 6 && utilityCount >= 2) {
                    int featureY = lowestFeatureY == topY ? seaLevel : lowestFeatureY;
                    found.add(new Candidate(cx, cz, featureY, score, containers,
                            workstations, redstone, doors, underground));
                }
            }
        }

        found.sort(Comparator.comparingInt(Candidate::score).reversed());
        candidates = found;
        scannedChunks = checked;
        loadedChunks = client.world.getChunkManager().getLoadedChunkCount();
        grownPlantClues = plantClues;
        scanMessage = "Scanned " + checked + " loaded chunks. Results only include data sent to your client.";
    }

    private static boolean isContainer(String id) {
        return id.equals("chest") || id.equals("trapped_chest") || id.equals("barrel")
                || id.endsWith("_shulker_box") || id.equals("hopper");
    }

    private static boolean isWorkstation(String id) {
        return id.equals("crafting_table") || id.equals("furnace") || id.equals("smoker")
                || id.equals("blast_furnace") || id.equals("enchanting_table")
                || id.equals("brewing_stand") || id.equals("smithing_table")
                || id.equals("loom") || id.equals("cartography_table")
                || id.equals("stonecutter") || id.equals("grindstone")
                || id.equals("lectern") || id.endsWith("_anvil");
    }

    private static boolean isRedstone(String id) {
        return id.contains("redstone") || id.contains("piston") || id.contains("observer")
                || id.contains("repeater") || id.contains("comparator")
                || id.equals("lever") || id.endsWith("_button")
                || id.equals("tripwire_hook") || id.equals("target")
                || id.equals("daylight_detector") || id.equals("dispenser")
                || id.equals("dropper");
    }

    private static boolean isCrop(String id) {
        return id.equals("wheat") || id.equals("carrots") || id.equals("potatoes")
                || id.equals("beetroots") || id.equals("nether_wart")
                || id.equals("cocoa") || id.equals("sweet_berry_bush")
                || id.equals("melon_stem") || id.equals("pumpkin_stem");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean hasPositiveAge(BlockState state) {
        for (Property property : state.getProperties()) {
            if (!property.getName().equals("age")) continue;
            Object value = state.get(property);
            try {
                return Integer.parseInt(String.valueOf(value)) > 0;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xB8000000);
        int panelWidth = Math.min(560, width - 20);
        int panelHeight = Math.min(420, height - 20);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        context.fill(left, top, left + panelWidth, top + panelHeight, 0xF0141D29);
        context.fill(left, top, left + panelWidth, top + 3, 0xFF55B7FF);
        context.drawTextWithShadow(textRenderer, "WORLD INTEL", left + 16, top + 14, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "CLIENT-SIDE SCAN  •  LOADED CHUNKS ONLY",
                left + 16, top + 34, 0xFF8FAAC4);

        context.fill(left + 12, top + 64, left + 124, top + 90,
                tab == Tab.BASES ? 0xFF28506B : 0xFF202B38);
        context.fill(left + 130, top + 64, left + 242, top + 90,
                tab == Tab.CHUNKS ? 0xFF28506B : 0xFF202B38);

        if (tab == Tab.BASES) renderBases(context, left, top, panelWidth, panelHeight);
        else renderChunks(context, left, top, panelWidth, panelHeight);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderBases(DrawContext context, int left, int top, int panelWidth, int panelHeight) {
        context.drawTextWithShadow(textRenderer, "POSSIBLE PLAYER-BUILT AREAS",
                left + 16, top + 104, 0xFF75E0A1);
        context.drawTextWithShadow(textRenderer, scanMessage, left + 16, top + 122, 0xFFB8C7D9);
        context.drawTextWithShadow(textRenderer,
                "Scanned chunks: " + scannedChunks + "   •   Growth clues: " + grownPlantClues,
                left + 16, top + 142, 0xFF9FC5E8);

        int rowY = top + 166;
        if (candidates.isEmpty()) {
            context.fill(left + 14, rowY, left + panelWidth - 14, rowY + 54, 0xFF202B38);
            context.drawTextWithShadow(textRenderer, "No strong candidates yet.",
                    left + 26, rowY + 12, 0xFFE5EDF5);
            context.drawTextWithShadow(textRenderer, "Try scanning while closer to the area.",
                    left + 26, rowY + 30, 0xFF9EADBD);
        } else {
            int rows = Math.min(5, candidates.size());
            for (int i = 0; i < rows; i++) {
                Candidate c = candidates.get(i);
                int y = rowY + i * 34;
                context.fill(left + 14, y, left + panelWidth - 14, y + 29,
                        i % 2 == 0 ? 0xFF202B38 : 0xFF1B2530);
                String confidence = c.score() >= 16 ? "HIGH" : c.score() >= 10 ? "MEDIUM" : "LOW";
                context.drawTextWithShadow(textRenderer,
                        confidence + "  Chunk " + c.chunkX() + ", " + c.chunkZ()
                                + "  •  approx Y " + c.y(),
                        left + 24, y + 5, c.score() >= 16 ? 0xFF75E0A1 : 0xFFFFC66D);
                context.drawTextWithShadow(textRenderer,
                        "Storage " + c.containers() + "  Workstations " + c.workstations()
                                + "  Redstone " + c.redstone() + "  Doors " + c.doors()
                                + (c.underground() ? "  •  underground" : ""),
                        left + 24, y + 17, 0xFFB8C7D9);
            }
        }

        context.drawTextWithShadow(textRenderer,
                "Scores are clues, not proof. Villages and normal builds can look suspicious.",
                left + 16, top + panelHeight - 52, 0xFF8999AA);
    }

    private void renderChunks(DrawContext context, int left, int top, int panelWidth, int panelHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        context.drawTextWithShadow(textRenderer, "LIVE CLIENT CHUNK MAP",
                left + 16, top + 104, 0xFF75E0A1);
        context.drawTextWithShadow(textRenderer,
                "Loaded now: " + (client.world == null ? 0 : client.world.getChunkManager().getLoadedChunkCount())
                        + "  •  Scan radius: " + radius + " chunks",
                left + 16, top + 122, 0xFFB8C7D9);

        int cell = 17;
        int mapSize = (radius * 2 + 1) * cell;
        int mapX = left + 24;
        int mapY = top + 150;
        int centerX = client.player == null ? 0 : client.player.getBlockX() >> 4;
        int centerZ = client.player == null ? 0 : client.player.getBlockZ() >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = centerX + dx;
                int cz = centerZ + dz;
                boolean loaded = client.world != null && client.world.getChunkManager().isChunkLoaded(cx, cz);
                int color = loaded ? 0xFF318C69 : 0xFF303B49;
                int x = mapX + (dx + radius) * cell;
                int y = mapY + (dz + radius) * cell;
                context.fill(x, y, x + cell - 2, y + cell - 2, color);
                if (dx == 0 && dz == 0) {
                    context.fill(x + 4, y + 4, x + cell - 6, y + cell - 6, 0xFFFFFFFF);
                }
            }
        }

        int infoX = mapX + mapSize + 26;
        context.drawTextWithShadow(textRenderer, "LEGEND", infoX, mapY + 4, 0xFFE5EDF5);
        context.fill(infoX, mapY + 24, infoX + 10, mapY + 34, 0xFF318C69);
        context.drawTextWithShadow(textRenderer, "Loaded by client", infoX + 16, mapY + 25, 0xFFB8C7D9);
        context.fill(infoX, mapY + 44, infoX + 10, mapY + 54, 0xFF303B49);
        context.drawTextWithShadow(textRenderer, "Not loaded now", infoX + 16, mapY + 45, 0xFFB8C7D9);
        context.drawTextWithShadow(textRenderer, "Center = your chunk", infoX, mapY + 70, 0xFF9FC5E8);
        context.drawTextWithShadow(textRenderer, "Green means loaded", infoX, mapY + 88, 0xFF9FC5E8);
        context.drawTextWithShadow(textRenderer, "at this moment only.", infoX, mapY + 102, 0xFF9FC5E8);

        context.drawTextWithShadow(textRenderer,
                "Plant growth stages are counted as clues only; they cannot prove past chunk loading.",
                left + 16, top + panelHeight - 52, 0xFF8999AA);
    }

    @Override
    public boolean shouldPause() { return false; }
}
