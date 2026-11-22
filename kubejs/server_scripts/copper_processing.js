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

  // Chance outputs roll independently per ingot: expected yield is 1.5 dust.
  DimensionWorksGrinding.register(event, {
    id: 'dimensionworks:copper_processing/dust_from_ingot',
    machine: 'create:crushing',
    input: 'minecraft:copper_ingot',
    outputs: [
      { item: 'mekanism:dust_copper', count: 1 },
      { item: 'mekanism:dust_copper', count: 1, chance: 0.5 }
    ]
  })

  // Four dust now yields one Veridium, preventing the copper loop from multiplying dust.
  event.recipes.create.mixing('create:veridium', [
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
      amount: 8000,
      ingredient: { gas: 'dimensionworks:oxygen' },
      minimum_pressure: 200000
    }],
    ingredients: [{ item: 'create:veridium', count: 1 }],
    results: [{ id: 'minecraft:raw_copper', count: 2 }]
  }).id('dimensionworks:copper_processing/oxide_from_veridium')
})
