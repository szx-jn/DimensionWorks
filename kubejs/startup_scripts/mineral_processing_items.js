// Startup registration for custom mineral-processing intermediates.
StartupEvents.registry('item', event => {
  event.create('iron_concentrate')
    .displayName('铁质活化料')
    .texture('minecraft:item/raw_iron')
})
