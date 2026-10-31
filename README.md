# Minecraft AI Build — Fabric 1.21.11

Starter GitHub project for a Java 21 / Fabric 1.21.11 mod.

## Build automatically on GitHub

1. Create a new GitHub repository.
2. Upload the **contents** of this folder (not the enclosing ZIP file).
3. Commit the files to the `main` branch.
4. Open the repository's **Actions** tab.
5. Select **Build Fabric Mod**. GitHub Actions will compile the project.
6. When the run finishes, open the successful workflow run and download the
   `minecraft-ai-build-jar` artifact.

The built JAR is created under `build/libs/` by Gradle and uploaded as a
workflow artifact. The workflow runs on every push and pull request.

## Current status

This is a build-project starter, not the completed AI builder. It includes a
Fabric entrypoint and a GitHub Actions build workflow. It does **not** yet
implement AI planning, Litematica/Sponge schematic import, pathfinding,
material gathering, construction pacing, a settings screen, or multiplayer
permission enforcement. Those features need to be added and tested before
using it on a server.

## Local build (optional)

Requires JDK 21 and Gradle 8.14.3:

```bash
gradle build
```
