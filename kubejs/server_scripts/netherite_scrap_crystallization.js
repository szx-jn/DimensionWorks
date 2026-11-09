// 下界合金碎片：化学冶金制备无特质混合金属，再由高压结晶赋予特质。
// Mekanism 10.4 原生提供硫酸、二氧化硫的液态形态及 Rotary Condensentrator 转换配方；
// 两种液体直接由 Mekanism 管道输入 Create 工作盆，不注册重复流体或配方。

ServerEvents.recipes(event => {
  event.recipes.create.mixing(
    'kubejs:mixed_metal',
    [
      Fluid.of('mekanism:sulfuric_acid', 250),
      Fluid.of('mekanism:sulfur_dioxide', 250),
      'minecraft:iron_ingot',
      'minecraft:gold_ingot',
      'minecraft:copper_ingot',
      '#forge:ingots/enderium',
      'create:sturdy_sheet'
    ]
  ).heated().id('dimensionworks:netherite_scrap/mixed_metal')

  const crystal = {
  'type': 'forge:partial_nbt',
  'item': 'dimensionworks_pressure_crystals:high_pressure_crystal',
  'nbt': {
    'DimensionWorks': {
      'Metal': 'netherite'
    }
  }
}
  const transitional = {
  'item': 'kubejs:incomplete_netherite_scrap',
  'nbt': {
    'DimensionWorks': {
      'Metal': 'netherite'
    }
  }
}
  const incompleteIngredient = {
  'type': 'forge:partial_nbt',
  'item': 'kubejs:incomplete_netherite_scrap',
  'nbt': {
    'DimensionWorks': {
      'Metal': 'netherite'
    }
  }
}

  event.custom(JsonIO.of({
    type: 'create:sequenced_assembly',
    ingredient: { item: 'kubejs:mixed_metal' },
    transitionalItem: transitional,
    loops: 1,
    sequence: [
      {
        type: 'create:deploying',
        ingredients: [incompleteIngredient, crystal],
        results: [transitional]
      },
      {
        type: 'create:pressing',
        ingredients: [incompleteIngredient],
        results: [transitional]
      }
    ],
    results: [{ item: 'minecraft:netherite_scrap', count: 2 }]
  })).id('dimensionworks:netherite_scrap/from_mixed_metal')

  console.info('[DimensionWorks] netherite scrap line: mixed metal + netherite high-pressure crystal -> 2 scraps')
})
