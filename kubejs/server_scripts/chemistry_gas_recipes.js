// DimensionWorks：高中化学核心气体的 Mekanism 获取链
// 所有配方使用当前固定版本 Mekanism 10.4.16.80（1.20.1）的 camelCase JSON 字段。
// 新气体由 startup_scripts/chemistry_gases.js 注册。

ServerEvents.recipes(event => {
  // Create 工作盆通过内部标记物品输出 Mekanism 气体；标记会被 Java 集成转换为气体缓冲。
  // 无盖或开盖时每 20 游戏刻固定流失 200 mB 气体；柴油动力工作盆盖关闭时不流失，液体规则不变。
  // Create 的 heated 也匹配超级加热；标记限制确保普通加热的一氧化碳路线不会抢占二氧化碳路线。
  event.recipes.create.mixing(
    Item.of('dimensionworks_mek_stress:gas_output', {
      Gas: 'dimensionworks:carbon_monoxide', Amount: 125, ExcludeSuperheated: true
    }),
    ['minecraft:charcoal']
  ).heated().id('dimensionworks:chemistry/create/carbon_monoxide_from_charcoal')

  event.recipes.create.mixing(
    Item.of('dimensionworks_mek_stress:gas_output', { Gas: 'dimensionworks:carbon_dioxide', Amount: 250 }),
    ['minecraft:charcoal']
  ).superheated().id('dimensionworks:chemistry/create/carbon_dioxide_from_charcoal')

  const item = id => ({ ingredient: { item: id } })
  const gas = (id, amount) => ({ gas: id, amount: amount })
  const fluid = (id, amount) => ({ fluid: id, amount: amount })

  // 氮气：以火药等含氮原料作为分离工艺的游戏化入口。
  event.custom({
    type: 'mekanism:oxidizing',
    input: item('minecraft:gunpowder'),
    output: gas('dimensionworks:nitrogen', 1000)
  }).id('dimensionworks:chemistry/nitrogen_from_gunpowder')

  // 哈伯法：N₂ + 3H₂ → 2NH₃。Mekanism 气体量按比例设置。
  event.custom({
    type: 'mekanism:chemical_infusing',
    leftInput: gas('dimensionworks:nitrogen', 1000),
    rightInput: gas('mekanism:hydrogen', 3000),
    output: gas('dimensionworks:ammonia', 2000)
  }).id('dimensionworks:chemistry/ammonia_haber_process')

  // 氮的氧化物：模拟高温氧化及后续氧化链。
  event.custom({
    type: 'mekanism:chemical_infusing',
    leftInput: gas('dimensionworks:nitrogen', 1000),
    rightInput: gas('mekanism:oxygen', 1000),
    output: gas('dimensionworks:nitric_oxide', 2000)
  }).id('dimensionworks:chemistry/nitric_oxide_from_nitrogen')

  event.custom({
    type: 'mekanism:chemical_infusing',
    leftInput: gas('dimensionworks:nitric_oxide', 1000),
    rightInput: gas('mekanism:oxygen', 500),
    output: gas('dimensionworks:nitrogen_dioxide', 1000)
  }).id('dimensionworks:chemistry/nitrogen_dioxide_from_nitric_oxide')

  event.custom({
    type: 'mekanism:chemical_infusing',
    leftInput: gas('dimensionworks:ammonia', 1000),
    rightInput: gas('mekanism:oxygen', 1000),
    output: gas('dimensionworks:nitrous_oxide', 500)
  }).id('dimensionworks:chemistry/nitrous_oxide_from_ammonia')

  // 硫化氢：硫粉、氢气和水在加压反应室中处理。
  event.custom({
    type: 'mekanism:reaction',
    itemInput: { ingredient: { tag: 'forge:dusts/sulfur' } },
    fluidInput: fluid('minecraft:water', 1000),
    gasInput: gas('mekanism:hydrogen', 1000),
    energyRequired: 10000,
    duration: 100,
    gasOutput: gas('dimensionworks:hydrogen_sulfide', 1000)
  }).id('dimensionworks:chemistry/hydrogen_sulfide_prc')

  // 甲烷：煤质原料的加氢处理，作为有机化学入门原料链。
  event.custom({
    type: 'mekanism:reaction',
    itemInput: item('minecraft:coal'),
    fluidInput: fluid('minecraft:water', 1000),
    gasInput: gas('mekanism:hydrogen', 2000),
    energyRequired: 20000,
    duration: 160,
    gasOutput: gas('dimensionworks:methane', 1000)
  }).id('dimensionworks:chemistry/methane_prc')

  // 乙炔：碳质原料的高温加氢处理，作为不饱和烃生产入口。
  event.custom({
    type: 'mekanism:reaction',
    itemInput: item('minecraft:charcoal'),
    fluidInput: fluid('minecraft:water', 1000),
    gasInput: gas('mekanism:hydrogen', 1000),
    energyRequired: 20000,
    duration: 160,
    gasOutput: gas('dimensionworks:ethyne', 500)
  }).id('dimensionworks:chemistry/ethyne_prc')

  // 臭氧：太阳中子活化器将氧气转化为臭氧。
  event.custom({
    type: 'mekanism:activating',
    input: gas('mekanism:oxygen', 1000),
    output: gas('dimensionworks:ozone', 500)
  }).id('dimensionworks:chemistry/ozone_activation')

  // 硝酸：加压反应室以二氧化氮、水和氧化剂构建工业化制酸步骤。
  event.custom({
    type: 'mekanism:reaction',
    itemInput: item('minecraft:iron_ingot'),
    fluidInput: fluid('minecraft:water', 1000),
    gasInput: gas('dimensionworks:nitrogen_dioxide', 1000),
    energyRequired: 20000,
    duration: 200,
    gasOutput: gas('dimensionworks:nitric_acid', 1000)
  }).id('dimensionworks:chemistry/nitric_acid_prc')
})
