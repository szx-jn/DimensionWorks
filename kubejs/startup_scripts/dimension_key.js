StartupEvents.registry('item', event => {
  event.create('dimension_key')
    .displayName(Text.translate('item.kubejs.dimension_key'))
    .texture('kubejs:item/dimension_key')
    .maxStackSize(1)
    .rarity('rare')
    .glow(true)
    .tooltip(Text.translate('item.kubejs.dimension_key.tooltip'))
})
