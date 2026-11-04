package com.austinwing.mcaibuild;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.shape.VoxelShapes;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import java.io.InputStream;
import java.io.OutputStream;

public final class SchematicBuilderClient implements ClientModInitializer {
    private static final Path SCHEMATIC_DIR = FabricLoader.getInstance().getConfigDir().resolve("minecraft-ai-build");
    private static final int MAX_SKIP_PER_TICK = 64;
    private static final Path RECOVERY_FILE = SCHEMATIC_DIR.resolve("build-progress.properties");

    private static KeyBinding loadKey, startKey, pauseKey, stopKey, slowerKey, fasterKey, materialsKey, dashboardKey, rotateKey, layerModeKey, layerUpKey, layerDownKey, ghostKey;
    private static SchematicData schematic;
    private static BlockPos origin;
    private static int index, delayTicks = 6, waitTicks, placed, skipped;
    private static boolean running, paused, layerMode, ghostPreview = true;
    private static int rotation, selectedLayer;
    private static final List<BlockPos> selectedContainerPositions = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MinecraftAIBuild.MOD_ID, "builder"));
        loadKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraft_ai_build.load_preview", GLFW.GLFW_KEY_F6, category));
        startKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraft_ai_build.start", GLFW.GLFW_KEY_F7, category));
        pauseKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraft_ai_build.pause", GLFW.GLFW_KEY_F8, category));
        stopKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraft_ai_build.stop", GLFW.GLFW_KEY_F9, category));
        slowerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraft_ai_build.slower", GLFW.GLFW_KEY_LEFT_BRACKET, category));
        fasterKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraft_ai_build.faster", GLFW.GLFW_KEY_RIGHT_BRACKET, category));
        materialsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraft_ai_build.materials", GLFW.GLFW_KEY_M, category));
        dashboardKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraft_ai_build.dashboard", GLFW.GLFW_KEY_H, category));
        rotateKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraft_ai_build.rotate", GLFW.GLFW_KEY_R, category));
        layerModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraft_ai_build.layer_mode", GLFW.GLFW_KEY_L, category));
        layerUpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraft_ai_build.layer_up", GLFW.GLFW_KEY_PAGE_UP, category));
        layerDownKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraft_ai_build.layer_down", GLFW.GLFW_KEY_PAGE_DOWN, category));
        ghostKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.minecraft_ai_build.ghost_preview", GLFW.GLFW_KEY_G, category));
        ClientTickEvents.END_CLIENT_TICK.register(SchematicBuilderClient::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(SchematicBuilderClient::renderBlueprint);
        try {
            Files.createDirectories(SCHEMATIC_DIR);
        } catch (IOException e) {
            MinecraftAIBuild.LOGGER.error("Could not create schematic folder", e);
        }
        MinecraftAIBuild.LOGGER.info("Schematic builder controls registered.");
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (loadKey.wasPressed()) loadLatest(client);
        if (startKey.wasPressed()) start(client);
        if (pauseKey.wasPressed() && running) {
            paused = !paused;
            tell(client, paused ? "Builder paused." : "Builder resumed.");
        }
        if (stopKey.wasPressed() && running) {
            running = false;
            paused = false;
            tell(client, "Builder stopped. Placed: " + placed + ", skipped: " + skipped + ".");
        }
        if (slowerKey.wasPressed()) {
            delayTicks = Math.min(40, delayTicks + 1);
            tell(client, "Build delay: " + delayTicks + " ticks per block.");
        }
        if (fasterKey.wasPressed()) {
            delayTicks = Math.max(1, delayTicks - 1);
            tell(client, "Build delay: " + delayTicks + " ticks per block.");
        }
        if (materialsKey.wasPressed() && client.currentScreen == null) {
            if (schematic == null) tell(client, "Load a schematic first with F6.");
            else client.setScreen(new MaterialListScreen(schematic));
        }
        if (dashboardKey.wasPressed() && client.currentScreen == null) client.setScreen(new BuildDashboardScreen());
        if (rotateKey.wasPressed()) { rotation = (rotation + 1) % 4; tell(client, "Schematic rotation: " + (rotation * 90) + " degrees."); }
        if (layerModeKey.wasPressed()) { layerMode = !layerMode; tell(client, "Layer-by-layer mode " + (layerMode ? "enabled" : "disabled") + "."); }
        if (ghostKey.wasPressed()) { ghostPreview = !ghostPreview; tell(client, "Ghost preview " + (ghostPreview ? "enabled (wireframe)" : "disabled (solid preview)") + "."); }
        if (schematic != null && layerUpKey.wasPressed()) { selectedLayer = Math.min(schematic.height() - 1, selectedLayer + 1); tell(client, "Selected layer: " + selectedLayer + " / " + (schematic.height() - 1)); }
        if (schematic != null && layerDownKey.wasPressed()) { selectedLayer = Math.max(0, selectedLayer - 1); tell(client, "Selected layer: " + selectedLayer + " / " + (schematic.height() - 1)); }
        if (client.currentScreen != null) return;
        if (!running || paused || schematic == null || client.interactionManager == null) return;
        if (waitTicks > 0) { waitTicks--; return; }
        placeNext(client);
    }

    private static void loadLatest(MinecraftClient client) {
        try {
            Files.createDirectories(SCHEMATIC_DIR);
            Path newest;
            try (var paths = Files.list(SCHEMATIC_DIR)) {
                newest = paths.filter(Files::isRegularFile)
                        .filter(p -> {
                            String n = p.getFileName().toString().toLowerCase();
                            return n.endsWith(".schem") || n.endsWith(".litematic");
                        })
                        .max(Comparator.comparingLong(p -> p.toFile().lastModified()))
                        .orElse(null);
            }
            if (newest == null) {
                tell(client, "Put a .schem or .litematic file in " + SCHEMATIC_DIR + " then press F6.");
                return;
            }
            schematic = SchematicReader.read(newest);
            origin = client.player.getBlockPos().offset(client.player.getHorizontalFacing(), 3);
            index = placed = skipped = 0;
            running = paused = false;
            clearRecovery();
            tell(client, "Loaded " + schematic.name() + " — " + schematic.width() + "x" +
                    schematic.height() + "x" + schematic.length() + ", " + schematic.blocks().size() +
                    " non-air blocks. F7 starts; F6 reloads.");
            showMaterialPreview(client);
        } catch (Exception e) {
            MinecraftAIBuild.LOGGER.error("Schematic load failed", e);
            tell(client, "Could not load schematic: " + e.getMessage());
        }
    }

    private static void start(MinecraftClient client) {
        if (schematic == null) {
            tell(client, "Load a schematic first with F6.");
            return;
        }
        if (client.player.isSpectator()) {
            tell(client, "Builder cannot run in spectator mode.");
            return;
        }
        if (!restoreRecovery(client)) {
            if (origin == null) origin = client.player.getBlockPos().offset(client.player.getHorizontalFacing(), 3);
            index = placed = skipped = 0;
        }
        waitTicks = 0;
        paused = false;
        running = true;
        tell(client, "Building from " + origin.toShortString() +
                ". Make sure you have materials and server permission. F8 pauses; F9 stops.");
    }

    private static void placeNext(MinecraftClient client) {
        List<SchematicData.BlockEntry> blocks = schematic.blocks();
        int examined = 0;
        while (index < blocks.size() && examined++ < MAX_SKIP_PER_TICK) {
            SchematicData.BlockEntry entry = blocks.get(index);
            if (layerMode && entry.y() != selectedLayer) { index++; continue; }
            BlockPos target = origin.add(rotatedX(entry), entry.y(), rotatedZ(entry));
            Block block = Registries.BLOCK.get(Identifier.of(entry.blockId()));
            BlockState desired = schematicState(block, entry);

            if (block == net.minecraft.block.Blocks.AIR) {
                index++;
                skipped++;
                continue;
            }
            BlockState existing = client.world.getBlockState(target);
            if (!existing.isReplaceable()) {
                if (!existing.isOf(block)) skipped++;
                index++;
                continue;
            }

            int slot = findHotbarItem(client, entry.blockId());
            if (slot < 0) {
                index++;
                skipped++;
                continue;
            }

            BlockHitResult hit = findSupport(client, target);
            if (hit == null) {
                index++;
                skipped++;
                continue;
            }

            client.player.getInventory().setSelectedSlot(slot);
            float previousYaw = client.player.getYaw();
            String desiredFacing = entry.properties().get("facing");
            if (desiredFacing != null) {
                try {
                    Direction facing = Direction.valueOf(rotateProperty("facing", desiredFacing).toUpperCase(java.util.Locale.ROOT));
                    if (facing.getAxis().isHorizontal()) {
                        Direction playerFacing = facing.getOpposite();
                        float yaw = switch (playerFacing) {
                            case SOUTH -> 0.0F;
                            case WEST -> 90.0F;
                            case NORTH -> 180.0F;
                            case EAST -> -90.0F;
                            default -> previousYaw;
                        };
                        client.player.setYaw(yaw);
                    }
                } catch (IllegalArgumentException ignored) { }
            }
            ActionResult result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);
            client.player.setYaw(previousYaw);
            if (result.isAccepted()) {
                index++;
                placed++;
                saveRecovery();
                waitTicks = delayTicks - 1;
                return;
            }

            running = false;
            tell(client, "Server/game rejected placement at " + target.toShortString() +
                    ". Builder stopped; check permissions and placement rules.");
            return;
        }

        if (index >= blocks.size()) {
            running = false;
            clearRecovery();
            tell(client, "Build pass finished. Placed: " + placed + ", skipped: " + skipped +
                    ". Some skipped blocks may need orientation/support or more materials.");
        }
    }

    private static BlockState schematicState(Block block, SchematicData.BlockEntry entry) {
        BlockState state = block.getDefaultState();
        for (Map.Entry<String, String> item : entry.properties().entrySet()) {
            Property<?> property = state.getBlock().getStateManager().getProperty(item.getKey());
            if (property != null) state = applyProperty(state, property, rotateProperty(item.getKey(), item.getValue()));
        }
        return state;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState applyProperty(BlockState state, Property property, String value) {
        var parsed = property.parse(value);
        return parsed.isPresent() ? state.with(property, (Comparable) parsed.get()) : state;
    }

    private static String rotateProperty(String key, String value) {
        if (rotation == 0) return value;
        if (key.equals("facing")) {
            try {
                Direction direction = Direction.valueOf(value.toUpperCase(java.util.Locale.ROOT));
                for (int i = 0; i < rotation; i++) direction = direction.rotateYClockwise();
                return direction.asString();
            } catch (IllegalArgumentException ignored) { return value; }
        }
        if (key.equals("axis") && rotation % 2 == 1) {
            if (value.equals("x")) return "z";
            if (value.equals("z")) return "x";
        }
        if (key.equals("shape") && rotation % 2 == 1) {
            if (value.equals("inner_left")) return "inner_right";
            if (value.equals("inner_right")) return "inner_left";
            if (value.equals("outer_left")) return "outer_right";
            if (value.equals("outer_right")) return "outer_left";
        }
        if (key.equals("rotation")) {
            try { return Integer.toString((Integer.parseInt(value) + rotation * 4) % 16); }
            catch (NumberFormatException ignored) { return value; }
        }
        return value;
    }

    private static int rotatedX(SchematicData.BlockEntry entry) {
        return switch (rotation) {
            case 1 -> schematic.length() - 1 - entry.z();
            case 2 -> schematic.width() - 1 - entry.x();
            case 3 -> entry.z();
            default -> entry.x();
        };
    }

    private static int rotatedZ(SchematicData.BlockEntry entry) {
        return switch (rotation) {
            case 1 -> entry.x();
            case 2 -> schematic.length() - 1 - entry.z();
            case 3 -> schematic.width() - 1 - entry.x();
            default -> entry.z();
        };
    }

    private static void renderBlueprint(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (schematic == null || origin == null || client.world == null
                || context.matrices() == null || context.consumers() == null) return;

        // Render the actual block models as a client-only blueprint. Nothing is placed
        // in the world by this renderer; the normal builder still handles placement.
        MatrixStack matrices = context.matrices();
        Vec3d camera = client.gameRenderer.getCamera().getCameraPos();
        VertexConsumer lines = context.consumers().getBuffer(RenderLayers.lines());
        int rendered = 0;
        matrices.push();
        try {
            for (SchematicData.BlockEntry entry : schematic.blocks()) {
                if (rendered >= 2500) break;
                if (layerMode && entry.y() != selectedLayer) continue;

                Block block = Registries.BLOCK.get(Identifier.of(entry.blockId()));
                if (block == net.minecraft.block.Blocks.AIR) continue;

                BlockPos target = origin.add(rotatedX(entry), entry.y(), rotatedZ(entry));
                BlockState existing = client.world.getBlockState(target);
                if (existing.isOf(block)) continue;

                BlockState state = schematicState(block, entry);
                double x = target.getX() - camera.x;
                double y = target.getY() - camera.y;
                double z = target.getZ() - camera.z;

                // Full-bright textured model makes the planned block easy to identify.
                // The cyan outline remains visible around the preview model.
                matrices.push();
                try {
                    matrices.translate(x, y, z);
                    if (!ghostPreview) {
                        client.getBlockRenderManager().renderBlockAsEntity(
                                state, matrices, context.consumers(), 15728880, OverlayTexture.DEFAULT_UV);
                    }
                } finally {
                    matrices.pop();
                }

                var shape = state.getOutlineShape(client.world, target);
                if (!shape.isEmpty()) {
                    VertexRendering.drawOutline(matrices, lines, shape, x, y, z, ghostPreview ? 0x8035D9FF : 0xFF35D9FF, 1.5F);
                }
                rendered++;
            }
        } finally {
            matrices.pop();
        }
    }

    private static void showMaterialPreview(MinecraftClient client) {
        Map<String, Integer> required = new HashMap<>();
        for (SchematicData.BlockEntry entry : schematic.blocks()) required.merge(entry.blockId(), 1, Integer::sum);
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            int available = 0;
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = client.player.getInventory().getStack(slot);
                if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(entry.getKey())) available += stack.getCount();
            }
            int shortBy = entry.getValue() - available;
            if (shortBy > 0) missing.add(entry.getKey() + " x" + shortBy);
        }
        missing.sort(String::compareTo);
        if (missing.isEmpty()) tell(client, "Hotbar material check: all required block items appear available.");
        else tell(client, "Hotbar shortages (first 8): " + String.join(", ", missing.subList(0, Math.min(8, missing.size()))) + (missing.size() > 8 ? " ... and " + (missing.size() - 8) + " more types." : ""));
    }

    private static int findHotbarItem(MinecraftClient client, String blockId) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (!stack.isEmpty() && Registries.ITEM.getId(stack.getItem()).toString().equals(blockId)) return slot;
        }
        return -1;
    }

    private static BlockHitResult findSupport(MinecraftClient client, BlockPos target) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = target.offset(direction);
            BlockState state = client.world.getBlockState(neighbor);
            Direction face = direction.getOpposite();
            if (state.isSideSolidFullSquare(client.world, neighbor, face)) {
                Vec3d point = Vec3d.ofCenter(neighbor).add(
                        face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
                return new BlockHitResult(point, face, neighbor, false);
            }
        }
        return null;
    }


    public static boolean selectLookedAtContainer() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null
                || !(client.crosshairTarget instanceof BlockHitResult hit)) return false;
        BlockPos pos = hit.getBlockPos();
        if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > 36.0) return false;
        var blockEntity = client.world.getBlockEntity(pos);
        if (!(blockEntity instanceof ChestBlockEntity) && !(blockEntity instanceof ShulkerBoxBlockEntity)) return false;
        if (!(blockEntity instanceof Inventory)) return false;
        if (!selectedContainerPositions.contains(pos)) selectedContainerPositions.add(pos.toImmutable());
        return true;
    }

    public static void clearSelectedContainer() {
        selectedContainerPositions.clear();
    }

    public static String selectedContainerLabel() {
        if (selectedContainerPositions.isEmpty()) return "No containers selected";
        return "Selected containers: " + selectedContainerPositions.size();
    }

    public static Map<String, Integer> getSelectedContainerMaterialCounts() {
        Map<String, Integer> counts = new HashMap<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return counts;
        for (BlockPos pos : selectedContainerPositions) {
            if (!client.world.isChunkLoaded(pos)) continue;
            var blockEntity = client.world.getBlockEntity(pos);
            if (!(blockEntity instanceof Inventory inventory)) continue;
            for (int slot = 0; slot < inventory.size(); slot++) {
                addStackAndShulkerContents(inventory.getStack(slot), counts, 0);
            }
        }
        return counts;
    }

    private static void addStackAndShulkerContents(ItemStack stack, Map<String, Integer> counts, int depth) {
        if (stack == null || stack.isEmpty() || depth > 4) return;
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        counts.merge(itemId, stack.getCount(), Integer::sum);
        ContainerComponent contents = stack.get(DataComponentTypes.CONTAINER);
        if (contents != null && depth < 4) {
            for (ItemStack nested : contents.iterateNonEmpty()) {
                addStackAndShulkerContents(nested, counts, depth + 1);
            }
        }
    }

    public static void dashboardStart() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) start(client);
    }

    public static void dashboardPause() {
        if (running) {
            paused = !paused;
            tell(MinecraftClient.getInstance(), paused ? "Builder paused." : "Builder resumed.");
        }
    }

    public static void dashboardStop() {
        running = false;
        paused = false;
        tell(MinecraftClient.getInstance(), "Builder stopped. Saved progress can be resumed with F7.");
    }

    public static String dashboardSchematicName() {
        return schematic == null ? "None loaded" : schematic.name();
    }

    public static String dashboardStatus() {
        return running ? (paused ? "Paused" : "Building") : (schematic == null ? "Idle" : "Ready / stopped");
    }

    public static String dashboardProgress() {
        if (schematic == null) return "0 / 0";
        return Math.min(index, schematic.blocks().size()) + " / " + schematic.blocks().size()
                + " | placed " + placed + " | skipped " + skipped;
    }

    private static void saveRecovery() {
        if (schematic == null || origin == null) return;
        try {
            Files.createDirectories(SCHEMATIC_DIR);
            Properties data = new Properties();
            data.setProperty("schematic", schematic.name());
            data.setProperty("x", Integer.toString(origin.getX()));
            data.setProperty("y", Integer.toString(origin.getY()));
            data.setProperty("z", Integer.toString(origin.getZ()));
            data.setProperty("index", Integer.toString(index));
            data.setProperty("placed", Integer.toString(placed));
            data.setProperty("skipped", Integer.toString(skipped));
            try (OutputStream out = Files.newOutputStream(RECOVERY_FILE)) {
                data.store(out, "Minecraft AI Build recovery state");
            }
        } catch (IOException e) {
            MinecraftAIBuild.LOGGER.warn("Could not save build recovery state", e);
        }
    }

    private static boolean restoreRecovery(MinecraftClient client) {
        if (schematic == null || !Files.isRegularFile(RECOVERY_FILE)) return false;
        try (InputStream in = Files.newInputStream(RECOVERY_FILE)) {
            Properties data = new Properties();
            data.load(in);
            if (!schematic.name().equals(data.getProperty("schematic"))) return false;
            origin = new BlockPos(Integer.parseInt(data.getProperty("x")),
                    Integer.parseInt(data.getProperty("y")), Integer.parseInt(data.getProperty("z")));
            index = Math.max(0, Math.min(schematic.blocks().size(), Integer.parseInt(data.getProperty("index"))));
            placed = Math.max(0, Integer.parseInt(data.getProperty("placed", "0")));
            skipped = Math.max(0, Integer.parseInt(data.getProperty("skipped", "0")));
            tell(client, "Recovered build progress: " + index + " / " + schematic.blocks().size()
                    + ". Continuing from saved position.");
            return true;
        } catch (Exception e) {
            MinecraftAIBuild.LOGGER.warn("Could not restore build recovery state", e);
            return false;
        }
    }

    private static void clearRecovery() {
        try {
            Files.deleteIfExists(RECOVERY_FILE);
        } catch (IOException e) {
            MinecraftAIBuild.LOGGER.warn("Could not clear build recovery state", e);
        }
    }

    private static void tell(MinecraftClient client, String message) {
        if (client.player != null) client.player.sendMessage(Text.literal("[AI Build] " + message), false);
    }
}