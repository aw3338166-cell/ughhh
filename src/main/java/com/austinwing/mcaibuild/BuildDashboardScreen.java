package com.austinwing.mcaibuild;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class BuildDashboardScreen extends Screen {
    private static final int PANEL_BG = 0xF0141B25;
    private static final int PANEL_INNER = 0xFF1B2633;
    private static final int ACCENT = 0xFF55B7FF;
    private static final int MUTED = 0xFF9EAFBF;
    private static final int TEXT = 0xFFEAF2FA;

    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    public BuildDashboardScreen() {
        super(Text.literal("AI Build Control Center"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(600, width - 24);
        panelHeight = Math.min(430, height - 20);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        int innerLeft = panelLeft + 14;
        int gap = 8;
        int buttonWidth = (panelWidth - 36) / 2;
        int rightX = innerLeft + buttonWidth + gap;
        int y = panelTop + 164;
        int rowGap = 28;

        addButton("Load newest schematic", innerLeft, y, buttonWidth,
                b -> SchematicBuilderClient.dashboardLoadLatest());
        addButton("Materials + storage", rightX, y, buttonWidth,
                b -> {
                    close();
                    SchematicBuilderClient.dashboardOpenMaterials();
                });

        y += rowGap;
        addButton("Start / resume build", innerLeft, y, buttonWidth,
                b -> SchematicBuilderClient.dashboardStart());
        addButton("Pause / resume", rightX, y, buttonWidth,
                b -> SchematicBuilderClient.dashboardPause());

        y += rowGap;
        addButton("Repair missing blocks", innerLeft, y, buttonWidth,
                b -> SchematicBuilderClient.dashboardRepair());
        addButton("Stop build", rightX, y, buttonWidth,
                b -> SchematicBuilderClient.dashboardStop());

        y += rowGap;
        addButton("Rotate schematic 90°", innerLeft, y, buttonWidth,
                b -> SchematicBuilderClient.dashboardRotate());
        addButton("Toggle layer mode", rightX, y, buttonWidth,
                b -> SchematicBuilderClient.dashboardToggleLayerMode());

        y += rowGap;
        addButton("Toggle 3D ghost preview", innerLeft, y, buttonWidth,
                b -> SchematicBuilderClient.dashboardTogglePreview());
        addButton("Redstone detector", rightX, y, buttonWidth,
                b -> {
                    close();
                    SchematicBuilderClient.dashboardOpenRedstoneDetector();
                });

        y += rowGap;
        addButton("World Intel", innerLeft, y, buttonWidth,
                b -> {
                    close();
                    SchematicBuilderClient.dashboardOpenWorldIntel();
                });
        addButton("Close dashboard", rightX, y, buttonWidth, b -> close());
    }

    private void addButton(String label, int x, int y, int buttonWidth,
                           java.util.function.Consumer<ButtonWidget> action) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), action::accept)
                .dimensions(x, y, buttonWidth, 22)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        panelWidth = Math.min(600, width - 24);
        panelHeight = Math.min(430, height - 20);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;

        context.fill(0, 0, width, height, 0xB8000000);
        context.fill(panelLeft - 1, panelTop - 1,
                panelLeft + panelWidth + 1, panelTop + panelHeight + 1, 0xFF34485D);
        context.fill(panelLeft, panelTop, panelLeft + panelWidth,
                panelTop + panelHeight, PANEL_BG);
        context.fill(panelLeft, panelTop, panelLeft + panelWidth,
                panelTop + 3, ACCENT);

        context.drawTextWithShadow(textRenderer, "AI BUILD",
                panelLeft + 18, panelTop + 15, ACCENT);
        context.drawTextWithShadow(textRenderer, "CONTROL CENTER",
                panelLeft + 18 + textRenderer.getWidth("AI BUILD") + 7,
                panelTop + 15, TEXT);
        context.drawTextWithShadow(textRenderer, "Your build tools, world intel and live progress",
                panelLeft + 18, panelTop + 31, MUTED);

        int cardLeft = panelLeft + 14;
        int cardTop = panelTop + 54;
        int cardRight = panelLeft + panelWidth - 14;
        context.fill(cardLeft, cardTop, cardRight, cardTop + 96, PANEL_INNER);
        context.fill(cardLeft, cardTop, cardLeft + 3, cardTop + 96, ACCENT);

        String schematic = SchematicBuilderClient.dashboardSchematicName();
        String status = SchematicBuilderClient.dashboardStatus();
        String progress = SchematicBuilderClient.dashboardProgress();
        int statusColor = "Building".equals(status) ? 0xFF75D69A
                : "Paused".equals(status) ? 0xFFFFC66D : MUTED;

        context.drawTextWithShadow(textRenderer, "CURRENT SCHEMATIC",
                cardLeft + 14, cardTop + 10, MUTED);
        context.drawTextWithShadow(textRenderer, trim(schematic, panelWidth - 190),
                cardLeft + 14, cardTop + 25, TEXT);
        context.drawTextWithShadow(textRenderer, "STATUS",
                cardLeft + 14, cardTop + 51, MUTED);
        context.drawTextWithShadow(textRenderer, status.toUpperCase(java.util.Locale.ROOT),
                cardLeft + 14, cardTop + 66, statusColor);

        int progressX = cardLeft + panelWidth / 2 - 4;
        int progressWidth = Math.max(90, cardRight - progressX - 14);
        context.drawTextWithShadow(textRenderer, "BUILD PROGRESS",
                progressX, cardTop + 10, MUTED);
        context.drawTextWithShadow(textRenderer, progress,
                progressX, cardTop + 27, TEXT);
        int percent = progressPercent(progress);
        int barTop = cardTop + 54;
        context.fill(progressX, barTop, progressX + progressWidth, barTop + 7, 0xFF344251);
        if (percent > 0) {
            context.fill(progressX, barTop,
                    progressX + Math.max(2, progressWidth * percent / 100),
                    barTop + 7, 0xFF55C98A);
        }
        context.drawTextWithShadow(textRenderer, percent + "% indexed",
                progressX, barTop + 14, MUTED);

        context.drawTextWithShadow(textRenderer, "QUICK ACTIONS",
                panelLeft + 16, panelTop + 151, ACCENT);
        context.drawTextWithShadow(textRenderer, "H  Dashboard",
                panelLeft + 16, panelTop + panelHeight - 20, MUTED);
        context.drawTextWithShadow(textRenderer, "Keybinds can be changed in Controls",
                panelLeft + 112, panelTop + panelHeight - 20, MUTED);

        super.render(context, mouseX, mouseY, delta);
    }

    private String trim(String value, int maxWidth) {
        if (textRenderer.getWidth(value) <= maxWidth) return value;
        String suffix = "...";
        while (!value.isEmpty() && textRenderer.getWidth(value + suffix) > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value + suffix;
    }

    private static int progressPercent(String progress) {
        try {
            String counts = progress.split("\\|", 2)[0].trim();
            String[] values = counts.split("/");
            if (values.length < 2) return 0;
            long current = Long.parseLong(values[0].trim());
            long total = Long.parseLong(values[1].trim());
            if (total <= 0) return 0;
            return (int) Math.max(0, Math.min(100, current * 100L / total));
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
