# DimensionWorks Item Pickup Guard

Forge 1.20.1 server-compatible fix for duplicate ground-item pickup races between Create belts and Sophisticated Backpacks pickup/magnet upgrades.

When an ItemEntity has already been removed, subsequent calls to `ItemEntity#getItem()` return `ItemStack.EMPTY`. This prevents another pickup handler from transferring the same old stack after the first handler has consumed and discarded the entity.

The mixin does not disable either mod's pickup feature and does not alter the stack of a live entity. Partial transfers remain available because the entity is not removed while a remainder exists.

Build:

```sh
gradle build --no-daemon
```

## 0.1.0

- Add a Mixin guard that hides the stack of an already-removed ItemEntity.
- Keep Create and Sophisticated Backpacks pickup behavior enabled.
- Preserve the remainder of partially transferred stacks.

Build verification: GitHub Actions successfully ran `gradle build --no-daemon` with Java 17 and Gradle 8.8. The resulting JAR contains the compiled Mixin, its refmap, the Mixin configuration, and the `MixinConfigs` manifest entry.

Actual in-game acceptance is still required for the simultaneous Create/backpack scenario.
