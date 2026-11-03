# DimensionWorks Spectator Toggle

Forge 1.20.1 mod that lets any player toggle spectator mode with **F4**.

- First press saves the player's exact position, dimension, rotation, and current game mode, then enters spectator mode.
- Second press returns to the saved point and restores the saved game mode.
- The key is configurable under **Options → Controls → DimensionWorks**.
- The server handles the mode change and return point, so the feature does not require OP permissions. Install the mod on both the client and server.
- Return data is saved per player and copied when a death respawn recreates the player.

If the original dimension is unavailable, the player stays in spectator mode and the saved return point is kept for a later attempt.

## Build

```sh
gradle build --no-daemon
```
