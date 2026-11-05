# DimensionWorks Transfer

Adds `dw:transfer`, a per-player isolated staging dimension for DimensionWorks.

## Behaviour

- **Dimension key access.** The configurable `P` key remains the return shortcut after the key tutorial. Completing Gear Heart stage 2 grants the key, stores the main-hand stack, and locks player controls until the key sends them into their transfer plot.
- **Independent admin path.** The OP 2 `/dwtransfer` command and the player key use separate permission paths.
- **Parcel dispatch.** Packed players enter a protected 1×1×1 terminal in their own plot to choose the overworld, nether, end or Twilight Forest. Alex's Caves and the Aether appear with a separate lock overlay and are rejected server-side.
- **Create logistics.** The paired DimensionWorks CMP build lets packed players ride belts. A packed player near a Frogport targeting a running chain conveyor is drawn in and attached using Create's chain-riding synchronization.
- **Progression tutorial.** Gear Heart stage 2 unlocks the temporary dimension key. Its original main-hand stack NBT and selected slot are restored after the first successful trip into the transfer plot.
- **Void world.** The chunk generator emits nothing at all. All blocks come from plot setup.
- **No sky.** A custom `DimensionSpecialEffects` suppresses sky, clouds, and weather rendering.
- **Constant light.** `fixed_time` is pinned to noon and the lightmap is forced bright, so every
  block reads as overworld daylight at all times.
- **Permanent peace.** The biome declares no spawners, hostile entities are refused on join, and the
  dimension type disables monster spawning. Other dimensions are unaffected.
- **Dispatch terminal.** A protected 1×1 terminal is built at the plot centre. Packed players can enter it to open the destination screen; route imagery comes from a JSON catalog and locked destinations are overlaid with a separate image.
- **One plot per player.** On first entry a player is assigned a 3x3 chunk plot, teleported to its
  centre, and the plot is built around them.
- **Confinement.** A barrier ring is placed on the plot's outer blocks and a per-tick clamp keeps
  players inside it no matter how they try to leave.

## Plot layout

Plots live on a square grid with a stride of 14 chunks, so two neighbours always leave 11 chunks of
empty space between their edges. Claims are handed out along a square spiral centred on grid
`(0,0)`, which keeps every plot near the origin instead of producing a long single-file strip.

| Ring | Cells | Furthest plot origin |
|------|-------|----------------------|
| 0 | 1 | chunk (0, 0) |
| 1 | 9 | chunk (14, 14) |
| 2 | 25 | chunk (28, 28) |
| 5 | 81 | chunk (70, 70) |

## Plot contents

A plot covers 48x48 blocks (3x3 chunks), from `minX`/`minZ` to `minX+47`/`minZ+47`.

- `y = -128`: bedrock
- `y = -127 .. -64`: stone
- `y > -64`: air, open for the owner to build in
- barrier ring on the four outer block columns, from `y = -128` to the build limit
- barrier ceiling closing the whole top face at the build limit, so a plot is a sealed box

Every `setBlock` call is guarded by `Territory#contains`, so generation can never spill outside the
plot.

## Commands

- `/dwtransfer` - OP 2 administrative entry. It bypasses the permanent unlock tag, sends you to `dw:transfer`, and claims a plot on first use.
- `/dwtransfer` inside `dw:transfer` returns to the saved source location and clears the return point.
- The default `P` key is the return shortcut. Players enter initially by right-clicking the granted KubeJS dimension key while it is in the main hand.

## Build

```sh
gradle build --no-daemon
```
