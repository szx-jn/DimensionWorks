// Copper oxide keeps minecraft:raw_copper and its normal smelting recipes.
ServerEvents.recipes(event => {
  // Replace native copper-clump purification with the veridium recovery step.
  event.remove({ type: 'mekanism:purifying', output: 'mekanism:clump_copper' })

  // CCB gas recipes keep the reactant and gas byproduct in one transaction.
  event.custom({
    type: 'createcraftedbeginning:reactor_kettle',
    gas_ingredients: [{
      amount: 250,
      ingredient: { gas: 'dimensionworks:carbon_monoxide' }
    }],
    ingredients: [{ item: 'minecraft:raw_copper', count: 1 }],
    results: [{ id: 'minecraft:copper_ingot', count: 1 }],
    gas_results: [{ id: 'dimensionworks:carbon_dioxide', amount: 250 }],
    temperature: 'heated',
    temperature_matching: 'exact'
  }).id('dimensionworks:copper_processing/reduction_with_carbon_monoxide')

  // Chance outputs roll independently per dust: expected yield = 1 + 2*.8 + .6 = 3.2.
  DimensionWorksGrinding.register(event, {
    id: 'dimensionworks:copper_processing/dust_from_ingot',
    machine: 'create:crushing',
    input: 'minecraft:copper_ingot',
    outputs: [
      { item: 'mekanism:dust_copper', count: 1 },
      { item: 'mekanism:dust_copper', count: 2, chance: 0.8 },
      { item: 'mekanism:dust_copper', count: 1, chance: 0.6 }
    ]
  })

  // Repeated entries make the basin really consume four dust, rather than
  // relying on ingredient-stack counts unsupported by Create processing.
  event.recipes.create.mixing('2x create:veridium', [
    'mekanism:dust_copper',
    'mekanism:dust_copper',
    'mekanism:dust_copper',
    'mekanism:dust_copper',
    'minecraft:cobblestone'
  ]).heated().id('dimensionworks:copper_processing/veridium_from_dust')

  // Copper oxide recovery consumes the CCB chemistry oxygen gas.
  event.custom({
    type: 'createcraftedbeginning:gas_injection',
    gas_ingredients: [{
      amount: 1,
      ingredient: { gas: 'dimensionworks:oxygen' }
    }],
    ingredients: [{ item: 'create:veridium', count: 1 }],
    results: [{ id: 'minecraft:raw_copper', count: 2 }]
  }).id('dimensionworks:copper_processing/oxide_from_veridium')
})
