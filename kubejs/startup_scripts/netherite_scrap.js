StartupEvents.registry('item', event => {
  event.create('incomplete_netherite_scrap')
    .displayName(Text.translate('item.kubejs.incomplete_netherite_scrap'))
    .tooltip(Text.translate('item.kubejs.incomplete_netherite_scrap.tooltip'))
    .texture('minecraft:item/netherite_scrap')
})
