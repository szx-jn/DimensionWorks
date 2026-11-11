const $DWMagicEggParticleTypes = Java.loadClass('net.minecraft.core.particles.ParticleTypes')
const $DWMagicEggMobEffects = Java.loadClass('net.minecraftforge.registries.ForgeRegistries')

const DW_MAGIC_EGG_ID = 'kubejs:magic_century_egg'
const DW_MAGIC_EGG_DURATION_TICKS = 20 * 60 * 3
const DW_ORIGINAL_CENTURY_EGG_ID = 'kaleidoscope_chinesefood:century_egg'
const DW_MAGIC_EGG_PENDING = 'dw_magic_century_egg_pending'
const DW_MAGIC_EGG_DEADLINE = 'dw_magic_egg_deadline'

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
  let attempted = 0
  let applied = 0

  const effects = $DWMagicEggMobEffects.MOB_EFFECTS.iterator()
  while (effects.hasNext()) {
    const effect = effects.next()
    if (!effect.isBeneficial()) continue
    const effectId = $DWMagicEggMobEffects.MOB_EFFECTS.getKey(effect)
    if (effectId == null || effectId.toString() === 'minecraft:levitation') continue

    attempted++
    player.potionEffects.add(effectId.toString(), DW_MAGIC_EGG_DURATION_TICKS, 4, false, true)
    if (player.hasEffect(effect)) applied++
  }

  console.info('[Magic Century Egg] Beneficial effects applied: ' + applied + '/' + attempted)

  const data = player.persistentData

  if (data.getBoolean(DW_MAGIC_EGG_PENDING)) {
    data.putLong(DW_MAGIC_EGG_DEADLINE, player.age)
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

  player.kill()
  if (!player.isDeadOrDying()) return

  data.putBoolean(DW_MAGIC_EGG_PENDING, false)
  data.putLong(DW_MAGIC_EGG_DEADLINE, 0)

  const level = player.level()
  dwStartMagicEggFireworks(level, player.x, player.y, player.z)
})

ServerEvents.tick(event => dwTickMagicEggFireworks(event))
