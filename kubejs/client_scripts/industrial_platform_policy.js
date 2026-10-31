// Hide Industrial Platform items from JEI, including non-3x3-capable tools.
JEIEvents.hideItems(event => {
  event.hide('industrial_platform:industrial_platform')
  event.hide('industrial_platform:platform_builder')
  event.hide('industrial_platform:fill_adjuster')
  event.hide('industrial_platform:fluid_pool')
})
