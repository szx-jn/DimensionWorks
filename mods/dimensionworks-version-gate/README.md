# DimensionWorks Version Gate

Forge 1.20.1 mod that compares the local pack's `VERSION` file with the server's during Forge login negotiation.

- If the client pack version is older, the login is disconnected before entering the world.
- A Minecraft-style dirt-background screen shows both versions.
- **立即更新** downloads the matching native Go client updater from the server's GitHub Release, verifies it against `SHA256SUMS`, runs it with the server's release tag, then exits Minecraft.
- **暂不更新** returns to the title screen.
- Supported automatic-update targets: Windows amd64, Linux amd64 and macOS arm64. Other OS/architecture combinations receive a clear unsupported-platform message.
- The updater is downloaded into the game directory and does not need to be bundled in the Modrinth package.

Build with `gradle build --no-daemon`. The resulting jar is added to `manifest/mods.json` as an in-repository mod for both client and server packaging.
