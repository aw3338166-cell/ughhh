package com.austinwing.mcaibuild;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;

public final class SelectionAnalyzerScreen extends Screen {
    public SelectionAnalyzerScreen() {
        super(Text.literal("Live Selection Analyzer"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(520, width - 24);
        int left = (width - panelWidth) / 2;
        int bottom = height / 2 + 145;
        int buttonWidth = Math.max(70, (panelWidth - 44) / 3);

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button ->
                        SelectionAnalyzerClient.refresh(client))
                .dimensions(left + 10, bottom, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear selection"), button ->
                        SelectionAnalyzerClient.clearSelection())
                .dimensions(left + 17 + buttonWidth, bottom, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(left + 24 + buttonWidth * 2, bottom, buttonWidth, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA0000000);
        int panelWidth = Math.min(520, width - 24);
        int panelHeight = Math.min(330, height - 24);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        context.fill(left, top, left + panelWidth, top + panelHeight, 0xEE15191F);
        context.fill(left, top, left + panelWidth, top + 2, 0xFF55B7FF);
        context.drawCenteredTextWithShadow(textRenderer, "LIVE SELECTION ANALYZER",
                width / 2, top + 12, 0xFFFFFFFF);

        BlockPos first = SelectionAnalyzerClient.first();
        BlockPos second = SelectionAnalyzerClient.second();
        context.drawTextWithShadow(textRenderer, "Corner 1: " + position(first), left + 14, top + 38, 0xFF9FD5FF);
        context.drawTextWithShadow(textRenderer, "Corner 2: " + position(second), left + 14, top + 54, 0xFF9FD5FF);

        SelectionBounds bounds = SelectionAnalyzerClient.bounds();
        if (bounds == null) {
            context.drawTextWithShadow(textRenderer, "Mark two block corners with J and K.",
                    left + 14, top + 86, 0xFFE3EAF2);
            context.drawTextWithShadow(textRenderer, "Press N to open this analyzer again.",
                    left + 14, top + 104, 0xFFB8C7D9);
        } else {
            context.drawTextWithShadow(textRenderer,
                    "Dimensions: " + bounds.width() + " × " + bounds.height() + " × " + bounds.length(),
                    left + 14, top + 82, 0xFFE3EAF2);
            context.drawTextWithShadow(textRenderer, "Volume: " + bounds.volume() + " block positions",
                    left + 14, top + 100, 0xFFE3EAF2);
            context.drawTextWithShadow(textRenderer, "Non-air blocks found: "
                            + SelectionAnalyzerClient.blockCount(),
                    left + 14, top + 118, 0xFF75D69A);
            context.drawTextWithShadow(textRenderer, "Checked: " + SelectionAnalyzerClient.checked()
                            + " / " + SelectionAnalyzerClient.volume()
                            + " | Unloaded positions skipped: " + SelectionAnalyzerClient.unloadedCount(),
                    left + 14, top + 136, 0xFFB8C7D9);
        }

        context.drawTextWithShadow(textRenderer, SelectionAnalyzerClient.status(),
                left + 14, top + 160, 0xFFFFD27A);
        context.drawTextWithShadow(textRenderer, "ESTIMATED MATERIALS (top 8)",
                left + 14, top + 188, 0xFFFFFFFF);

        List<Map.Entry<String, Integer>> materials = SelectionAnalyzerClient.topMaterials(8);
        int row = 0;
        for (Map.Entry<String, Integer> material : materials) {
            String line = material.getKey() + "  × " + material.getValue();
            context.drawTextWithShadow(textRenderer, line, left + 18, top + 208 + row * 14, 0xFFE3EAF2);
            row++;
        }
        if (materials.isEmpty()) {
            context.drawTextWithShadow(textRenderer, "No material counts yet.",
                    left + 18, top + 208, 0xFF9EACBC);
        }

        context.drawTextWithShadow(textRenderer,
                "Client-side scan only: unloaded chunks are skipped, not requested.",
                left + 14, top + panelHeight - 22, 0xFF9FD5FF);
        super.render(context, mouseX, mouseY, delta);
    }

    private static String position(BlockPos pos) {
        return pos == null ? "not set" : pos.toShortString();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}