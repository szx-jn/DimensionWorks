// Industrial Platform items are intentionally unavailable through crafting.
ServerEvents.recipes(event => {
  event.remove({ output: 'industrial_platform:industrial_platform' })
  event.remove({ output: 'industrial_platform:platform_builder' })
  event.remove({ output: 'industrial_platform:fill_adjuster' })
  event.remove({ output: 'industrial_platform:fluid_pool' })
})
