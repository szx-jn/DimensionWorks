// DimensionWorks：通用熔融材料流体注册。
// Minecraft 1.20.1 Forge / KubeJS 6。
// 本文件只负责注册流体；熔炼、浇注与加工配方放在 server_scripts 中。

class MoltenMetalFluidDefinition {
  constructor(id, displayName, color) {
    this.id = id
    this.displayName = displayName
    this.color = color
  }

  register(event) {
    return event.create(this.id)
      .thickTexture(this.color)
      .bucketColor(this.color)
      .displayName(this.displayName)
  }
}

const DW_MOLTEN_METALS = [
  // 原版
  ['dimensionworks:molten_iron', '熔融铁', 0xD77A42],
  ['dimensionworks:molten_copper', '熔融铜', 0xE07845],
  ['dimensionworks:molten_gold', '熔融金', 0xFFD34D],
  ['dimensionworks:molten_netherite', '熔融下界合金', 0x51454A],

  // Create
  ['dimensionworks:molten_zinc', '熔融锌', 0xB7C6C7],
  ['dimensionworks:molten_brass', '熔融黄铜', 0xE5BE48],

  // Mekanism
  ['dimensionworks:molten_tin', '熔融锡', 0xC7D8DE],
  ['dimensionworks:molten_lead', '熔融铅', 0x777F91],
  ['dimensionworks:molten_osmium', '熔融锇', 0xA6D9D1],
  ['dimensionworks:molten_uranium', '熔融铀', 0x8DC65A],
  ['dimensionworks:molten_bronze', '熔融青铜', 0xB97943],
  ['dimensionworks:molten_steel', '熔融钢', 0xAAB3BF],
  ['dimensionworks:molten_refined_obsidian', '熔融强化黑曜石', 0x51446D],
  ['dimensionworks:molten_refined_glowstone', '熔融强化荧石', 0xF6D66B],

  // Alex's Caves
  ['dimensionworks:molten_scarlet_neodymium', '熔融赤钕合金', 0xC94B55],
  ['dimensionworks:molten_azure_neodymium', '熔融青钕合金', 0x4B83D1],

  // L2 Complements
  ['dimensionworks:molten_totemic_gold', '熔融图腾金', 0xE9C85A],
  ['dimensionworks:molten_poseidite', '熔融海神石', 0x4C9CC4],
  ['dimensionworks:molten_shulkerate', '熔融潜影合金', 0xA47CC2],
  ['dimensionworks:molten_sculkium', '熔融幽匿金属', 0x397B78],
  ['dimensionworks:molten_eternium', '熔融永恒金属', 0xE6E1D0]
]

StartupEvents.registry('fluid', event => {
  DW_MOLTEN_METALS.forEach(([id, displayName, color]) => {
    new MoltenMetalFluidDefinition(id, displayName, color).register(event)
  })
})
