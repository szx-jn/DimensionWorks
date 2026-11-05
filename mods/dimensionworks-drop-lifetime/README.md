# DimensionWorks Drop Lifetime

Forge 1.20.1 server-side behavior that controls dropped-item lifetime based on whether the item's spawn chunk is claimed in FTB Chunks.

| Spawn location | Ground lifetime |
|---|---:|
| FTB Chunks claimed chunk | 5 minutes |
| Unclaimed chunk, Common rarity | 25 seconds |
| Unclaimed chunk, Uncommon rarity | 50 seconds |
| Unclaimed chunk, Rare rarity | 75 seconds |
| Unclaimed chunk, Epic rarity | 180 seconds (3 minutes) |
| Unclaimed chunk, custom Forge rarity | 25 seconds |

The claim check uses the FTB Chunks API at item-entity spawn time. FTB Chunks is included in the modpack manifest; FTB Library and FTB Teams are already required dependencies in the pack. Reflection keeps this mod compilable without a hard compile-time dependency on FTB Chunks. If FTB Chunks is absent or its API cannot be queried, the mod falls back to normal rarity-based lifetimes.

The lifetime is assigned once when the item entity joins the server world. Items spawned in claimed chunks receive 5 minutes; items spawned in unclaimed chunks use their vanilla rarity. The rule does not extend item lifetime repeatedly at expiry.

Forge allows other mods to add rarities. Unknown rarities use the Common lifetime instead of causing an error. The handler ignores client worlds and non-item entities; an item loaded from a chunk is evaluated again when it joins the server world.

Build with `gradle build --no-daemon`.
