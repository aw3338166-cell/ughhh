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
    private enum Tab { BASES, CHUNKS, RADAR }
    private static Tab tab = Tab.BASES;
    private static int radius = 2;
    private int selectedCandidate = -1;
    private int radarMapX, radarMapY, radarCell, radarRadius;

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
            refreshScreen();
        }).dimensions(x, top + 68, 112, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Chunk Viewer"), b -> {
            tab = Tab.CHUNKS;
            refreshScreen();
        }).dimensions(x + 118, top + 68, 112, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Base Radar"), b -> {
            tab = Tab.RADAR;
            refreshScreen();
        }).dimensions(x + 236, top + 68, 112, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Scan nearby"), b -> {
            BaseFinderScanner.start(MinecraftClient.getInstance(), radius);
            selectedCandidate = -1;
            refreshScreen();
        }).dimensions(x, buttonY, buttonWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Radius: " + radius + " chunks"), b -> {
            radius = radius == 3 ? 1 : radius + 1;
            refreshScreen();
        }).dimensions(x + buttonWidth + gap, buttonY, buttonWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(x + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20).build());
    }

    private void refreshScreen() {
        clearChildren();
        init();
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
        context.fill(left + 248, top + 64, left + 360, top + 90,
                tab == Tab.RADAR ? 0xFF28506B : 0xFF202B38);

        if (tab == Tab.BASES) renderBases(context, left, top, panelWidth, panelHeight);
        else if (tab == Tab.CHUNKS) renderChunks(context, left, top, panelWidth, panelHeight);
        else renderRadar(context, left, top, panelWidth, panelHeight);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderBases(DrawContext context, int left, int top, int panelWidth, int panelHeight) {
        context.drawTextWithShadow(textRenderer, "POSSIBLE PLAYER-BUILT AREAS",
                left + 16, top + 104, 0xFF75E0A1);
        context.drawTextWithShadow(textRenderer, BaseFinderScanner.message(), left + 16, top + 122, 0xFFB8C7D9);
        context.drawTextWithShadow(textRenderer,
                "Scanned chunks: " + BaseFinderScanner.scannedChunks() + "/" + BaseFinderScanner.totalChunks() + "   •   Progress: " + BaseFinderScanner.progressPercent() + "%",
                left + 16, top + 142, 0xFF9FC5E8);

        int rowY = top + 166;
        if (BaseFinderScanner.candidates().isEmpty()) {
            context.fill(left + 14, rowY, left + panelWidth - 14, rowY + 54, 0xFF202B38);
            context.drawTextWithShadow(textRenderer, "No strong candidates yet.",
                    left + 26, rowY + 12, 0xFFE5EDF5);
            context.drawTextWithShadow(textRenderer, "Try scanning while closer to the area.",
                    left + 26, rowY + 30, 0xFF9EADBD);
        } else {
            int rows = Math.min(5, BaseFinderScanner.candidates().size());
            for (int i = 0; i < rows; i++) {
                BaseFinderScanner.Candidate c = BaseFinderScanner.candidates().get(i);
                int y = rowY + i * 34;
                context.fill(left + 14, y, left + panelWidth - 14, y + 29,
                        i % 2 == 0 ? 0xFF202B38 : 0xFF1B2530);
                String confidence = c.confidence();
                context.drawTextWithShadow(textRenderer,
                        confidence + " (" + c.score() + ")  Chunk " + c.chunkX() + ", " + c.chunkZ()
                                + "  •  approx X " + ((c.chunkX() << 4) + 8)
                                + " Z " + ((c.chunkZ() << 4) + 8) + " Y " + c.y(),
                        left + 24, y + 5, c.score() >= 16 ? 0xFF75E0A1 : 0xFFFFC66D);
                context.drawTextWithShadow(textRenderer,
                        c.evidence(),
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

    private void renderRadar(DrawContext context, int left, int top, int panelWidth, int panelHeight) {
        context.drawTextWithShadow(textRenderer, "DETECTED CANDIDATE RADAR",
                left + 16, top + 104, 0xFF75E0A1);
        context.drawTextWithShadow(textRenderer,
                "Scan origin chunk: " + BaseFinderScanner.scanCenterX() + ", " + BaseFinderScanner.scanCenterZ()
                        + "  •  Radius: " + BaseFinderScanner.scanRadius(),
                left + 16, top + 122, 0xFFB8C7D9);

        radarRadius = BaseFinderScanner.scanRadius();
        radarCell = 30;
        radarMapX = left + 22;
        radarMapY = top + 158;
        int diameter = radarRadius * 2 + 1;
        List<BaseFinderScanner.Candidate> results = BaseFinderScanner.candidates();

        for (int dx = -radarRadius; dx <= radarRadius; dx++) {
            for (int dz = -radarRadius; dz <= radarRadius; dz++) {
                int x = radarMapX + (dx + radarRadius) * radarCell;
                int y = radarMapY + (dz + radarRadius) * radarCell;
                int chunkX = BaseFinderScanner.scanCenterX() + dx;
                int chunkZ = BaseFinderScanner.scanCenterZ() + dz;
                boolean loaded = MinecraftClient.getInstance().world != null
                        && MinecraftClient.getInstance().world.getChunkManager().isChunkLoaded(chunkX, chunkZ);
                int color = loaded ? 0xFF263B4B : 0xFF202B38;
                int candidateIndex = -1;
                for (int i = 0; i < results.size(); i++) {
                    BaseFinderScanner.Candidate c = results.get(i);
                    if (c.chunkX() == chunkX && c.chunkZ() == chunkZ) {
                        candidateIndex = i;
                        color = c.score() >= 18 ? 0xFFB94E55 : c.score() >= 10 ? 0xFFB58A3E : 0xFF547D9A;
                        break;
                    }
                }
                context.fill(x, y, x + radarCell - 3, y + radarCell - 3, color);
                if (candidateIndex == selectedCandidate) {
                    context.fill(x + 2, y + 2, x + radarCell - 5, y + radarCell - 5, 0xFFFFFFFF);
                } else if (dx == 0 && dz == 0) {
                    context.fill(x + 9, y + 9, x + radarCell - 12, y + radarCell - 12, 0xFF65D6FF);
                }
                if (candidateIndex >= 0) {
                    context.drawTextWithShadow(textRenderer, Integer.toString(candidateIndex + 1),
                            x + 10, y + 10, 0xFFFFFFFF);
                }
            }
        }

        int infoX = radarMapX + diameter * radarCell + 22;
        context.drawTextWithShadow(textRenderer, "LEGEND", infoX, radarMapY, 0xFFE5EDF5);
        context.fill(infoX, radarMapY + 20, infoX + 12, radarMapY + 32, 0xFFB94E55);
        context.drawTextWithShadow(textRenderer, "High", infoX + 18, radarMapY + 21, 0xFFB8C7D9);
        context.fill(infoX, radarMapY + 40, infoX + 12, radarMapY + 52, 0xFFB58A3E);
        context.drawTextWithShadow(textRenderer, "Medium", infoX + 18, radarMapY + 41, 0xFFB8C7D9);
        context.fill(infoX, radarMapY + 60, infoX + 12, radarMapY + 72, 0xFF547D9A);
        context.drawTextWithShadow(textRenderer, "Low", infoX + 18, radarMapY + 61, 0xFFB8C7D9);
        context.fill(infoX, radarMapY + 80, infoX + 12, radarMapY + 92, 0xFF263B4B);
        context.drawTextWithShadow(textRenderer, "Loaded", infoX + 18, radarMapY + 81, 0xFFB8C7D9);

        if (selectedCandidate >= 0 && selectedCandidate < results.size()) {
            BaseFinderScanner.Candidate c = results.get(selectedCandidate);
            int detailY = radarMapY + 112;
            context.drawTextWithShadow(textRenderer, "SELECTED #" + (selectedCandidate + 1),
                    infoX, detailY, 0xFF75E0A1);
            context.drawTextWithShadow(textRenderer, c.confidence() + " score " + c.score(),
                    infoX, detailY + 18, 0xFFFFC66D);
            context.drawTextWithShadow(textRenderer, "X " + ((c.chunkX() << 4) + 8)
                    + "  Y " + c.y() + "  Z " + ((c.chunkZ() << 4) + 8),
                    infoX, detailY + 36, 0xFFE5EDF5);
            context.drawTextWithShadow(textRenderer, "Chunk " + c.chunkX() + ", " + c.chunkZ(),
                    infoX, detailY + 54, 0xFFB8C7D9);
            context.drawTextWithShadow(textRenderer, "Storage " + c.containers() + "  Workstations " + c.workstations(),
                    infoX, detailY + 72, 0xFFB8C7D9);
            context.drawTextWithShadow(textRenderer, "Redstone " + c.redstone() + "  Doors " + c.doors(),
                    infoX, detailY + 86, 0xFFB8C7D9);
            context.drawTextWithShadow(textRenderer, "Crops " + c.crops() + "  Lights " + c.lighting()
                    + (c.underground() ? "  Underground" : ""),
                    infoX, detailY + 100, 0xFFB8C7D9);
        } else {
            context.drawTextWithShadow(textRenderer, "Candidates: " + results.size(),
                    infoX, radarMapY + 112, 0xFFE5EDF5);
            context.drawTextWithShadow(textRenderer, "Click a colored", infoX, radarMapY + 132, 0xFFB8C7D9);
            context.drawTextWithShadow(textRenderer, "chunk to inspect it.", infoX, radarMapY + 146, 0xFFB8C7D9);
        }

        context.drawTextWithShadow(textRenderer, "Click a numbered marker to select its evidence.",
                left + 16, top + panelHeight - 52, 0xFF8999AA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == Tab.RADAR && button == 0 && radarCell > 0) {
            List<BaseFinderScanner.Candidate> results = BaseFinderScanner.candidates();
            int dx = (int) ((mouseX - radarMapX) / radarCell) - radarRadius;
            int dz = (int) ((mouseY - radarMapY) / radarCell) - radarRadius;
            if (mouseX >= radarMapX && mouseY >= radarMapY
                    && dx >= -radarRadius && dx <= radarRadius
                    && dz >= -radarRadius && dz <= radarRadius) {
                int chunkX = BaseFinderScanner.scanCenterX() + dx;
                int chunkZ = BaseFinderScanner.scanCenterZ() + dz;
                for (int i = 0; i < results.size(); i++) {
                    BaseFinderScanner.Candidate c = results.get(i);
                    if (c.chunkX() == chunkX && c.chunkZ() == chunkZ) {
                        selectedCandidate = i;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() { return false; }
}
