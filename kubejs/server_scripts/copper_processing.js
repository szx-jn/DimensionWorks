// Copper oxide keeps minecraft:raw_copper and its normal smelting recipes.
ServerEvents.recipes(event => {
  // Replace native copper-clump purification with the veridium recovery step.
  event.remove({ type: 'mekanism:purifying', output: 'mekanism:clump_copper' })

  // Gas requirements are encoded on the gas output marker and checked by the
  // basin integration before any real item/gas consumption or output.
  event.recipes.create.mixing([
    'minecraft:copper_ingot',
    Item.of('dimensionworks_mek_stress:gas_output', {
      Gas: 'dimensionworks:carbon_dioxide',
      Amount: 250,
      InputGas: 'dimensionworks:carbon_monoxide',
      InputAmount: 250,
      ExcludeSuperheated: true,
      display: {
        Name: JSON.stringify({ text: '250 mB 二氧化碳', italic: false }),
        Lore: [JSON.stringify({ text: '消耗：250 mB 一氧化碳', italic: false })]
      }
    })
  ], ['minecraft:raw_copper'])
    .heated()
    .id('dimensionworks:copper_processing/reduction_with_carbon_monoxide')

  // Chance outputs roll independently per dust: expected yield = 1 + 2*.8 + .6 = 3.2.
  global.DimensionWorksGrinding.register(event, {
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

  // Oxygen uses Mekanism's native purification consumption semantics.
  event.custom({
    type: 'mekanism:purifying',
    itemInput: { ingredient: { item: 'create:veridium' } },
    chemicalInput: { gas: 'mekanism:oxygen', amount: 1 },
    output: { item: 'minecraft:raw_copper', count: 2 }
  }).id('dimensionworks:copper_processing/oxide_from_veridium')
})
