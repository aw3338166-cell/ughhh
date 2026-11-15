package com.austinwing.mcaibuild;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public final class RedstoneDetectorScreen extends Screen {
    public RedstoneDetectorScreen() {
        super(Text.literal("Redstone Sound Detector"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(500, width - 20);
        int top = Math.max(10, (height - 400) / 2);
        int buttonY = top + 364;
        int gap = 6;
        int total = panelWidth - 24;
        int toggleWidth = total * 30 / 100;
        int rangeWidth = total * 24 / 100;
        int clearWidth = total * 18 / 100;
        int closeWidth = total - toggleWidth - rangeWidth - clearWidth - gap * 3;
        int x = (width - panelWidth) / 2 + 12;

        addDrawableChild(ButtonWidget.builder(
                Text.literal(RedstoneSoundDetector.isEnabled() ? "Detector: ON" : "Detector: OFF"),
                b -> {
                    RedstoneSoundDetector.toggleEnabled();
                    MinecraftClient.getInstance().setScreen(new RedstoneDetectorScreen());
                }).dimensions(x, buttonY, toggleWidth, 20).build());
        x += toggleWidth + gap;
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Range: " + RedstoneSoundDetector.getRange() + "m"),
                b -> {
                    RedstoneSoundDetector.cycleRange();
                    MinecraftClient.getInstance().setScreen(new RedstoneDetectorScreen());
                }).dimensions(x, buttonY, rangeWidth, 20).build());
        x += rangeWidth + gap;
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear"),
                b -> RedstoneSoundDetector.clear()).dimensions(x, buttonY, clearWidth, 20).build());
        x += clearWidth + gap;
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"),
                b -> close()).dimensions(x, buttonY, closeWidth, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xB0000000);
        int panelWidth = Math.min(500, width - 20);
        int panelHeight = Math.min(400, height - 20);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        context.fill(left, top, left + panelWidth, top + panelHeight, 0xF018202A);
        context.fill(left, top, left + panelWidth, top + 3, 0xFF55B7FF);
        context.drawCenteredTextWithShadow(textRenderer, "REDSTONE SOUND DETECTOR",
                width / 2, top + 14, 0xFFFFFFFF);
        String status = RedstoneSoundDetector.isEnabled()
                ? "LISTENING  •  " + RedstoneSoundDetector.getRange() + " block range"
                : "PAUSED  •  turn detector on to collect sounds";
        context.drawCenteredTextWithShadow(textRenderer, status,
                width / 2, top + 34, RedstoneSoundDetector.isEnabled() ? 0xFF75E0A1 : 0xFFFFC36B);
        context.drawTextWithShadow(textRenderer, "LIVE RADAR  •  top of radar is the direction you face",
                left + 14, top + 56, 0xFFB8C7D9);

        RedstoneSoundDetector.Detection latest = RedstoneSoundDetector.getLatest();
        int radarLeft = left + 18;
        int radarTop = top + 78;
        drawRadar(context, radarLeft, radarTop, latest);

        int infoX = left + 160;
        if (latest == null) {
            context.drawTextWithShadow(textRenderer, "NO SIGNAL", infoX, radarTop + 24, 0xFFCBD5E1);
            context.drawTextWithShadow(textRenderer, "Waiting for a redstone-related", infoX, radarTop + 44, 0xFF9AAABD);
            context.drawTextWithShadow(textRenderer, "sound inside the selected range.", infoX, radarTop + 58, 0xFF9AAABD);
        } else {
            String name = RedstoneSoundDetector.displayName(latest.soundId());
            if (name.length() > 30) name = name.substring(0, 27) + "...";
            context.drawTextWithShadow(textRenderer, "LATEST SIGNAL", infoX, radarTop + 8, 0xFF75E0A1);
            context.drawTextWithShadow(textRenderer, name, infoX, radarTop + 26, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer,
                    RedstoneSoundDetector.directionFromPlayer(latest) + " compass",
                    infoX, radarTop + 45, 0xFFB8C7D9);
            context.drawTextWithShadow(textRenderer,
                    String.format(java.util.Locale.ROOT, "%.1f blocks away",
                            RedstoneSoundDetector.horizontalDistance(latest)),
                    infoX, radarTop + 60, 0xFF9FC5E8);
            context.drawTextWithShadow(textRenderer,
                    "Height difference: " + Math.round(latest.y() - MinecraftClient.getInstance().player.getY()) + " blocks",
                    infoX, radarTop + 75, 0xFF9FC5E8);
            context.drawTextWithShadow(textRenderer,
                    "Heard " + RedstoneSoundDetector.ageLabel(latest),
                    infoX, radarTop + 90, 0xFF8999AA);
        }

        context.drawTextWithShadow(textRenderer, "RECENT DETECTIONS",
                left + 14, top + 220, 0xFFB8C7D9);
        List<RedstoneSoundDetector.Detection> detections = RedstoneSoundDetector.getRecent();
        int rowY = top + 240;
        int rows = Math.max(1, Math.min(5, (panelHeight - 280) / 20));
        if (detections.isEmpty()) {
            context.fill(left + 12, rowY - 4, left + panelWidth - 12, rowY + 34, 0xFF222D39);
            context.drawCenteredTextWithShadow(textRenderer,
                    RedstoneSoundDetector.isEnabled() ? "No redstone sounds detected yet" : "Detector is off",
                    width / 2, rowY + 8, 0xFFCBD5E1);
        } else {
            int count = Math.min(rows, detections.size());
            for (int i = 0; i < count; i++) {
                RedstoneSoundDetector.Detection detection = detections.get(i);
                int y = rowY + i * 20;
                int bg = (i % 2 == 0) ? 0xFF222D39 : 0xFF1D2732;
                context.fill(left + 12, y - 3, left + panelWidth - 12, y + 15, bg);

                String name = RedstoneSoundDetector.displayName(detection.soundId());
                if (name.length() > 25) name = name.substring(0, 22) + "...";
                String details = RedstoneSoundDetector.directionFromPlayer(detection) + "  "
                        + Math.round(RedstoneSoundDetector.distanceFromPlayer(detection)) + "m"
                        + "  Y " + Math.round(detection.y())
                        + "  " + RedstoneSoundDetector.ageLabel(detection);
                context.drawTextWithShadow(textRenderer, name, left + 18, y, 0xFFE8F1FA);
                context.drawTextWithShadow(textRenderer, details,
                        left + panelWidth - 155, y, 0xFF9FC5E8);
            }
        }

        context.drawTextWithShadow(textRenderer,
                "Sound-based only: inactive wiring and silent blocks cannot be detected.",
                left + 14, top + panelHeight - 22, 0xFF8999AA);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawRadar(DrawContext context, int left, int top,
                           RedstoneSoundDetector.Detection detection) {
        int size = 126;
        int cx = left + size / 2;
        int cy = top + size / 2;
        int radius = 48;
        context.fill(left, top, left + size, top + size, 0xFF202D38);

        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.sqrt(radius * radius - dy * dy);
            context.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, 0xFF263A45);
            int inner = (int) Math.sqrt(35 * 35 - Math.min(35 * 35, dy * dy));
            if (Math.abs(dy) <= 35) {
                context.fill(cx - inner, cy + dy, cx + inner + 1, cy + dy + 1, 0xFF202D38);
            }
        }
        context.fill(cx - radius, cy, cx + radius + 1, cy + 1, 0xFF45606B);
        context.fill(cx, cy - radius, cx + 1, cy + radius + 1, 0xFF45606B);
        context.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFFFFFFF);

        if (detection != null) {
            double angle = Math.toRadians(RedstoneSoundDetector.relativeBearingDegrees(detection));
            int dotX = cx + (int) Math.round(Math.sin(angle) * 31);
            int dotY = cy - (int) Math.round(Math.cos(angle) * 31);
            context.fill(dotX - 4, dotY - 4, dotX + 5, dotY + 5, 0xFFFFC857);
            context.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, 0xFFFFFFFF);
        }
        context.drawCenteredTextWithShadow(textRenderer, "FWD", cx, top - 10, 0xFF75E0A1);
        context.drawCenteredTextWithShadow(textRenderer, "YOU", cx, cy + 9, 0xFFB8C7D9);
    }

    @Override
    public boolean shouldPause() { return false; }
}
