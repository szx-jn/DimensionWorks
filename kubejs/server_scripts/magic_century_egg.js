const $DWMagicEggParticleTypes = Java.loadClass('net.minecraft.core.particles.ParticleTypes')
const $DWMagicEggMobEffects = Java.loadClass('net.minecraftforge.registries.ForgeRegistries')

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

// 每页每栏 12 个效果；选择只属于当前菜单，不与其他玩家共享。
function dwMagicEggEffectButton(gui, entry, x, y, selected) {
  const slot = gui.getSlot(x, y)
  function refresh() {
    slot.item = Item.of(selected[entry.id] ? 'minecraft:lime_dye' : 'minecraft:gray_dye')
      .withName(Text.of(selected[entry.id] ? '已选择：' : '未选择：').append(Text.translate(entry.effect.getDescriptionId())).append(' V'))
      .withLore([Text.of(entry.id), Text.of('点击切换；持续效果 3 分钟，瞬时效果立即触发')])
  }
  refresh()
  slot.leftClicked = () => {
    selected[entry.id] = !selected[entry.id]
    refresh()
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

function dwOpenMagicEggSelection(player) {
  const groups = [[], [], []]
  const selected = {}
  const effects = $DWMagicEggMobEffects.MOB_EFFECTS.iterator()
  while (effects.hasNext()) {
    let effect = effects.next()
    let id = $DWMagicEggMobEffects.MOB_EFFECTS.getKey(effect).toString()
    let category = effect.getCategory().toString()
    groups[category === 'BENEFICIAL' ? 0 : category === 'NEUTRAL' ? 1 : 2].push({ id: id, effect: effect })
  }
  const pages = Math.max(1, Math.ceil(Math.max(groups[0].length, groups[1].length, groups[2].length) / 12))
  let page = 0
  let confirmed = false
  function render(gui) {
    gui.playerSlots = false
    for (let index = 0; index < 54; index++) {
      gui.slots[index].item = Item.empty
      gui.slots[index].resetClickHandlers()
    }
    const names = ['增益状态', '中立状态', '减益状态']
    const colors = ['minecraft:green_stained_glass_pane', 'minecraft:yellow_stained_glass_pane', 'minecraft:red_stained_glass_pane']
    for (let group = 0; group < 3; group++) {
      gui.button(group * 3 + 1, 0, Item.of(colors[group]), Text.of(names[group] + ' · 默认 V'), () => {})
      for (let offset = 0; offset < 12; offset++) {
        let entry = groups[group][page * 12 + offset]
        if (entry) dwMagicEggEffectButton(gui, entry, group * 3 + offset % 3, 1 + Math.floor(offset / 3), selected)
      }
    }
    gui.button(0, 5, Item.of('minecraft:arrow'), Text.of('上一页'), () => { page = (page + pages - 1) % pages; render(gui) })
    gui.button(1, 5, Item.of('minecraft:paper'), Text.of('第 ' + (page + 1) + ' / ' + pages + ' 页'), () => {})
    gui.button(2, 5, Item.of('minecraft:arrow'), Text.of('下一页'), () => { page = (page + 1) % pages; render(gui) })
    gui.button(4, 5, Item.of('minecraft:barrier').withLore([
      Text.of('有些效果可能造成画面被影响，'),
      Text.of('整合包概不负责这些效果，请谨慎选择')
    ]), Text.of('免责声明 · 请谨慎选择'), () => {})
    gui.button(8, 5, Item.of('minecraft:emerald'), Text.of('确认选择并开始倒计时'), () => {
      if (confirmed || !player.isAlive()) return
      confirmed = true
      player.closeMenu()
      const data = player.persistentData
      data.putBoolean(DW_MAGIC_EGG_PENDING, true)
      data.putLong(DW_MAGIC_EGG_DEADLINE, player.age + DW_MAGIC_EGG_DURATION_TICKS)
      for (let group = 0; group < 3; group++) {
        for (let index = 0; index < groups[group].length; index++) {
          let entry = groups[group][index]
          if (!selected[entry.id]) continue
          if (entry.effect.isInstantenous()) {
            entry.effect.applyInstantenousEffect(player, player, player, 4, 1.0)
          } else {
            player.potionEffects.add(entry.effect, DW_MAGIC_EGG_DURATION_TICKS, 4, false, true)
          }
          if (!player.isAlive()) return
        }
      }
    })
  }
  player.openChestGUI(Text.of('魔法皮蛋：选择本次效果'), 6, gui => render(gui))
}

ItemEvents.foodEaten(DW_MAGIC_EGG_ID, event => {
  const player = event.player
  const data = player.persistentData
  if (data.getBoolean(DW_MAGIC_EGG_PENDING)) {
    data.putLong(DW_MAGIC_EGG_DEADLINE, player.age)
    dwFinishMagicEggDeath(player)
    return
  }
  dwOpenMagicEggSelection(player)
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
