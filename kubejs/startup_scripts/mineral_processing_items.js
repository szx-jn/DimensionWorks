// Startup registration for custom mineral-processing intermediates.
StartupEvents.registry('item', event => {
  event.create('crude_sulfur')
    .displayName(Text.translate('item.kubejs.crude_sulfur'))
    .tooltip(Text.translate('item.kubejs.crude_sulfur.tooltip'))
    .texture('minecraft:item/raw_gold')

  event.create('iron_concentrate')
    .displayName(Text.translate('item.kubejs.iron_concentrate'))
    .tooltip(Text.translate('item.kubejs.iron_concentrate.tooltip'))
    .texture('minecraft:item/raw_iron')
})
