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

Additional controls:\n\n- **M** opens the Materials list.\n- **H** opens the Build Dashboard.\n- **R** rotates the schematic placement by 90 degrees (position rotation only).\n- **L** toggles layer-by-layer mode; **Page Up / Page Down** selects the layer.
- **B** toggles the optional snake build order (alternating row direction); the original schematic order remains the default.\n- Build progress is saved after successful placements and can be resumed with F7 after restarting, as long as the same schematic and dimension are loaded. Recovery also stores placement rotation, layer settings, and build order.\n\nRebind keys in Options → Controls → Key Binds → Minecraft AI Build.

## Current limitations

- This is a deterministic client-side builder, not an AI planner.
- Blueprint ghost rendering is not implemented yet; preview is still a chat summary.\n- Rotation currently changes block coordinates only; directional block states are not rotated.
- Block states such as stair direction, door halves, signs, containers, and block-entity data are not restored; blocks use default states.
- It places only blocks whose matching item is present in the hotbar.\n- Chest withdrawal, scaffold-assisted movement, and automatic undo are not implemented yet.
- It skips occupied positions and positions without a solid neighboring face, and stops if the server rejects a placement.
- Some special blocks need orientation, water, redstone, or multi-step placement and may be skipped.
- Test in a creative test world or a server where you have permission before using a valuable build.

The builder uses normal player interactions. Server protections remain authoritative.

## Base Finder / World Intel

Open World Intel with **P** and choose **Base Finder**. Select a scan radius (1–3 chunks) and press **Scan nearby**.

- Scans only chunks already loaded and available to the client; it does not request unloaded chunks or inspect server-only data.
- Processes a limited number of block positions per client tick to avoid the old full-radius synchronous scan.
- Scores candidate chunks using combinations of storage, workstations, redstone components, doors, crops, and lighting.
- Displays approximate chunk-center coordinates, a Y estimate, evidence counts, and a heuristic confidence label.
- Results are clues for authorized inspection, not proof that a player base is present. Villages, public builds, and natural/utility areas can produce false positives.

The scanner reads world state on the client thread in small batches for API safety. It is incremental rather than a separate background thread.


### Base Radar

World Intel now has a **Base Radar** tab. After a Base Finder scan completes, it plots candidate chunks inside the scan area on a clickable grid. Marker colors represent heuristic confidence (red/high, amber/medium, blue/low); the center marker identifies the scan-origin chunk. Selecting a candidate displays its approximate chunk-center coordinates and evidence counts.

The radar displays the current scan's in-memory results. It is not a live view of unloaded terrain, and candidate results are not persisted between game sessions yet.


## Automated tests

The project includes JUnit 5 tests for Litematica bit unpacking, schematic transforms, material counts, and redstone bearing math. Run them with `gradle test`; the CI build runs the full `gradle build` task.
