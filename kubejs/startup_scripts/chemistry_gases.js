// DimensionWorks：高中化学核心物质的 Mekanism 化学品扩展
// Minecraft 1.21.1 NeoForge / Mekanism 10.7 / KubeJS 7
// 只注册 Mekanism 原版未提供的化学品，避免覆盖原版 ID。

StartupEvents.registry('mekanism:chemical', event => {
  const gases = [
    // ID、颜色。颜色仅用于区分化学品，不代表真实颜色或物态。
    ['dimensionworks:carbon_dioxide', '#9AA5B1'],
    ['dimensionworks:carbon_monoxide', '#7D8791'],
    ['dimensionworks:nitrogen', '#9EB9D8'],
    ['dimensionworks:ammonia', '#B8E0E8'],
    ['dimensionworks:nitric_oxide', '#A5B8C8'],
    ['dimensionworks:nitrogen_dioxide', '#B86A35'],
    ['dimensionworks:nitrous_oxide', '#B8C7D5'],
    ['dimensionworks:hydrogen_sulfide', '#D5D56B'],
    ['dimensionworks:methane', '#B6C8A0'],
    ['dimensionworks:ethyne', '#C6B5A5'],
    ['dimensionworks:ozone', '#8A9CFF'],
    ['dimensionworks:nitric_acid', '#D8C5A8']
  ]

  gases.forEach(([id, tint]) => {
    event.create(id).tint(tint).gaseous()
  })
})
