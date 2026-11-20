// DimensionWorks: make redstone a mandatory cost for Create's blaze-cake fuel chain.
// Replace every recipe producing the base so addon recipes cannot bypass the redstone cost.
// The vanilla Create filling recipe (blaze_cake_base + 250 mB lava -> blaze_cake) is preserved.

ServerEvents.recipes(event => {
  event.remove({ output: 'create:blaze_cake_base' })

  event.recipes.create.compacting(
    'create:blaze_cake_base',
    [
      '#c:eggs',
      'minecraft:sugar',
      'create:cinder_flour',
      'minecraft:redstone'
    ]
  ).id('dimensionworks:blaze_cake_base_with_redstone')

  console.info('[DimensionWorks] Blaze Cake Base recipe updated: requires 1 redstone dust; Create lava filling remains unchanged.')
})
