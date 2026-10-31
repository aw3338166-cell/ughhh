# Minecraft AI Build — Fabric 1.21.11

A client-side schematic building assistant for Minecraft Java 1.21.11 / Fabric / Java 21.

## Feature branch

- Read-only NBT parsing for Sponge `.schem` v2/v3 and Litematica `.litematic` files.
- Loads the newest schematic from `config/minecraft-ai-build/`.
- Chat preview summary with dimensions and non-air block count.
- Start, pause/resume, stop, and adjustable pacing controls.
- Hotbar-only material matching and ordinary Minecraft block interactions.
- Server protections remain authoritative; the mod does not bypass claims, permissions, reach, or server rules.

## Install and use

1. Build the mod using the repository's GitHub Actions workflow.
2. Put one `.schem` or `.litematic` file in `config/minecraft-ai-build/`.
3. Join a world/server where you have permission to build.
4. Stand where you want the front-left-bottom corner of the build, facing the build direction. The builder starts three blocks in front of you.
5. Press **F6** to load the newest schematic and see its dimensions/block count.
6. Put the needed block items in your **hotbar**.
7. Press **F7** to start.
8. Press **F8** to pause/resume, **F9** to stop, **[** to slow down, and **]** to speed up.

Additional controls:\n\n- **M** opens the Materials list.\n- **H** opens the Build Dashboard.\n- **R** rotates the schematic placement by 90 degrees (position rotation only).\n- **L** toggles layer-by-layer mode; **Page Up / Page Down** selects the layer.\n- Build progress is saved after successful placements and can be resumed with F7 after restarting, as long as the same schematic is loaded.\n\nRebind keys in Options → Controls → Key Binds → Minecraft AI Build.

## Current limitations

- This is a deterministic client-side builder, not an AI planner.
- Blueprint ghost rendering is not implemented yet; preview is still a chat summary.\n- Rotation currently changes block coordinates only; directional block states are not rotated.
- Block states such as stair direction, door halves, signs, containers, and block-entity data are not restored; blocks use default states.
- It places only blocks whose matching item is present in the hotbar.\n- Chest withdrawal, scaffold-assisted movement, and automatic undo are not implemented yet.
- It skips occupied positions and positions without a solid neighboring face, and stops if the server rejects a placement.
- Some special blocks need orientation, water, redstone, or multi-step placement and may be skipped.
- Test in a creative test world or a server where you have permission before using a valuable build.

The builder uses normal player interactions. Server protections remain authoritative.