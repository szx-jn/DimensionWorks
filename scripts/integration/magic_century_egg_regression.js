// 仅用于独立测试世界；不要随包分发或放入正式实例。
var dwMagicEggRegressionStarted = false
ServerEvents.tick(event => {
  if (dwMagicEggRegressionStarted) return
  dwMagicEggRegressionStarted = true
  event.server.scheduleInTicks(10, () => {
    var failures = 0
    function check(name, ok) {
      console.info('[DW_EGG_REGRESSION] ' + (ok ? 'PASS ' : 'FAIL ') + name)
      if (!ok) failures++
    }

    var player
    try {
      var factory = Java.loadClass('net.minecraftforge.common.util.FakePlayerFactory')
      var registries = Java.loadClass('net.minecraftforge.registries.ForgeRegistries')
      var level = event.server.overworld()
      player = factory.getMinecraft(level)
      player.potionEffects.clear()
      player.persistentData.putBoolean('dw_magic_century_egg_pending', false)
      player.setHealth(1)
      player.eat(level, Item.of('kubejs:magic_century_egg'))

      var effects = registries.MOB_EFFECTS.iterator()
      var timed = 0
      var instant = 0
      var moddedTimed = 0
      while (effects.hasNext()) {
        var effect = effects.next()
        var id = registries.MOB_EFFECTS.getKey(effect).toString()
        var active = player.potionEffects.getActive(effect)
        if (!effect.isBeneficial() || id === 'minecraft:levitation') {
          check('excluded ' + id, active == null)
        } else if (effect.isInstantenous()) {
          instant++
          check('instant has no lasting status ' + id, active == null)
        } else {
          timed++
          if (!id.startsWith('minecraft:')) moddedTimed++
          check('duration and level ' + id, active != null && active.getDuration() === 3600 && active.getAmplifier() === 4)
        }
      }

      check('multiple distinct effects', timed > 1 && player.potionEffects.active.size() === timed)
      check('third-party effects covered', moddedTimed > 0)
      check('instant healing triggered', player.getHealth() > 1)
      check('first eating starts countdown', player.persistentData.getBoolean('dw_magic_century_egg_pending') &&
        player.persistentData.getLong('dw_magic_century_egg_deadline') === player.age + 3600)
      player.eat(level, Item.of('kubejs:magic_century_egg'))
      check('second eating expires countdown', player.persistentData.getLong('dw_magic_century_egg_deadline') === player.age)
      check('second eating retains distinct effects', player.potionEffects.active.size() === timed)
      console.info('[DW_EGG_REGRESSION] COVERAGE timed=' + timed + ' moddedTimed=' + moddedTimed + ' instant=' + instant)
    } catch (error) {
      failures++
      console.error('[DW_EGG_REGRESSION] EXCEPTION ' + error)
    } finally {
      if (player) {
        player.potionEffects.clear()
        player.persistentData.putBoolean('dw_magic_century_egg_pending', false)
        player.persistentData.putLong('dw_magic_century_egg_deadline', 0)
      }
    }
    console.info('[DW_EGG_REGRESSION] COMPLETE failures=' + failures)
  })
})
