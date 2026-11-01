// Temporary remote-only integration probe. Never distributed with the pack.
var dwProbeStarted = false
ServerEvents.tick(event => {
  if (dwProbeStarted) return
  dwProbeStarted = true
  var server = event.server
  server.scheduleInTicks(40, () => {
    var level = server.overworld()
    var failures = 0
    function check(name, ok) {
      console.info('[DW_RUNTIME_TEST] ' + (ok ? 'PASS ' : 'FAIL ') + name)
      if (!ok) failures++
    }
    try {
      var $ProbePos = Java.loadClass('net.minecraft.core.BlockPos')
      var $ProbeSpawnType = Java.loadClass('net.minecraft.world.entity.MobSpawnType')
      var $ProbeType = Java.loadClass('net.minecraft.world.entity.EntityType')
      var $ProbeRegistries = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries')
      var $ProbeRL = Java.loadClass('net.minecraft.resources.ResourceLocation')
      var $ProbeHand = Java.loadClass('net.minecraft.world.InteractionHand')
      var $ProbeDirection = Java.loadClass('net.minecraft.core.Direction')
      var $ProbeVec3 = Java.loadClass('net.minecraft.world.phys.Vec3')
      var $ProbeHit = Java.loadClass('net.minecraft.world.phys.BlockHitResult')
      var $ProbeUse = Java.loadClass('net.minecraft.world.item.context.UseOnContext')
      var $ProbePlace = Java.loadClass('net.minecraft.world.item.context.BlockPlaceContext')
      var $ProbeBox = Java.loadClass('net.minecraft.world.phys.AABB')
      var $ProbeItemEntity = Java.loadClass('net.minecraft.world.entity.item.ItemEntity')
      var $ProbeBlock = Java.loadClass('net.minecraft.world.level.block.Block')
      var $ProbeBlocks = Java.loadClass('net.minecraft.world.level.block.Blocks')
      var $ProbeFakePlayers = Java.loadClass('net.minecraftforge.common.util.FakePlayerFactory')
      var $ProbeUUID = Java.loadClass('java.util.UUID')
      var $ProbeProfile = Java.loadClass('com.mojang.authlib.GameProfile')
      var player = $ProbeFakePlayers.get(level, new $ProbeProfile($ProbeUUID.fromString('73bc15d0-e1e4-4be5-b731-a442561e103e'), 'DWValidation'))
      player.setPos(8.5, 81, 8.5)
      var fairy = $ProbeRegistries.ENTITY_TYPE.get(new $ProbeRL('touhou_little_maid:fairy'))
      var pos = new $ProbePos(8, 80, 8)
      level.getChunkAt(pos)
      check('saved pre-ban fairy rejected on chunk load', !server.runCommandSilent('execute if entity @e[type=touhou_little_maid:fairy,tag=dw_persistence_probe]'))
      server.runCommandSilent('kill @e[type=touhou_little_maid:fairy]')
      for (var name of ['NATURAL','SPAWN_EGG','SPAWNER','COMMAND','CHUNK_GENERATION']) {
        var spawned = fairy.spawn(level, pos, $ProbeSpawnType[name])
        check('fairy ' + name + ' rejected from level', !server.runCommandSilent('execute if entity @e[type=touhou_little_maid:fairy]'))
      }
      var direct = fairy.create(level)
      direct.setPos(8,80,8)
      check('fairy direct addFreshEntity rejected', !level.addFreshEntity(direct))
      server.runCommandSilent('summon touhou_little_maid:fairy 8 80 8')
      check('fairy summon adds no entity', !server.runCommandSilent('execute if entity @e[type=touhou_little_maid:fairy]'))
      check('ordinary chicken still spawns', server.runCommandSilent('summon minecraft:chicken 12 80 12') > 0)
      var hit = new $ProbeHit(new $ProbeVec3(8.5,80.5,8.5), $ProbeDirection.UP, pos, false)
      level.setBlockAndUpdate(pos, $ProbeBlocks.STONE.defaultBlockState())
      var egg = Item.of('touhou_little_maid:fairy_spawn_egg')
      player.setItemInHand($ProbeHand.MAIN_HAND, egg)
      egg.item.useOn(new $ProbeUse(player, $ProbeHand.MAIN_HAND, hit))
      check('actual fairy spawn egg adds no fairy', !server.runCommandSilent('execute if entity @e[type=touhou_little_maid:fairy]'))
      var clearDrops = function() {
        level.getEntitiesOfClass($ProbeItemEntity, new $ProbeBox(pos).inflate(4)).forEach(e => e.discard())
      }
      var emptyBoxes = function() {
        var count = 0
        level.getEntitiesOfClass($ProbeItemEntity, new $ProbeBox(pos).inflate(4)).forEach(e => {
          if (e.getItem().id === 'mekanism:cardboard_box') count += e.getItem().count
        })
        return count
      }
      var pack = function(id, state) {
        clearDrops()
        level.setBlockAndUpdate(pos, $ProbeBlocks.STONE.defaultBlockState())
        player.setShiftKeyDown(false)
        if (state) {
          level.setBlockAndUpdate(pos, state)
          if (state.block.id === 'minecraft:chest') level.getBlockEntity(pos).setItem(0, Item.of('minecraft:diamond', 7))
        }
        var stack = Item.of(id)
        player.setItemInHand($ProbeHand.MAIN_HAND, stack)
        stack.item.onItemUseFirst(stack, new $ProbeUse(player, $ProbeHand.MAIN_HAND, hit))
        return level.getBlockEntity(pos)
      }
      var unpack = function() {
        player.setShiftKeyDown(true)
        level.getBlockState(pos).use(level, player, $ProbeHand.MAIN_HAND, hit)
      }
      var tile = pack('create_mobile_packages:mobile_packager')
      check('tool actually packs stone', level.getBlockState(pos).block.id === 'mekanism:cardboard_box')
      var saved = tile.saveWithFullMetadata()
      check('box origin persists in NBT', saved.getBoolean('create_mobile_packages:from_mobile_packager'))
      tile.load(saved)
      unpack()
      check('tool unpack restores stone', level.getBlockState(pos).block.id === 'minecraft:stone')
      check('tool unpack drops no empty box', emptyBoxes() === 0)
      tile = pack('create_mobile_packages:mobile_packager')
      var drops = $ProbeBlock.getDrops(level.getBlockState(pos), level, pos, tile, player, Item.of('minecraft:diamond_pickaxe'))
      check('packed box has harvest drop', !drops.isEmpty())
      var transported = drops.get(0)
      level.setBlockAndUpdate(pos, $ProbeBlocks.AIR.defaultBlockState())
      level.setBlockAndUpdate(pos.below(), $ProbeBlocks.STONE.defaultBlockState())
      player.setShiftKeyDown(false)
      player.setItemInHand($ProbeHand.MAIN_HAND, transported)
      var groundHit = new $ProbeHit(new $ProbeVec3(8.5,80,8.5), $ProbeDirection.UP, pos.below(), false)
      transported.item.place(new $ProbePlace(player, $ProbeHand.MAIN_HAND, transported, groundHit))
      check('transported box can be placed', level.getBlockState(pos).block.id === 'mekanism:cardboard_box')
      clearDrops()
      unpack()
      check('transported tool box restores stone', level.getBlockState(pos).block.id === 'minecraft:stone')
      check('transported tool box drops no empty box', emptyBoxes() === 0)
      pack('mekanism:cardboard_box')
      unpack()
      check('ordinary Mekanism box still drops one box', emptyBoxes() === 1)
      pack('create_mobile_packages:mobile_packager', $ProbeBlocks.CHEST.defaultBlockState())
      unpack()
      check('packed chest restores contents', level.getBlockEntity(pos).getItem(0).count === 7)
      check('chest unpack drops no empty box', emptyBoxes() === 0)
      for (var rid of ['mekanism:rotary/hydrofluoric_acid', 'mekanism:processing/copper/slurry/clean', 'mekanism:reaction/wood_gasification/logs', 'mekanism:separator/water', 'mekanism:evaporating/brine', 'dimensionworks:chemistry/carbon_dioxide_from_coal']) {
        check('preserved recipe ' + rid, server.recipeManager.byKey(new $ProbeRL(rid)).isPresent())
      }
      check('AE2 128 spatial cell recipe loaded', server.recipeManager.byKey(new $ProbeRL('ae2:network/cells/spatial_storage_cell_128_cubed')).isPresent())
    } catch (error) {
      failures++
      console.error('[DW_RUNTIME_TEST] EXCEPTION ' + error)
      if (error.stack) console.error(error.stack)
    }
    console.info('[DW_RUNTIME_TEST] COMPLETE failures=' + failures)
  })
})
