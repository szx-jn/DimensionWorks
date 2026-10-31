const ALEX_CAVES_DIMENSION_KEYS = [
  'alex_caves_dimensions:abyssal_chasm_key',
  'alex_caves_dimensions:candy_cavity_key',
  'alex_caves_dimensions:forlorn_hollows_key',
  'alex_caves_dimensions:magnetic_caves_key',
  'alex_caves_dimensions:primordial_caves_key',
  'alex_caves_dimensions:toxic_caves_key'
]

ServerEvents.recipes(event => {
  event.remove({ type: 'alex_caves_dimensions:cave_key' })
  ALEX_CAVES_DIMENSION_KEYS.forEach(id => event.remove({ output: id }))
})

ItemEvents.rightClicked(event => {
  if (ALEX_CAVES_DIMENSION_KEYS.includes(event.item.id)) {
    event.cancel()
  }
})
