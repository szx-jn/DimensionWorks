// 暮色弹力封带：黏液胶料、钢叶带坯和两种铁木弹簧汇合，最终升级保留旧工具完整数据。
ServerEvents.recipes(event => {
  event.recipes.create.mixing('2x kubejs:elastic_compound', [
    '4x minecraft:slime_ball', 'twilightforest:fiery_blood', Fluid.of('create:honey', 250)
  ]).heated().id('dimensionworks:elastic/compound')

  event.recipes.create.cutting('2x kubejs:steeleaf_seal_strip', 'twilightforest:steeleaf_ingot')
    .id('dimensionworks:elastic/seal_strip')

  const workpiece = { item: 'kubejs:incomplete_bounce_tape' }
  const deploy = item => ({
    type: 'create:deploying', ingredients: [workpiece, { item: item }], results: [workpiece]
  })
  const press = () => ({ type: 'create:pressing', ingredients: [workpiece], results: [workpiece] })
  event.custom({
    type: 'create:sequenced_assembly',
    ingredient: { item: 'kubejs:elastic_compound' },
    transitionalItem: workpiece,
    sequence: [
      deploy('kubejs:steeleaf_seal_strip'),
      press(),
      deploy('vintageimprovements:ironwood_spring'),
      deploy('vintageimprovements:small_ironwood_spring'),
      deploy('twilightforest:arctic_fur'),
      { type: 'create:filling', ingredients: [workpiece, { fluid: 'create:honey', amount: 100 }], results: [workpiece] },
      press(),
      deploy('twilightforest:steeleaf_ingot'),
      press(),
      { type: 'create:cutting', ingredients: [workpiece], results: [workpiece], processingTime: 80 }
    ],
    loops: 1,
    results: [{ item: 'kubejs:bounce_tape', count: 1 }]
  }).id('dimensionworks:elastic/bounce_tape')

  const ElasticPackager = Java.loadClass('dev.szx.dimensionworks.transfer.elastic.ElasticPackager')
  event.shapeless('dimensionworks_transfer:elastic_packager', [
    'create_mobile_packages:mobile_packager', 'kubejs:bounce_tape'
  ]).modifyResult((grid, result) => {
    return ElasticPackager.upgrade(grid.find('create_mobile_packages:mobile_packager'))
  }).id('dimensionworks:elastic/upgrade_packager')
})
