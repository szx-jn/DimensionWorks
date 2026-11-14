// 仅用于独立测试世界；不要随包分发或放入正式实例。此脚本不验证客户端渲染。
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
    var sessions
    try {
      var factory = Java.loadClass('net.minecraftforge.common.util.FakePlayerFactory')
      var registries = Java.loadClass('net.minecraftforge.registries.ForgeRegistries')
      var selection = Java.loadClass('dev.szx.dimensionworks.magicegg.MagicEggSelection')
      var history = Java.loadClass('dev.szx.dimensionworks.magicegg.SelectionHistory')
      var arrayList = Java.loadClass('java.util.ArrayList')
      // 测试读取私有会话以模拟网络确认；生产接口不暴露会话或发放物品。
      var loader = Java.loadClass('java.lang.Thread').currentThread().getContextClassLoader()
      var klass = Java.loadClass('java.lang.Class').forName('dev.szx.dimensionworks.magicegg.MagicEggSelection', false, loader)
      var field = klass.getDeclaredField('SESSIONS')
      field.setAccessible(true)
      sessions = field.get(null)
      var level = event.server.overworld()
      player = factory.getMinecraft(level)
      player.potionEffects.clear()
      player.persistentData.putBoolean('dw_magic_century_egg_pending', false)
      player.setHealth(player.getMaxHealth())
      player.eat(level, Item.of('kubejs:magic_century_egg'))
      var session = sessions.get(player.getUUID())
      check('eating opens selection without countdown', session != null && !player.persistentData.getBoolean('dw_magic_century_egg_pending'))
      var chosen = new arrayList()
      chosen.add('minecraft:speed')
      chosen.add('minecraft:poison')
      chosen.add('minecraft:levitation')
      check('no effects before confirmation', player.potionEffects.active.size() === 0)
      selection.submit(player, session.token(), false, chosen)
      check('confirmation starts ten-minute countdown', player.persistentData.getBoolean('dw_magic_century_egg_pending') &&
        player.persistentData.getLong('dw_magic_century_egg_deadline') === player.age + 12000)
      check('only selected effects active', player.potionEffects.active.size() === 3)
      for (var i = 0; i < chosen.size(); i++) {
        var id = chosen.get(i)
        var active = player.potionEffects.getActive(registries.MOB_EFFECTS.getValue(id))
        check('selected duration and level ' + id, active != null && active.getDuration() === 12000 && active.getAmplifier() === 4)
      }
      check('confirmation saves selection', history.read(player.forgePersistentData).size() === 3)

      // 清除测试倒计时，模拟下一次可正常打开的食用。
      player.potionEffects.clear()
      player.persistentData.putBoolean('dw_magic_century_egg_pending', false)
      player.eat(level, Item.of('kubejs:magic_century_egg'))
      session = sessions.get(player.getUUID())
      check('next selection includes previous choices', session.hasPrevious() && session.previous().size() === 3)
      selection.cancel(player, session.token())
      selection.submit(player, session.token(), false, chosen)
      check('cancelled selection cannot grant effects', !player.persistentData.getBoolean('dw_magic_century_egg_pending') && player.potionEffects.active.size() === 0)
      check('cancel preserves history', history.read(player.forgePersistentData).size() === 3)

      player.eat(level, Item.of('kubejs:magic_century_egg'))
      session = sessions.get(player.getUUID())
      selection.submit(player, session.token(), true, new arrayList())
      check('reuse restores previous effects', player.potionEffects.active.size() === 3 && player.persistentData.getBoolean('dw_magic_century_egg_pending'))
      player.setHealth(player.getMaxHealth())
      player.eat(level, Item.of('kubejs:magic_century_egg'))
      check('second eating kills immediately', player.isDeadOrDying())
      check('death clears effects', player.potionEffects.active.size() === 0)
      check('death clears countdown', !player.persistentData.getBoolean('dw_magic_century_egg_pending'))
      check('death retains last selection', history.read(player.forgePersistentData).size() === 3)
    } catch (error) {
      failures++
      console.error('[DW_EGG_REGRESSION] EXCEPTION ' + error)
    } finally {
      if (player) {
        player.potionEffects.clear()
        player.persistentData.putBoolean('dw_magic_century_egg_pending', false)
        player.persistentData.putLong('dw_magic_century_egg_deadline', 0)
        if (sessions) sessions.remove(player.getUUID())
      }
    }
    console.info('[DW_EGG_REGRESSION] COMPLETE failures=' + failures)
  })
})
