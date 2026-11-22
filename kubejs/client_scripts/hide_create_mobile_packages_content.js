// Hide Create: Mobile Packages items from JEI without filtering other namespaces.
const DW_CMP_HIDDEN_ITEMS = [
  'create_mobile_packages:bee_port',
  'create_mobile_packages:portable_stock_ticker',
  'create_mobile_packages:robo_bee',
  'create_mobile_packages:packed_mob_spawn_egg'
]

RecipeViewerEvents.removeEntries('item', event => {
  DW_CMP_HIDDEN_ITEMS.forEach(id => event.remove(id))
})
