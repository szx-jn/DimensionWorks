// Remove every crafting path for Industrial Platform items, including the
// platform itself, the builder, fill adjuster and optional Create fluid pool.
ServerEvents.recipes(event => {
  event.remove({ output: 'industrial_platform:industrial_platform' })
  event.remove({ output: 'industrial_platform:platform_builder' })
  event.remove({ output: 'industrial_platform:fill_adjuster' })
  event.remove({ output: 'industrial_platform:fluid_pool' })
})
