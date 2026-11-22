// Keep only the mobile packager in the mod's creative tab.
const DW_CMP_HIDDEN_ITEMS = [
  'create_mobile_packages:bee_port',
  'create_mobile_packages:portable_stock_ticker',
  'create_mobile_packages:robo_bee',
  'create_mobile_packages:packed_mob_spawn_egg'
]

StartupEvents.modifyCreativeTab('create_mobile_packages:create_mobile_packages_tab', event => {
  DW_CMP_HIDDEN_ITEMS.forEach(id => event.remove(id))
})
