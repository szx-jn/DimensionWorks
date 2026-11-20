StartupEvents.registry('item', event => {
  event.create('gear_heart')
    .displayName(Text.translate('item.kubejs.gear_heart'))
    .tooltip(Text.translate('item.kubejs.gear_heart.tooltip'))
    .maxStackSize(1)
    .rarity('epic')
    .glow(false)
    .modelGenerator(model => {
      model.parent('minecraft:item/generated')
      model.texture('layer0', 'kubejs:item/gear_heart_normal')
      model.override('kubejs:item/gear_heart_damaged', override => {
        override.predicate('custom_model_data', 1)
      })
    })
})

StartupEvents.registry('item', event => {
  event.create('gear_heart_enchanted')
    .displayName(Text.translate('item.kubejs.gear_heart_enchanted'))
    .tooltip(Text.translate('item.kubejs.gear_heart_enchanted.tooltip'))
    .maxStackSize(1)
    .rarity('epic')
    .glow(false)
    .texture('kubejs:item/gear_heart_blessed')
})

StartupEvents.registry('item', event => {
  for (let index = 1; index <= 7; index++) {
    event.create(`gear_heart_repair_fragment_${index}`)
      .displayName(Text.translate(`item.kubejs.gear_heart_repair_fragment_${index}`))
      .tooltip(Text.translate(`item.kubejs.gear_heart_repair_fragment_${index}.tooltip`))
      .maxStackSize(16)
      .rarity('rare')
      .texture(`kubejs:item/gear_heart_repair_fragment_${index}`)
  }
})
