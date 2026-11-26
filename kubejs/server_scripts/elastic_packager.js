// 暮色弹力封带：黏液胶料、钢叶带坯和两种铁木弹簧汇合，最终升级保留旧工具完整数据。
ServerEvents.recipes(event => {
  event.recipes.create.mixing('2x kubejs:elastic_compound', [
    '4x minecraft:slime_ball', 'twilightforest:fiery_blood', Fluid.of('create:honey', 250)
  ]).heated().id('dimensionworks:elastic/compound')

  event.recipes.create.cutting('2x kubejs:steeleaf_seal_strip', 'twilightforest:steeleaf_ingot')
    .id('dimensionworks:elastic/seal_strip')

  const workpiece = { id: 'kubejs:incomplete_bounce_tape' }
  const ingredient = { item: 'kubejs:incomplete_bounce_tape' }
  const deploy = item => ({
    type: 'create:deploying', ingredients: [ingredient, { item: item }], results: [workpiece]
  })
  const press = () => ({ type: 'create:pressing', ingredients: [ingredient], results: [workpiece] })
  event.custom({
    type: 'create:sequenced_assembly',
    ingredient: { item: 'kubejs:elastic_compound' },
    transitional_item: workpiece,
    sequence: [
      deploy('kubejs:steeleaf_seal_strip'),
      press(),
      deploy('vintageimprovements:ironwood_spring'),
      deploy('vintageimprovements:small_ironwood_spring'),
      deploy('twilightforest:arctic_fur'),
      { type: 'create:filling', ingredients: [ingredient, { type: 'neoforge:single', fluid: 'create:honey', amount: 100 }], results: [workpiece] },
      press(),
      deploy('twilightforest:steeleaf_ingot'),
      press(),
      { type: 'create:cutting', ingredients: [ingredient], results: [workpiece], processing_time: 80 }
    ],
    loops: 1,
    results: [{ id: 'kubejs:bounce_tape', count: 1 }]
  }).id('dimensionworks:elastic/bounce_tape')

  event.shapeless('dimensionworks_transfer:elastic_packager', [
    'create_mobile_packages:mobile_packager', 'kubejs:bounce_tape'
  ]).modifyResult('dimensionworks:elastic_packager_upgrade').id('dimensionworks:elastic/upgrade_packager')

  // Magnetic Caves materials form the opposing-polarity clamp for ordinary block packing.
  event.shapeless('dimensionworks_transfer:block_packager', [
    'dimensionworks_transfer:elastic_packager',
    'alexscaves:energized_galena_neutral',
    'alexscaves:raw_scarlet_neodymium',
    'alexscaves:raw_scarlet_neodymium',
    'alexscaves:raw_azure_neodymium',
    'alexscaves:raw_azure_neodymium',
    'create:sturdy_sheet'
  ]).modifyResult('dimensionworks:block_packager_upgrade').id('dimensionworks:elastic/upgrade_block_packager')

  // Purple requires one signature material from each of the five remaining caves.
  event.shapeless('dimensionworks_transfer:advanced_packager', [
    'dimensionworks_transfer:block_packager',
    'alexscaves:pewen_sap',
    'alexscaves:uranium',
    'alexscaves:pearl',
    'alexscaves:pure_darkness',
    'alexscaves:sweet_tooth',
    'create:precision_mechanism',
    'mekanism:alloy_atomic'
  ]).modifyResult('dimensionworks:advanced_packager_upgrade').id('dimensionworks:elastic/upgrade_advanced_packager')
})

// 2101 的结果修饰器以固定名称注册，使用原生 CraftingInput 读取输入。
const ElasticPackager = Java.loadClass('dev.szx.dimensionworks.transfer.elastic.ElasticPackager')

function upgradePackagerResult(event, sourceId, upgrade) {
  for (let slot = 0; slot < event.grid.size(); slot++) {
    const stack = event.grid.getItem(slot)
    if (stack.id === sourceId) {
      event.exit(upgrade(stack))
      return
    }
  }
  throw new Error('打包工具升级配方缺少前置工具：' + sourceId)
}

ServerEvents.modifyRecipeResult('dimensionworks:elastic_packager_upgrade', event => {
  upgradePackagerResult(event, 'create_mobile_packages:mobile_packager', stack => ElasticPackager.upgrade(stack))
})

ServerEvents.modifyRecipeResult('dimensionworks:block_packager_upgrade', event => {
  upgradePackagerResult(event, 'dimensionworks_transfer:elastic_packager', stack => ElasticPackager.upgradeToBlue(stack))
})

ServerEvents.modifyRecipeResult('dimensionworks:advanced_packager_upgrade', event => {
  upgradePackagerResult(event, 'dimensionworks_transfer:block_packager', stack => ElasticPackager.upgradeToPurple(stack))
})
