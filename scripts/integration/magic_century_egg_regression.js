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

      check('menu opens without countdown', !player.persistentData.getBoolean('dw_magic_century_egg_pending'))
      var gui = player.containerMenu.data
      var click = Java.loadClass('net.minecraft.world.inventory.ClickType').PICKUP
      var pages = 0
      var found = 0
      do {
        for (var slot = 9; slot < 45; slot++) {
          var stack = gui.slots[slot].item
          var id = String(stack.nbt)
          if (id.indexOf('minecraft:speed') >= 0 || id.indexOf('minecraft:poison') >= 0 || id.indexOf('minecraft:levitation') >= 0) {
            gui.handleClick(slot, click, 0)
            found++
          }
        }
        pages++
        gui.handleClick(47, click, 0)
      } while (String(gui.slots[46].item.hoverName.string).indexOf('第 1 /') < 0 && pages < 100)
      check('three effects selected across pages', found === 3)
      check('no effects before confirmation', player.potionEffects.active.size() === 0)
      gui.handleClick(53, click, 0)
      check('confirmation starts countdown', player.persistentData.getBoolean('dw_magic_century_egg_pending') &&
        player.persistentData.getLong('dw_magic_century_egg_deadline') === player.age + 3600)
      check('only selected effects active', player.potionEffects.active.size() === 3)
      var chosen = ['minecraft:speed', 'minecraft:poison', 'minecraft:levitation']
      for (var i = 0; i < chosen.length; i++) {
        var active = player.potionEffects.getActive(registries.MOB_EFFECTS.getValue(chosen[i]))
        check('selected duration and level ' + chosen[i], active != null && active.getDuration() === 3600 && active.getAmplifier() === 4)
      }
      player.setHealth(player.getMaxHealth())
      player.eat(level, Item.of('kubejs:magic_century_egg'))
      check('second eating kills immediately', player.isDeadOrDying())
      check('death clears effects', player.potionEffects.active.size() === 0)
      check('death clears countdown', !player.persistentData.getBoolean('dw_magic_century_egg_pending'))
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
