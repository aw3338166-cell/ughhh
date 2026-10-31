package com.austinwing.mcaibuild;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class BuildDashboardScreen extends Screen {
    public BuildDashboardScreen() {
        super(Text.literal("AI Build Dashboard"));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(360, width - 24);
        int left = (width - panelWidth) / 2;
        int top = height / 2 - 72;
        int buttonWidth = (panelWidth - 28) / 3;

        addDrawableChild(ButtonWidget.builder(Text.literal("Start / Resume"), b ->
                SchematicBuilderClient.dashboardStart()).dimensions(left + 10, top + 88, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Pause"), b ->
                SchematicBuilderClient.dashboardPause()).dimensions(left + 14 + buttonWidth, top + 88, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Stop"), b ->
                SchematicBuilderClient.dashboardStop()).dimensions(left + 18 + buttonWidth * 2, top + 88, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(left + panelWidth - 90, top + 118, 80, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int panelWidth = Math.min(360, width - 24);
        int left = (width - panelWidth) / 2;
        int top = height / 2 - 92;
        context.fill(left, top, left + panelWidth, top + 154, 0xEE15191F);
        context.fill(left, top, left + panelWidth, top + 2, 0xFF55B7FF);
        context.drawCenteredTextWithShadow(textRenderer, "AI BUILD DASHBOARD", width / 2, top + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Schematic: " + SchematicBuilderClient.dashboardSchematicName(), left + 14, top + 34, 0xFFE3EAF2);
        context.drawTextWithShadow(textRenderer, "Status: " + SchematicBuilderClient.dashboardStatus(), left + 14, top + 50, 0xFFB8C7D9);
        context.drawTextWithShadow(textRenderer, "Progress: " + SchematicBuilderClient.dashboardProgress(), left + 14, top + 66, 0xFFB8C7D9);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
