// DimensionWorks：金属锭高温熔融配方。
// Create 6.0.8 + KubeJS Create。
// Create 原生只区分 HEATED / SUPERHEATED 两档热需求；这里按材料熔点分档。
// 每个锭熔成 90 mB，沿用常见的 9 锭 = 810 mB 方块换算比例。

const DW_MOLTEN_INGOT_AMOUNT = 90

const DW_MOLTEN_INGOT_RECIPES = [
  // 普通加热：低熔点金属与常规铜合金
  { input: 'minecraft:copper_ingot', fluid: 'dimensionworks:molten_copper', heat: 'heated' },
  { input: 'minecraft:gold_ingot', fluid: 'dimensionworks:molten_gold', heat: 'heated' },
  { input: 'create:zinc_ingot', fluid: 'dimensionworks:molten_zinc', heat: 'heated' },
  { input: 'create:brass_ingot', fluid: 'dimensionworks:molten_brass', heat: 'heated' },
  { input: 'mekanism:ingot_tin', fluid: 'dimensionworks:molten_tin', heat: 'heated' },
  { input: 'mekanism:ingot_lead', fluid: 'dimensionworks:molten_lead', heat: 'heated' },
  { input: 'mekanism:ingot_bronze', fluid: 'dimensionworks:molten_bronze', heat: 'heated' },
  { input: 'mekanism:ingot_uranium', fluid: 'dimensionworks:molten_uranium', heat: 'heated' },

  // 普通加热：铁、锇与钢在本整合包采用普通加热熔融
  { input: 'minecraft:iron_ingot', fluid: 'dimensionworks:molten_iron', heat: 'heated' },
  { input: 'mekanism:ingot_osmium', fluid: 'dimensionworks:molten_osmium', heat: 'heated' },
  { input: 'mekanism:ingot_steel', fluid: 'dimensionworks:molten_steel', heat: 'heated' },

  // 超级加热：下界合金、强化材料与特殊金属
  { input: 'minecraft:netherite_ingot', fluid: 'dimensionworks:molten_netherite', heat: 'superheated' },
  { input: 'mekanism:ingot_refined_obsidian', fluid: 'dimensionworks:molten_refined_obsidian', heat: 'superheated' },
  { input: 'mekanism:ingot_refined_glowstone', fluid: 'dimensionworks:molten_refined_glowstone', heat: 'superheated' },
  { input: 'alexscaves:scarlet_neodymium_ingot', fluid: 'dimensionworks:molten_scarlet_neodymium', heat: 'superheated' },
  { input: 'alexscaves:azure_neodymium_ingot', fluid: 'dimensionworks:molten_azure_neodymium', heat: 'superheated' },
  { input: 'l2complements:totemic_gold_ingot', fluid: 'dimensionworks:molten_totemic_gold', heat: 'superheated' },
  { input: 'l2complements:poseidite_ingot', fluid: 'dimensionworks:molten_poseidite', heat: 'superheated' },
  { input: 'l2complements:shulkerate_ingot', fluid: 'dimensionworks:molten_shulkerate', heat: 'superheated' },
  { input: 'l2complements:sculkium_ingot', fluid: 'dimensionworks:molten_sculkium', heat: 'superheated' },
  { input: 'l2complements:eternium_ingot', fluid: 'dimensionworks:molten_eternium', heat: 'superheated' }
]

ServerEvents.recipes(event => {
  DW_MOLTEN_INGOT_RECIPES.forEach((entry, index) => {
    let recipe = event.recipes.create.mixing(
      Fluid.of(entry.fluid, DW_MOLTEN_INGOT_AMOUNT),
      [entry.input]
    )

    recipe = entry.heat === 'superheated' ? recipe.superheated() : recipe.heated()
    recipe.id('dimensionworks:molten_metals/' + entry.fluid.split(':')[1] + '_from_ingot')
  })

  // 按 Create 6.0.8 原配方 1 铜锭 + 1 锌锭 -> 2 黄铜锭等量迁移。
  event.recipes.create.mixing(
    Fluid.of('dimensionworks:molten_brass', 2 * DW_MOLTEN_INGOT_AMOUNT),
    [
      Fluid.of('dimensionworks:molten_copper', DW_MOLTEN_INGOT_AMOUNT),
      Fluid.of('dimensionworks:molten_zinc', DW_MOLTEN_INGOT_AMOUNT)
    ]
  ).heated().id('dimensionworks:molten_metals/brass_from_molten_metals')

  // Mekanism 原配方为 3 铜锭 + 10 mB 锡化学品 -> 4 青铜锭；
  // 熔融配方将锡化学品投入改为 1 个等锭量熔融锡，保持 3:1 金属配比及总量。
  event.recipes.create.mixing(
    Fluid.of('dimensionworks:molten_bronze', 4 * DW_MOLTEN_INGOT_AMOUNT),
    [
      Fluid.of('dimensionworks:molten_copper', 3 * DW_MOLTEN_INGOT_AMOUNT),
      Fluid.of('dimensionworks:molten_tin', DW_MOLTEN_INGOT_AMOUNT)
    ]
  ).heated().id('dimensionworks:molten_metals/bronze_from_molten_metals')

  // 冷却配方同样使用 Create mixing：必须由机械动力搅拌器在工作盆上执行，
  // 工作盆用于承接流体输入与锭输出；不要求燃烧器热量，90 mB 对应一锭。
  DW_MOLTEN_INGOT_RECIPES.forEach(entry => {
    event.recipes.create.mixing(
      entry.input,
      [Fluid.of(entry.fluid, DW_MOLTEN_INGOT_AMOUNT)]
    ).id('dimensionworks:molten_metals/' + entry.fluid.split(':')[1] + '_cooled_to_ingot')
  })

  console.info('[DimensionWorks] molten ingot recipes registered=' + DW_MOLTEN_INGOT_RECIPES.length)
  console.info('[DimensionWorks] molten cooling recipes registered=' + DW_MOLTEN_INGOT_RECIPES.length)
})
