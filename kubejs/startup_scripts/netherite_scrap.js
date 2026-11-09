StartupEvents.registry('item', event => {
  event.create('mixed_metal')
    .displayName(Text.translate('item.kubejs.mixed_metal'))
    .tooltip(Text.translate('item.kubejs.mixed_metal.tooltip'))
    .texture('minecraft:item/iron_ingot')

  event.create('incomplete_netherite_scrap')
    .displayName(Text.translate('item.kubejs.incomplete_netherite_scrap'))
    .tooltip(Text.translate('item.kubejs.incomplete_netherite_scrap.tooltip'))
    .texture('minecraft:item/netherite_scrap')
})
