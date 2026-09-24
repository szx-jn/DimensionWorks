# DimensionWorks RPM Limit API

## Default
New players start at **32 RPM**. The hard maximum is **10240 RPM**.

## Command
Requires permission level 2:
```
/dw rpm <player> <limit>
```

This changes the target player's RPM limit for Create kinetic networks they own.

## KubeJS
Server-side KubeJS can call the public Java API:
```js
const RpmLimitApi = Java.loadClass("dev.szx.dimensionworks.rpmlimit.RpmLimitApi")

// ServerPlayer -> limit
RpmLimitApi.setLimit(player, 64)
const limit = RpmLimitApi.getLimit(player)

// UUID string -> read limit
const limit2 = RpmLimitApi.getLimit(player.uuid.toString())
```

Values are clamped to 1..10240. This API changes the player's stored limit immediately; the next kinetic speed calculation uses the new value.
