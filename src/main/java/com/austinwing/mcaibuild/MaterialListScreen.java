package com.austinwing.mcaibuild;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MaterialListScreen extends Screen {
    private final SchematicData schematic;
    private final List<MaterialRow> rows = new ArrayList<>();
    private TextFieldWidget searchField;
    private String search = "";
    private int scroll;
    private int left, top, panelWidth, panelHeight;
    private int totalRequired, totalAvailable;

    private record MaterialRow(String id, String name, int required, int available) {}

    public MaterialListScreen(SchematicData schematic) {
        super(Text.literal("Build Materials"));
        this.schematic = schematic;
        refreshRows();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(500, width - 30);
        panelHeight = Math.min(390, height - 30);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;

        searchField = new TextFieldWidget(textRenderer, left + 14, top + 48,
                panelWidth - 28, 20, Text.literal("Search materials"));
        searchField.setPlaceholder(Text.literal("Search blocks..."));
        searchField.setMaxLength(64);
        searchField.setText(search);
        searchField.setChangedListener(value -> {
            search = value;
            scroll = 0;
        });
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), button -> refreshRows())
                .dimensions(left + 14, top + panelHeight - 28, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), button -> close())
                .dimensions(left + panelWidth - 104, top + panelHeight - 28, 90, 20).build());

        // Screen.client is assigned by Minecraft before init(); populate inventory now,
        // not in the constructor where client/player may still be null.
        refreshRows();
    }

    private void refreshRows() {
        rows.clear();
        totalRequired = 0;
        totalAvailable = 0;
        if (client == null || client.player == null) return;

        Map<String, Integer> required = new HashMap<>();
        for (SchematicData.BlockEntry entry : schematic.blocks()) {
            required.merge(entry.blockId(), 1, Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            int available = countInventory(entry.getKey());
            Item item = Registries.ITEM.get(Identifier.of(entry.getKey()));
            String name = item == null ? entry.getKey() : new ItemStack(item).getName().getString();
            rows.add(new MaterialRow(entry.getKey(), name, entry.getValue(), available));
            totalRequired += entry.getValue();
            totalAvailable += Math.min(entry.getValue(), available);
        }
        rows.sort(Comparator.comparingInt((MaterialRow row) ->
                row.available() >= row.required() ? 1 : 0)
                .thenComparing(MaterialRow::name, String.CASE_INSENSITIVE_ORDER));
        scroll = Math.min(scroll, Math.max(0, filteredRows().size() - visibleRows()));
    }

    private int countInventory(String id) {
        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(id)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private List<MaterialRow> filteredRows() {
        if (search.isBlank()) return rows;
        String query = search.toLowerCase();
        return rows.stream().filter(row -> row.name().toLowerCase().contains(query)
                || row.id().toLowerCase().contains(query)).toList();
    }

    private int visibleRows() {
        return Math.max(1, (panelHeight - 112) / 23);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // Avoid Screen.renderBackground blur: Dawn's screen pipeline may already blur this frame.
        context.fill(0, 0, width, height, 0xA0000000);
        panelWidth = Math.min(500, width - 30);
        panelHeight = Math.min(390, height - 30);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;

        context.fill(left, top, left + panelWidth, top + panelHeight, 0xEE15191F);
        context.fill(left, top, left + panelWidth, top + 2, 0xFF55B7FF);
        context.drawCenteredTextWithShadow(textRenderer, "BUILD MATERIALS", width / 2, top + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, schematic.name(), left + 14, top + 34, 0xFFB8C7D9);

        int percent = totalRequired == 0 ? 100 : (int) (totalAvailable * 100L / totalRequired);
        context.fill(left + 14, top + 75, left + panelWidth - 14, top + 82, 0xFF343B45);
        int barWidth = (panelWidth - 28) * percent / 100;
        if (barWidth > 0) context.fill(left + 14, top + 75, left + 14 + barWidth, top + 82, 0xFF55C98A);
        context.drawTextWithShadow(textRenderer,
                "Collected: " + totalAvailable + " / " + totalRequired + " blocks (" + percent + "%)",
                left + 14, top + 87, 0xFFE3EAF2);

        int listTop = top + 112;
        int listBottom = top + panelHeight - 38;
        context.fill(left + 10, listTop - 5, left + panelWidth - 10, listBottom, 0x88252D38);
        context.enableScissor(left + 10, listTop, left + panelWidth - 10, listBottom);

        List<MaterialRow> filtered = filteredRows();
        for (int i = scroll; i < Math.min(filtered.size(), scroll + visibleRows()); i++) {
            MaterialRow row = filtered.get(i);
            int y = listTop + (i - scroll) * 23;
            boolean ready = row.available() >= row.required();
            Item item = Registries.ITEM.get(Identifier.of(row.id()));
            if (item != null) context.drawItem(new ItemStack(item), left + 18, y + 1);
            context.drawTextWithShadow(textRenderer, row.name(), left + 42, y + 6, 0xFFFFFFFF);
            String amount = Math.min(row.available(), row.required()) + " / " + row.required()
                    + (ready ? "  READY" : "  NEED " + (row.required() - row.available()));
            int amountWidth = textRenderer.getWidth(amount);
            context.drawTextWithShadow(textRenderer, amount,
                    left + panelWidth - 22 - amountWidth, y + 6,
                    ready ? 0xFF75D69A : 0xFFFF7777);
        }
        context.disableScissor();

        if (filtered.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, "No matching materials.",
                    width / 2, listTop + 12, 0xFFB8C7D9);
        } else if (filtered.size() > visibleRows()) {
            context.drawTextWithShadow(textRenderer, "Scroll to see all " + filtered.size() + " materials",
                    left + 14, listBottom + 5, 0xFF9EACBC);
        }
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listTop = top + 112;
        int listBottom = top + panelHeight - 38;
        if (mouseX >= left && mouseX <= left + panelWidth && mouseY >= listTop && mouseY <= listBottom) {
            int max = Math.max(0, filteredRows().size() - visibleRows());
            scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(verticalAmount) * 3));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
