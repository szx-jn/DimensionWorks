// Sulfur supply from sulfur-bearing volcanic material (a pack-world abstraction).
ServerEvents.recipes(event => {
  // Remove every native coal/charcoal form, including dusts and storage blocks.
  // Preserve gunpowder extraction and other sulfur chemistry.
  const nativeCoalForms = ['coals', 'dusts_coals', 'blocks_coals']
  nativeCoalForms.forEach(path => {
    event.remove({ id: 'mekanism:reaction/coal_gasification/' + path })
  })

  // Create consumes one item per ingredient entry; four entries are required.
  event.recipes.create.mixing('kubejs:crude_sulfur', [
    'create:cinder_flour',
    'create:cinder_flour',
    'create:cinder_flour',
    'create:cinder_flour'
  ]).heated().id('dimensionworks:sulfur_processing/thermal_separation')

  global.DimensionWorksGrinding.register(event, {
    id: 'dimensionworks:sulfur_processing/dust_from_crude_sulfur',
    machine: 'create:milling',
    input: 'kubejs:crude_sulfur',
    outputs: [{ item: 'mekanism:dust_sulfur', count: 1 }]
  })
})
