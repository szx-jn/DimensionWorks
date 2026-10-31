// Keep Industrial Platform tools and variants out of JEI item search.
JEIEvents.hideItems(event => {
  event.hide('industrial_platform:industrial_platform')
  event.hide('industrial_platform:platform_builder')
  event.hide('industrial_platform:fill_adjuster')
  event.hide('industrial_platform:fluid_pool')
})
