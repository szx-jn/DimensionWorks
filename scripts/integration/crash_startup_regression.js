// Isolated validation world only; never distribute with the pack.
var dwCrashProbeStarted = false
ServerEvents.tick(event => {
  if (dwCrashProbeStarted) return
  dwCrashProbeStarted = true
  var server = event.server
  server.scheduleInTicks(40, () => {
    var failures = 0
    function check(name, ok) {
      console.info('[DW_CRASH_REGRESSION] ' + (ok ? 'PASS ' : 'FAIL ') + name)
      if (!ok) failures++
    }
    try {
      var Pos = Java.loadClass('net.minecraft.core.BlockPos')
      var FloatingLong = Java.loadClass('mekanism.api.math.FloatingLong')
      var Types = Java.loadClass('net.minecraft.world.entity.EntityType')
      var level = server.overworld()
      var generatorPos = new Pos(8, 120, 8)
      var cubePos = new Pos(8, 120, 7)
      var millPos = new Pos(10, 120, 8)
      server.runCommandSilent('forceload add 8 8')
      check('place generator', server.runCommandSilent('setblock 8 120 8 mekanismgenerators:heat_generator[facing=north]') > 0)
      check('place energy cube', server.runCommandSilent('setblock 8 120 7 mekanism:basic_energy_cube') > 0)
      check('place millstone', server.runCommandSilent('setblock 10 120 8 kaleidoscope_cookery:millstone') > 0)
      var generator = level.getBlockEntity(generatorPos)
      var cube = level.getBlockEntity(cubePos)
      var mill = level.getBlockEntity(millPos)
      var chicken = Types.CHICKEN.create(level)
      check('millstone LivingEntity injection executes', typeof mill.canBindEntity(chicken) === 'boolean')
      generator.getEnergyContainer().setEnergy(FloatingLong.create(1000))
      check('seed generator energy', !generator.getEnergyContainer().getEnergy().isZero())
      server.scheduleInTicks(20, () => {
        try {
          check('generator tick consumes internal energy', generator.getEnergyContainer().getEnergy().isZero())
          check('adjacent cube receives no FE', cube.getEnergyContainer().getEnergy().isZero())
        } catch (error) { failures++; console.error('[DW_CRASH_REGRESSION] ' + error) }
        console.info('[DW_CRASH_REGRESSION] COMPLETE failures=' + failures)
        server.runCommandSilent('setblock 8 120 8 minecraft:air')
        server.runCommandSilent('setblock 8 120 7 minecraft:air')
        server.runCommandSilent('setblock 10 120 8 minecraft:air')
        server.runCommandSilent('forceload remove 8 8')
      })
    } catch (error) {
      console.error('[DW_CRASH_REGRESSION] EXCEPTION ' + error)
      console.info('[DW_CRASH_REGRESSION] COMPLETE failures=' + (failures + 1))
    }
  })
})
