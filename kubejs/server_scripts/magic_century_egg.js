const $DWMagicEggMobEffectInstance = Java.loadClass('net.minecraft.world.effect.MobEffectInstance')
const $DWMagicEggMobEffects = Java.loadClass('net.minecraft.world.effect.MobEffects')
const $DWMagicEggBuiltInRegistries = Java.loadClass('net.minecraft.core.registries.BuiltInRegistries')
const $DWMagicEggParticleTypes = Java.loadClass('net.minecraft.core.particles.ParticleTypes')

const DW_MAGIC_EGG_ID = 'kubejs:magic_century_egg'
const DW_MAGIC_EGG_DURATION_TICKS = 20 * 60
const DW_MAGIC_EGG_DAMAGE = 9999999999999
const DW_MAGIC_EGG_PENDING = 'dw_magic_century_egg_pending'
const DW_MAGIC_EGG_DEADLINE = 'dw_magic_century_egg_deadline'

let dwMagicEggFireworkShows = []

ServerEvents.recipes(event => {
  event.remove({ id: 'kaleidoscope_chinesefood:pickle_jar/century_egg' })
  event.remove({
    type: 'kaleidoscope_chinesefood:pickle_jar',
    output: 'kaleidoscope_chinesefood:century_egg'
  })
})

function dwApplyMagicEggEffects(player) {
  const effects = $DWMagicEggBuiltInRegistries.MOB_EFFECT.iterator()
  while (effects.hasNext()) {
    const effect = effects.next()
    if (!effect.isBeneficial()) continue

    player.addEffect(new $DWMagicEggMobEffectInstance(
      effect,
      DW_MAGIC_EGG_DURATION_TICKS,
      4,
      false,
      true,
      true
    ))
  }

  const explicitEffects = [
    [$DWMagicEggMobEffects.ABSORPTION, 9],
    [$DWMagicEggMobEffects.LEVITATION, 1],
    [$DWMagicEggMobEffects.JUMP, 4],
    [$DWMagicEggMobEffects.MOVEMENT_SPEED, 4],
    [$DWMagicEggMobEffects.FIRE_RESISTANCE, 4]
  ]
  explicitEffects.forEach(([effect, amplifier]) => {
    player.addEffect(new $DWMagicEggMobEffectInstance(
      effect,
      DW_MAGIC_EGG_DURATION_TICKS,
      amplifier,
      false,
      true,
      true
    ))
  })
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
  dwApplyMagicEggEffects(player)
  player.persistentData.putBoolean(DW_MAGIC_EGG_PENDING, true)
  player.persistentData.putLong(DW_MAGIC_EGG_DEADLINE, player.age + DW_MAGIC_EGG_DURATION_TICKS)
})

PlayerEvents.tick(event => {
  const player = event.player
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
  player.hurt(level.damageSources().genericKill(), DW_MAGIC_EGG_DAMAGE)
})

ServerEvents.tick(event => dwTickMagicEggFireworks(event))
