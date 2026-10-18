# DimensionWorks Version Gate

Forge 1.20.1 mod that compares the local pack's `VERSION` file with the server's during Forge login negotiation.

- If the client pack version is older, the login is disconnected before entering the world.
- A Minecraft-style dirt-background screen shows both versions.
- **立即更新** launches the bundled client updater with the server's release tag, then exits Minecraft.
- **暂不更新** returns to the title screen.
- The updater scripts must be present in the game directory (or `release-updaters/` subdirectory).

Build with `gradle build --no-daemon`. The resulting jar is added to `manifest/mods.json` as an in-repository mod for both client and server packaging.
