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
        int left = (width - panelWidth) / 2;
        int top = Math.max(10, (height - 320) / 2);
        int buttonY = top + 284;
        int gap = 6;
        int total = panelWidth - 24;
        int toggleWidth = total * 30 / 100;
        int rangeWidth = total * 24 / 100;
        int clearWidth = total * 18 / 100;
        int closeWidth = total - toggleWidth - rangeWidth - clearWidth - gap * 3;
        int x = left + 12;

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
        int panelHeight = Math.min(320, height - 20);
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
        context.drawTextWithShadow(textRenderer,
                "Recent redstone-related sounds received from the server",
                left + 14, top + 54, 0xFFB8C7D9);

        List<RedstoneSoundDetector.Detection> detections = RedstoneSoundDetector.getRecent();
        int rowY = top + 76;
        int rows = Math.max(1, Math.min(9, (panelHeight - 124) / 20));
        if (detections.isEmpty()) {
            context.fill(left + 12, rowY - 5, left + panelWidth - 12, rowY + 38, 0xFF222D39);
            context.drawCenteredTextWithShadow(textRenderer,
                    RedstoneSoundDetector.isEnabled()
                            ? "No redstone sounds detected yet"
                            : "Detector is off",
                    width / 2, rowY + 9, 0xFFCBD5E1);
            context.drawCenteredTextWithShadow(textRenderer,
                    "Move near an active mechanism and wait for it to make a sound",
                    width / 2, rowY + 24, 0xFF8999AA);
        } else {
            int count = Math.min(rows, detections.size());
            for (int i = 0; i < count; i++) {
                RedstoneSoundDetector.Detection detection = detections.get(i);
                int y = rowY + i * 20;
                int bg = (i % 2 == 0) ? 0xFF222D39 : 0xFF1D2732;
                context.fill(left + 12, y - 3, left + panelWidth - 12, y + 15, bg);

                String name = RedstoneSoundDetector.displayName(detection.soundId());
                if (name.length() > 24) name = name.substring(0, 21) + "...";
                String direction = RedstoneSoundDetector.directionFromPlayer(detection);
                String details = direction + "  "
                        + Math.round(RedstoneSoundDetector.distanceFromPlayer(detection)) + "m"
                        + "  Y " + Math.round(detection.y())
                        + "  " + RedstoneSoundDetector.ageLabel(detection);

                context.drawTextWithShadow(textRenderer, name, left + 18, y, 0xFFE8F1FA);
                context.drawTextWithShadow(textRenderer, details,
                        left + panelWidth - 150, y, 0xFF9FC5E8);
            }
        }

        context.drawTextWithShadow(textRenderer,
                "Sound-based only: inactive wiring and silent blocks cannot be detected.",
                left + 14, top + panelHeight - 54, 0xFF8999AA);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}
