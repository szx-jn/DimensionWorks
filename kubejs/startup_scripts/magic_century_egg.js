const $DWMagicEggTextColor = Java.loadClass('net.minecraft.network.chat.TextColor')
const $DWMagicEggAwtColor = Java.loadClass('java.awt.Color')

function dwRainbowItemName() {
  const label = Text.translate('item.kubejs.magic_century_egg').getString()
  const result = Text.empty()
  const timeOffset = (Date.now() % 3000) / 3000

  for (let index = 0; index < label.length; index++) {
    const hue = (timeOffset + index * 0.05) % 1
    const rgb = $DWMagicEggAwtColor.HSBtoRGB(hue, 1, 1) & 0xFFFFFF
    result.append(Text.string(label.charAt(index)).color($DWMagicEggTextColor.fromRgb(rgb)))
  }
  return result
}

StartupEvents.registry('item', event => {
  event.create('century_egg_mixture')
    .displayName(Text.translate('item.kubejs.century_egg_mixture'))
    .texture('kaleidoscope_chinesefood:item/century_egg')

  event.create('century_egg_smelted')
    .displayName(Text.translate('item.kubejs.century_egg_smelted'))
    .texture('kaleidoscope_chinesefood:item/century_egg')

  event.create('century_egg_washed')
    .displayName(Text.translate('item.kubejs.century_egg_washed'))
    .texture('kaleidoscope_chinesefood:item/century_egg')

  event.create('incomplete_magic_century_egg', 'create:sequenced_assembly')
    .displayName(Text.translate('item.kubejs.incomplete_magic_century_egg'))
    .tooltip(Text.translate('item.kubejs.incomplete_magic_century_egg.tooltip'))
    .texture('kaleidoscope_chinesefood:item/century_egg')
    .maxStackSize(1)

  event.create('magic_century_egg')
    .name(() => dwRainbowItemName())
    .tooltip(Text.translate('item.kubejs.magic_century_egg.tooltip'))
    .texture('kaleidoscope_chinesefood:item/century_egg')
    .rarity('epic')
    .food(food => food
      .hunger(99999)
      .saturation(99999)
      .alwaysEdible()
    )
})
