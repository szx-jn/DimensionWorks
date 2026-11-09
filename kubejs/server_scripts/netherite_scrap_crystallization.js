// Six-stage netherite scrap production using high-pressure metal crystals.
// Each recipe is locked to one crystal metal trait; crystals from different metals cannot mix.
const DW_NETHERITE_CRYSTAL_METALS = [
  'iron', 'copper', 'gold', 'netherite', 'zinc', 'brass', 'tin', 'lead',
  'osmium', 'uranium', 'bronze', 'steel', 'refined_obsidian',
  'refined_glowstone', 'scarlet_neodymium', 'azure_neodymium',
  'totemic_gold', 'poseidite', 'shulkerate', 'sculkium', 'eternium'
]

ServerEvents.recipes(event => {
  const crystalIngredient = metal => ({
    type: 'forge:partial_nbt',
    item: 'dimensionworks_pressure_crystals:high_pressure_crystal',
    nbt: { DimensionWorks: { Metal: metal } }
  })
  const incompleteIngredient = metal => ({
    type: 'forge:partial_nbt',
    item: 'kubejs:incomplete_netherite_scrap',
    nbt: { DimensionWorks: { Metal: metal } }
  })

  DW_NETHERITE_CRYSTAL_METALS.forEach(metal => {
    const incomplete = {
      item: 'kubejs:incomplete_netherite_scrap',
      nbt: { DimensionWorks: { Metal: metal } }
    }

    event.custom(JsonIO.of({
      type: 'create:sequenced_assembly',
      ingredient: crystalIngredient(metal),
      transitionalItem: incomplete,
      loops: 1,
      sequence: [
        {
          type: 'create:deploying',
          ingredients: [incompleteIngredient(metal), crystalIngredient(metal)],
          results: [incomplete]
        },
        {
          type: 'create:deploying',
          ingredients: [incompleteIngredient(metal), { item: 'mekanism:alloy_atomic' }],
          results: [incomplete]
        },
        {
          type: 'create:filling',
          ingredients: [incompleteIngredient(metal), { fluid: 'dimensionworks:molten_netherite', amount: 500 }],
          results: [incomplete]
        },
        {
          type: 'create:deploying',
          ingredients: [incompleteIngredient(metal), { item: 'ae2:engineering_processor' }],
          results: [incomplete]
        },
        {
          type: 'create:deploying',
          ingredients: [incompleteIngredient(metal), { item: 'minecraft:echo_shard' }],
          results: [incomplete]
        },
        {
          type: 'create:pressing',
          ingredients: [incompleteIngredient(metal)],
          results: [incomplete]
        }
      ],
      results: [{ item: 'minecraft:netherite_scrap', count: 1 }]
    })).id('dimensionworks:netherite_scrap/from_' + metal + '_high_pressure_crystals')
  })

  console.info('[DimensionWorks] registered ' + DW_NETHERITE_CRYSTAL_METALS.length + ' trait-locked netherite scrap assembly recipes')
})
