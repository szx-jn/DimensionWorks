// DimensionWorks: keep Draconic Evolution as a reactor-only progression.
// Target: Draconic Evolution 1.20.1 / 3.1.2.621 (source branch 1.20).
// KubeJS cannot unregister items or disable worldgen; this restricts recipes and JEI separately.
const DW_DE_KEEP = [
  'draconium_dust',
  'draconium_ingot',
  'awakened_draconium_ingot',
  'awakened_draconium_block',
  'awakened_draconium_nugget',
  'large_chaos_frag',
  'medium_chaos_frag',
  'small_chaos_frag',
  'reactor_core',
  'reactor_stabilizer',
  'reactor_injector'
]

ServerEvents.recipes(event => {
  // Remove all native recipes, including DE's custom fusion crafting recipes.
  event.remove({ mod: 'draconicevolution' })

  // Also remove cross-mod recipes producing DE gear and machines.
  // Replace the six approved outputs below with a compact reactor-only chain.
  Ingredient.of('@draconicevolution').itemIds.forEach(id => {
    if (!DW_DE_KEEP.includes(String(id).replace('draconicevolution:', ''))) {
      event.remove({ output: id })
    }
  })

  // Ore and dust remain usable as reactor precursor materials.
  event.smelting('draconicevolution:draconium_ingot', 'draconicevolution:draconium_dust')
    .id('dimensionworks:reactor/draconium_dust_smelting')
  event.smelting('draconicevolution:draconium_ingot', 'draconicevolution:overworld_draconium_ore')
    .id('dimensionworks:reactor/draconium_ore_smelting')
  event.smelting('draconicevolution:draconium_ingot', 'draconicevolution:deepslate_draconium_ore')
    .id('dimensionworks:reactor/deepslate_draconium_ore_smelting')
  event.smelting('draconicevolution:draconium_ingot', 'draconicevolution:nether_draconium_ore')
    .id('dimensionworks:reactor/nether_draconium_ore_smelting')
  event.smelting('draconicevolution:draconium_ingot', 'draconicevolution:end_draconium_ore')
    .id('dimensionworks:reactor/end_draconium_ore_smelting')

  // Fuel can be inserted/withdrawn in block, ingot and nugget units.
  event.shaped('draconicevolution:awakened_draconium_block', [
    'AAA', 'AAA', 'AAA'
  ], { A: 'draconicevolution:awakened_draconium_ingot' })
    .id('dimensionworks:reactor/awakened_draconium_block')
  event.shapeless('9x draconicevolution:awakened_draconium_ingot',
    ['draconicevolution:awakened_draconium_block'])
    .id('dimensionworks:reactor/awakened_draconium_block_unpack')
  event.shapeless('9x draconicevolution:awakened_draconium_nugget',
    ['draconicevolution:awakened_draconium_ingot'])
    .id('dimensionworks:reactor/awakened_draconium_nugget_unpack')
  event.shaped('draconicevolution:awakened_draconium_ingot', [
    'AAA', 'AAA', 'AAA'
  ], { A: 'draconicevolution:awakened_draconium_nugget' })
    .id('dimensionworks:reactor/awakened_draconium_nugget_pack')

  // Spent fuel is returned by the reactor as large/medium/small chaos fragments.
  event.shapeless('9x draconicevolution:medium_chaos_frag',
    ['draconicevolution:large_chaos_frag'])
    .id('dimensionworks:reactor/large_chaos_fragment_unpack')
  event.shapeless('9x draconicevolution:small_chaos_frag',
    ['draconicevolution:medium_chaos_frag'])
    .id('dimensionworks:reactor/medium_chaos_fragment_unpack')
  event.shaped('draconicevolution:large_chaos_frag', [
    'AAA', 'AAA', 'AAA'
  ], { A: 'draconicevolution:medium_chaos_frag' })
    .id('dimensionworks:reactor/large_chaos_fragment_pack')
  event.shaped('draconicevolution:medium_chaos_frag', [
    'AAA', 'AAA', 'AAA'
  ], { A: 'draconicevolution:small_chaos_frag' })
    .id('dimensionworks:reactor/medium_chaos_fragment_pack')

  // Avoid the FE-powered fusion crafting chain entirely.
  event.shaped('draconicevolution:awakened_draconium_ingot', [
    'DND',
    'NSN',
    'DND'
  ], {
    D: 'draconicevolution:draconium_ingot',
    N: 'minecraft:netherite_ingot',
    S: 'minecraft:nether_star'
  }).id('dimensionworks:reactor/awakened_draconium_ingot')

  event.shaped('draconicevolution:reactor_core', [
    'ANA',
    'NSN',
    'ANA'
  ], {
    A: 'draconicevolution:awakened_draconium_ingot',
    N: 'minecraft:netherite_block',
    S: 'minecraft:nether_star'
  }).id('dimensionworks:reactor/core')

  event.shaped('draconicevolution:reactor_stabilizer', [
    'ANA',
    'NEN',
    'ANA'
  ], {
    A: 'draconicevolution:awakened_draconium_ingot',
    N: 'minecraft:netherite_ingot',
    E: 'minecraft:ender_eye'
  }).id('dimensionworks:reactor/stabilizer')

  event.shaped('draconicevolution:reactor_injector', [
    'ADA',
    'NEN',
    'ADA'
  ], {
    A: 'draconicevolution:awakened_draconium_ingot',
    D: 'draconicevolution:draconium_ingot',
    N: 'minecraft:netherite_ingot',
    E: 'minecraft:ender_eye'
  }).id('dimensionworks:reactor/injector')
})
