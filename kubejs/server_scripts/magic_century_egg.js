const $DWMagicEggParticleTypes = Java.loadClass('net.minecraft.core.particles.ParticleTypes')
const $DWMagicEggMobEffectInstance = Java.loadClass('net.minecraft.world.effect.MobEffectInstance')
const $DWMagicEggBuiltInRegistries = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries')

const DW_MAGIC_EGG_ID = 'kubejs:magic_century_egg'
const DW_MAGIC_EGG_DURATION_TICKS = 20 * 60 * 3
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

ItemEvents.foodEaten(DW_MAGIC_EGG_ID, event => {
  const player = event.player
  const effects = $DWMagicEggBuiltInRegistries.MOB_EFFECT.iterator()
  while (effects.hasNext()) {
    const effect = effects.next()
    if (!effect.isBeneficial()) continue
    const effectId = $DWMagicEggBuiltInRegistries.MOB_EFFECT.getKey(effect)
    if (effectId != null && effectId.toString() === 'minecraft:levitation') continue
    player.addEffect(new $DWMagicEggMobEffectInstance(effect, DW_MAGIC_EGG_DURATION_TICKS, 4, false, true, true))
  }

  const data = player.persistentData

  if (data.getBoolean(DW_MAGIC_EGG_PENDING)) {
    data.putBoolean(DW_MAGIC_EGG_PENDING, false)
    data.putLong(DW_MAGIC_EGG_DEADLINE, 0)

    const level = player.level()
    dwStartMagicEggFireworks(level, player.x, player.y, player.z)
    player.kill()
    return
  }

  data.putBoolean(DW_MAGIC_EGG_PENDING, true)
  data.putLong(DW_MAGIC_EGG_DEADLINE, player.age + DW_MAGIC_EGG_DURATION_TICKS)
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

  data.putBoolean(DW_MAGIC_EGG_PENDING, false)
  data.putLong(DW_MAGIC_EGG_DEADLINE, 0)

  const level = player.level()
  const x = player.x
  const y = player.y
  const z = player.z
  dwStartMagicEggFireworks(level, x, y, z)
  player.kill()
})

ServerEvents.tick(event => dwTickMagicEggFireworks(event))
