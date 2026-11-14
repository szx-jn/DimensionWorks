const $DWMagicEggParticleTypes = Java.loadClass('net.minecraft.core.particles.ParticleTypes')
const $DWMagicEggSelection = Java.loadClass('dev.szx.dimensionworks.magicegg.MagicEggSelection')

const DW_MAGIC_EGG_ID = 'kubejs:magic_century_egg'
const DW_ORIGINAL_CENTURY_EGG_ID = 'kaleidoscope_chinesefood:century_egg'
const DW_MAGIC_EGG_PENDING = 'dw_magic_century_egg_pending'
const DW_MAGIC_EGG_DEADLINE = 'dw_magic_century_egg_deadline'

let dwMagicEggFireworkShows = []

ServerEvents.recipes(event => {
  event.remove({ output: DW_ORIGINAL_CENTURY_EGG_ID })
  event.remove({ output: 'kaleidoscope_chinesefood:century_egg_congee' })
})

function dwPurgeOriginalCenturyEgg(player) {
  for (let slot = 0; slot <= 40; slot++) {
    const stack = player.inventory.getStackInSlot(slot)
    if (stack && stack.id === DW_ORIGINAL_CENTURY_EGG_ID) {
      stack.setCount(0)
    }
  }
}

function dwStartMagicEggFireworks(level, x, y, z) {
  const now = level.getGameTime()
  dwMagicEggFireworkShows.push({
    level: level,
    x: x,
    y: y,
    z: z,
    nextWaveAt: now,
    endAt: now + 15 * 20,
    wave: 0
  })
}

function dwTickMagicEggFireworks(event) {
  for (let index = dwMagicEggFireworkShows.length - 1; index >= 0; index--) {
    const show = dwMagicEggFireworkShows[index]
    const now = show.level.getGameTime()

    if (now >= show.endAt) {
      dwMagicEggFireworkShows.splice(index, 1)
      continue
    }
    if (show.wave < 3 && now >= show.nextWaveAt) {
      show.wave++
      show.nextWaveAt += 5 * 20
    }
    if (show.wave > 0 && now % 2 === 0) {
      show.level.sendParticles(
        $DWMagicEggParticleTypes.FIREWORK,
        show.x,
        show.y + 1,
        show.z,
        24,
        4.5,
        4.5,
        4.5,
        0.08
      )
      show.level.sendParticles(
        $DWMagicEggParticleTypes.END_ROD,
        show.x,
        show.y + 1,
        show.z,
        8,
        4.5,
        4.5,
        4.5,
        0.03
      )
      show.level.sendParticles(
        $DWMagicEggParticleTypes.FLASH,
        show.x,
        show.y + 1,
        show.z,
        3,
        1.5,
        1.5,
        1.5,
        0
      )
    }
  }
}

function dwFinishMagicEggDeath(player) {
  // 先移除增益，防止免伤、复活和持续治疗效果阻止死亡。
  player.potionEffects.clear()
  player.setHealth(0)
  player.die(player.damageSources().genericKill())
  if (!player.isDeadOrDying()) return
  const data = player.persistentData
  data.putBoolean(DW_MAGIC_EGG_PENDING, false)
  data.putLong(DW_MAGIC_EGG_DEADLINE, 0)
  dwStartMagicEggFireworks(player.level(), player.x, player.y, player.z)
}

ItemEvents.foodEaten(DW_MAGIC_EGG_ID, event => {
  const player = event.player
  const data = player.persistentData
  if (data.getBoolean(DW_MAGIC_EGG_PENDING)) {
    data.putLong(DW_MAGIC_EGG_DEADLINE, player.age)
    dwFinishMagicEggDeath(player)
    return
  }
  // KubeJS persistentData 与 Forge persistentData 不同；明确传递原倒计时数据。
  $DWMagicEggSelection.open(player, data)
})

PlayerEvents.inventoryChanged(event => {
  if (event.item.id === DW_ORIGINAL_CENTURY_EGG_ID) {
    event.item.setCount(0)
  }
})

PlayerEvents.loggedIn(event => dwPurgeOriginalCenturyEgg(event.player))

PlayerEvents.tick(event => {
  const player = event.player
  if (player.age % 20 === 0) {
    dwPurgeOriginalCenturyEgg(player)
  }
  const data = player.persistentData
  if (!data.getBoolean(DW_MAGIC_EGG_PENDING)) return
  if (player.age < data.getLong(DW_MAGIC_EGG_DEADLINE)) return

  dwFinishMagicEggDeath(player)
})

ServerEvents.tick(event => dwTickMagicEggFireworks(event))
