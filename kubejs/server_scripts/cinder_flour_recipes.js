// Redstone and Netherrack now share a Cinder Flour production chain.
ServerEvents.recipes(event => {
  // Replace Create's native Netherrack crushing recipe with the DimensionWorks yield.
  event.remove({ id: 'create:crushing/netherrack' })
  global.DimensionWorksGrinding.register(event, {
    id: 'dimensionworks:cinder_flour/netherrack_crushing',
    machine: 'create:crushing',
    input: 'minecraft:netherrack',
    outputs: [
      { item: 'create:cinder_flour', count: 1 },
      { item: 'create:cinder_flour', count: 2, chance: 0.85 },
      { item: 'create:cinder_flour', count: 2, chance: 0.80 }
    ]
  })

  event.recipes.create.compacting(
    'minecraft:netherrack',
    ['4x create:cinder_flour']
  ).id('dimensionworks:cinder_flour/netherrack_compacting')

  event.recipes.create.filling(
    'minecraft:redstone',
    [
      'create:cinder_flour',
      Fluid.of('createdieselgenerators:crude_oil', 250)
    ]
  ).id('dimensionworks:cinder_flour/redstone_from_crude_oil')

  console.info('[DimensionWorks] Cinder Flour chain registered: Netherrack crushing/compacting and crude-oil redstone filling.')
})
