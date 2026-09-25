# DimensionWorks Transfer

Adds `dw:transfer`, a per-player isolated staging dimension for DimensionWorks.

## Behaviour

- **Void world.** The chunk generator emits nothing at all. All blocks come from plot setup.
- **No sky.** A custom `DimensionSpecialEffects` suppresses sky, clouds, and weather rendering.
- **Constant light.** `fixed_time` is pinned to noon and the lightmap is forced bright, so every
  block reads as overworld daylight at all times.
- **Permanent peace.** The biome declares no spawners, hostile entities are refused on join, and the
  dimension type disables monster spawning. Other dimensions are unaffected.
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

- `/dwtransfer` - send yourself to `dw:transfer`, claiming a plot on first use.

## Build

```sh
gradle build --no-daemon
```
