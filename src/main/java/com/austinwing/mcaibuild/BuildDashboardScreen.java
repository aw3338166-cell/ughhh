package com.austinwing.mcaibuild;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class BuildDashboardScreen extends Screen {
    public BuildDashboardScreen() { super(Text.literal("AI Build Control Center")); }

    @Override
    protected void init() {
        int panelWidth = Math.min(420, width - 24);
        int left = (width - panelWidth) / 2;
        int top = height / 2 - 112;
        int gap = 6;
        int buttonWidth = (panelWidth - 32 - gap) / 2;
        int x1 = left + 10, x2 = x1 + buttonWidth + gap, y = top + 106;

        addDrawableChild(ButtonWidget.builder(Text.literal("Load newest schematic"), b -> SchematicBuilderClient.dashboardLoadLatest()).dimensions(x1, y, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Materials + storage"), b -> SchematicBuilderClient.dashboardOpenMaterials()).dimensions(x2, y, buttonWidth, 20).build());
        y += 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("Start / Resume"), b -> SchematicBuilderClient.dashboardStart()).dimensions(x1, y, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Pause / Resume"), b -> SchematicBuilderClient.dashboardPause()).dimensions(x2, y, buttonWidth, 20).build());
        y += 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("Repair missing blocks"), b -> SchematicBuilderClient.dashboardRepair()).dimensions(x1, y, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Stop build"), b -> SchematicBuilderClient.dashboardStop()).dimensions(x2, y, buttonWidth, 20).build());
        y += 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("Rotate 90°"), b -> SchematicBuilderClient.dashboardRotate()).dimensions(x1, y, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Layer mode"), b -> SchematicBuilderClient.dashboardToggleLayerMode()).dimensions(x2, y, buttonWidth, 20).build());
        y += 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("Toggle 3D preview"), b -> SchematicBuilderClient.dashboardTogglePreview()).dimensions(x1, y, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close()).dimensions(x2, y, buttonWidth, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xA0000000);
        int panelWidth = Math.min(420, width - 24);
        int left = (width - panelWidth) / 2;
        int top = height / 2 - 132;
        context.fill(left, top, left + panelWidth, top + 274, 0xEE15191F);
        context.fill(left, top, left + panelWidth, top + 2, 0xFF55B7FF);
        context.drawCenteredTextWithShadow(textRenderer, "AI BUILD CONTROL CENTER", width / 2, top + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "Schematic: " + SchematicBuilderClient.dashboardSchematicName(), left + 14, top + 34, 0xFFE3EAF2);
        context.drawTextWithShadow(textRenderer, "Status: " + SchematicBuilderClient.dashboardStatus(), left + 14, top + 50, 0xFFB8C7D9);
        context.drawTextWithShadow(textRenderer, "Progress: " + SchematicBuilderClient.dashboardProgress(), left + 14, top + 66, 0xFFB8C7D9);
        context.drawTextWithShadow(textRenderer, "F6 load | F7 start | F8 pause | F9 stop | M materials", left + 14, top + 84, 0xFF9FD5FF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override public boolean shouldPause() { return false; }
}
