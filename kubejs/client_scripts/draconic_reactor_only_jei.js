// Hide Draconic Evolution content unrelated to the reactor from JEI.
// Items stay registered so existing worlds and Draconic Evolution internals remain valid.
const DW_REACTOR_VISIBLE = new Set([
  'draconicevolution:draconium_dust',
  'draconicevolution:draconium_ingot',
  'draconicevolution:awakened_draconium_ingot',
  'draconicevolution:reactor_core',
  'draconicevolution:reactor_stabilizer',
  'draconicevolution:reactor_injector',
  'draconicevolution:overworld_draconium_ore',
  'draconicevolution:deepslate_draconium_ore',
  'draconicevolution:nether_draconium_ore',
  'draconicevolution:end_draconium_ore'
])

JEIEvents.hideItems(event => {
  Ingredient.of('@draconicevolution').itemIds.forEach(id => {
    if (!DW_REACTOR_VISIBLE.has(String(id))) {
      event.hide(id)
    }
  })
})
