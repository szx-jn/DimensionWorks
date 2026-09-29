const SELF_PROPAGATION = 'dimensionworks_cave_factory:self_propagation/'

function selfRecipe(recipe, path) {
  return recipe.id(SELF_PROPAGATION + path)
}

function supportRecipe(recipe, path) {
  return recipe.id(SELF_PROPAGATION + 'support/' + path)
}

// Create processing machines consume one item per ingredient entry and ignore
// per-stack counts, so a literal "2x item" entry would only ever cost one item.
// Expanding the entries keeps the written ratio and the machine in sync with
// Create's own recipes, which repeat the same ingredient instead of counting it.
function expandIngredients(input) {
  if (!Array.isArray(input)) {
    return input
  }
  const expanded = []
  input.forEach(entry => {
    const match = typeof entry === 'string' ? /^(\d+)x (.+)$/.exec(entry) : null
    if (match) {
      for (let index = 0; index < Number(match[1]); index++) {
        expanded.push(match[2])
      }
    } else {
      expanded.push(entry)
    }
  })
  return expanded
}

function createRecipes(namespace) {
  return {
    mixing: (results, ingredients) => namespace.mixing(results, expandIngredients(ingredients)),
    compacting: (results, ingredients) => namespace.compacting(results, expandIngredients(ingredients)),
    crushing: (results, ingredients) => namespace.crushing(results, expandIngredients(ingredients)),
    milling: (results, ingredients) => namespace.milling(results, expandIngredients(ingredients)),
    filling: (results, ingredients) => namespace.filling(results, expandIngredients(ingredients)),
    haunting: (results, ingredients) => namespace.haunting(results, expandIngredients(ingredients))
  }
}

ServerEvents.recipes(event => {
  const create = createRecipes(event.recipes.create)

  // Magnetic Caves: keep the first ferrouslime and neodymium samples, then
  // rebuild the local ecosystem from ordinary metals, fluids and scrap.
  supportRecipe(create.compacting('2x alexscaves:galena', [
    'minecraft:deepslate',
    'minecraft:iron_nugget',
    'minecraft:flint'
  ]), 'magnetic/galena')
  supportRecipe(create.compacting('2x alexscaves:galena', [
    '2x dimensionworks_cave_factory:polarity_residue',
    'minecraft:iron_nugget'
  ]), 'magnetic/galena_recovery')

  selfRecipe(create.mixing([
    '2x alexscaves:ferrouslime_ball'
  ], [
    'minecraft:slime_ball',
    'minecraft:iron_nugget',
    Fluid.of('minecraft:water', 250)
  ]), 'magnetic/ferrouslime_primary')
  selfRecipe(create.mixing([
    'alexscaves:ferrouslime_ball'
  ], [
    '2x dimensionworks_cave_factory:polarity_residue',
    'minecraft:slime_ball'
  ]), 'magnetic/ferrouslime_recovery')

  selfRecipe(create.mixing([
    '2x alexscaves:raw_scarlet_neodymium',
    '2x alexscaves:raw_azure_neodymium'
  ], [
    'alexscaves:ferrouslime_ball',
    'alexscaves:scrap_metal_plate',
    'minecraft:redstone',
    'minecraft:lapis_lazuli'
  ]), 'magnetic/neodymium_primary')
  selfRecipe(create.mixing([
    'alexscaves:raw_scarlet_neodymium',
    'alexscaves:raw_azure_neodymium'
  ], [
    '2x dimensionworks_cave_factory:polarity_residue',
    'alexscaves:scrap_metal_plate'
  ]), 'magnetic/neodymium_recovery')

  selfRecipe(create.mixing([
    '2x alexscaves:energized_galena_neutral'
  ], [
    '4x alexscaves:galena',
    'alexscaves:raw_scarlet_neodymium',
    'alexscaves:raw_azure_neodymium',
    Fluid.of('minecraft:water', 500)
  ]), 'magnetic/energized_galena_primary')
  selfRecipe(create.mixing([
    'alexscaves:energized_galena_neutral'
  ], [
    '2x dimensionworks_cave_factory:polarity_residue',
    'alexscaves:galena'
  ]), 'magnetic/energized_galena_recovery')

  // Primordial Caves: limestone is mineralized locally, pewen trees provide
  // the wood loop, and sap plus lichen feeds a steady pines supply.
  selfRecipe(create.compacting('2x alexscaves:limestone', [
    'minecraft:calcite',
    'minecraft:bone_meal',
    Fluid.of('minecraft:water', 250)
  ]), 'primordial/limestone_primary')
  selfRecipe(create.compacting('alexscaves:limestone', [
    '2x dimensionworks_cave_factory:genesis_residue',
    'minecraft:calcite'
  ]), 'primordial/limestone_recovery')

  selfRecipe(create.mixing('2x alexscaves:pewen_sap', [
    '2x alexscaves:pewen_planks',
    Fluid.of('minecraft:water', 500)
  ]), 'primordial/pewen_sap_primary')
  selfRecipe(create.mixing('alexscaves:pewen_sap', [
    '2x dimensionworks_cave_factory:genesis_residue',
    'alexscaves:pewen_planks'
  ]), 'primordial/pewen_sap_recovery')

  // 星树果 is the Primordial ecologic base, so the tree farm feeds the star
  // fruit loop instead of another wood product.
  selfRecipe(create.mixing('2x alexscaves:tree_star', [
    'alexscaves:pewen_sap',
    'minecraft:glow_lichen',
    'minecraft:bone_meal'
  ]), 'primordial/tree_star_primary')
  selfRecipe(create.mixing('alexscaves:tree_star', [
    '2x dimensionworks_cave_factory:genesis_residue',
    'minecraft:glow_lichen'
  ]), 'primordial/tree_star_recovery')

  // Toxic Caves: acid is drawn from the native acid sea, then used to turn
  // reconstructed radrock into an endless uranium-shard feedstock.
  supportRecipe(create.compacting('2x alexscaves:radrock', [
    'minecraft:deepslate',
    'minecraft:basalt',
    'minecraft:iron_nugget'
  ]), 'toxic/radrock')
  supportRecipe(create.compacting('alexscaves:radrock', [
    '2x dimensionworks_cave_factory:decay_residue',
    'minecraft:basalt'
  ]), 'toxic/radrock_recovery')
  supportRecipe(create.mixing('2x create:crushed_raw_uranium', [
    'alexscaves:acidic_radrock',
    'alexscaves:sulfur_dust'
  ]), 'toxic/uranium_concentrate')

  selfRecipe(create.mixing('2x alexscaves:sulfur_dust', [
    'minecraft:gunpowder',
    'minecraft:basalt',
    Fluid.of('alexscaves:acid', 250)
  ]), 'toxic/sulfur_dust_primary')
  selfRecipe(create.mixing('alexscaves:sulfur_dust', [
    '2x dimensionworks_cave_factory:decay_residue',
    'minecraft:gunpowder'
  ]), 'toxic/sulfur_dust_recovery')

  selfRecipe(create.filling('alexscaves:acidic_radrock', [
    'alexscaves:radrock',
    Fluid.of('alexscaves:acid', 250)
  ]), 'toxic/acidic_radrock_primary')
  selfRecipe(create.compacting('alexscaves:acidic_radrock', [
    '2x dimensionworks_cave_factory:decay_residue',
    'alexscaves:radrock'
  ]), 'toxic/acidic_radrock_recovery')

  selfRecipe(create.crushing([
    '2x alexscaves:uranium_shard',
    Item.of('alexscaves:uranium_shard').withChance(0.5)
  ], 'create:crushed_raw_uranium'), 'toxic/uranium_shard_primary')
  selfRecipe(create.mixing('alexscaves:uranium_shard', [
    '2x dimensionworks_cave_factory:decay_residue',
    'alexscaves:acidic_radrock'
  ]), 'toxic/uranium_shard_recovery')

  // Abyssal Chasm: mussel plates are the local seed stock. A saw and arm
  // harvest them, while sea pigs remain the only required entity loop.
  supportRecipe(create.compacting('2x alexscaves:mussel', [
    'alexscaves:mussel',
    'alexscaves:muck'
  ]), 'abyssal/mussel_culture')

  selfRecipe(create.compacting('2x alexscaves:abyssmarine', [
    'alexscaves:mussel',
    'alexscaves:muck',
    'minecraft:prismarine_shard'
  ]), 'abyssal/abyssmarine_primary')
  selfRecipe(create.compacting('alexscaves:abyssmarine', [
    '2x dimensionworks_cave_factory:pressure_residue',
    'alexscaves:muck'
  ]), 'abyssal/abyssmarine_recovery')

  selfRecipe(create.mixing('2x alexscaves:bioluminesscence', [
    'minecraft:glow_lichen',
    'minecraft:sea_pickle',
    'minecraft:prismarine_shard'
  ]), 'abyssal/bioluminesscence_primary')
  selfRecipe(create.mixing('alexscaves:bioluminesscence', [
    '2x dimensionworks_cave_factory:pressure_residue',
    'minecraft:glow_lichen'
  ]), 'abyssal/bioluminesscence_recovery')

  selfRecipe(create.compacting('alexscaves:marine_snow', [
    '2x dimensionworks_cave_factory:pressure_residue',
    'minecraft:clay_ball'
  ]), 'abyssal/marine_snow_recovery')

  // Forlorn Hollows: reconstruct guano and guanostone, keep a thornwood
  // sapling loop, and convert a renewable dark-tatter chain into pure darkness.
  supportRecipe(create.compacting('2x alexscaves:guano', [
    'minecraft:rotten_flesh',
    'minecraft:bone_meal',
    Fluid.of('minecraft:water', 250)
  ]), 'forlorn/guano')
  supportRecipe(create.compacting('2x alexscaves:guanostone', [
    'alexscaves:guano',
    'minecraft:deepslate',
    Fluid.of('minecraft:water', 250)
  ]), 'forlorn/guanostone')
  supportRecipe(create.compacting('2x alexscaves:fertilizer', [
    'minecraft:bone_meal',
    'minecraft:rotten_flesh',
    Fluid.of('minecraft:water', 250)
  ]), 'forlorn/fertilizer')
  supportRecipe(create.compacting('alexscaves:thornwood_sapling', [
    '2x alexscaves:thornwood_branch',
    'alexscaves:fertilizer'
  ]), 'forlorn/thornwood_sapling')
  supportRecipe(create.mixing('2x alexscaves:shadow_silk', [
    'minecraft:cobweb',
    'minecraft:ink_sac',
    'minecraft:string'
  ]), 'forlorn/shadow_silk')
  supportRecipe(create.mixing('2x alexscaves:dark_tatters', [
    'minecraft:black_wool',
    'alexscaves:shadow_silk'
  ]), 'forlorn/dark_tatters')

  selfRecipe(create.compacting('2x alexscaves:coprolith', [
    'alexscaves:guanostone',
    'alexscaves:guano',
    Fluid.of('minecraft:water', 250)
  ]), 'forlorn/coprolith_primary')
  selfRecipe(create.compacting('alexscaves:coprolith', [
    '2x dimensionworks_cave_factory:umbral_residue',
    'alexscaves:guano'
  ]), 'forlorn/coprolith_recovery')

  selfRecipe(create.mixing('4x alexscaves:thornwood_log', [
    'alexscaves:thornwood_sapling',
    'minecraft:bone_meal',
    Fluid.of('minecraft:water', 250)
  ]), 'forlorn/thornwood_log_primary')
  selfRecipe(create.compacting('alexscaves:thornwood_log', [
    '2x dimensionworks_cave_factory:umbral_residue',
    'minecraft:bone_meal'
  ]), 'forlorn/thornwood_log_recovery')

  selfRecipe(create.haunting('2x alexscaves:pure_darkness', [
    'alexscaves:dark_tatters'
  ]), 'forlorn/pure_darkness_primary')
  selfRecipe(create.mixing('alexscaves:pure_darkness', [
    '2x dimensionworks_cave_factory:umbral_residue',
    'minecraft:black_wool'
  ]), 'forlorn/pure_darkness_recovery')

  // Candy Cavity: sugar and peppermint form the crystal base, colored
  // gelatin shares one primary stock, and soda closes the fluid loop.
  supportRecipe(create.mixing('4x alexscaves:candy_cane', [
    '2x minecraft:sugar',
    'minecraft:white_dye',
    Fluid.of('minecraft:water', 250)
  ]), 'candy/candy_cane')
  supportRecipe(create.milling('2x alexscaves:peppermint_powder', 'alexscaves:candy_cane'), 'candy/peppermint_powder')

  selfRecipe(create.compacting('4x alexscaves:rock_candy_white', [
    '4x minecraft:sugar',
    'alexscaves:peppermint_powder',
    Fluid.of('alexscaves:purple_soda', 250)
  ]), 'candy/rock_candy_primary')
  selfRecipe(create.compacting('alexscaves:rock_candy_white', [
    '2x dimensionworks_cave_factory:crystal_residue',
    'minecraft:sugar'
  ]), 'candy/rock_candy_recovery')

  selfRecipe(create.mixing('2x alexscaves:gelatin_pink', [
    'minecraft:dried_kelp',
    'minecraft:sugar',
    'minecraft:slime_ball',
    Fluid.of('alexscaves:purple_soda', 250)
  ]), 'candy/gelatin_primary')
  selfRecipe(create.mixing('alexscaves:gelatin_pink', [
    '2x dimensionworks_cave_factory:crystal_residue',
    'minecraft:dried_kelp'
  ]), 'candy/gelatin_recovery')

  const gelatinColors = [
    ['red', 'minecraft:red_dye'],
    ['green', 'minecraft:green_dye'],
    ['yellow', 'minecraft:yellow_dye'],
    ['blue', 'minecraft:blue_dye']
  ]
  gelatinColors.forEach(entry => {
    supportRecipe(create.mixing(`2x alexscaves:gelatin_${entry[0]}`, [
      'alexscaves:gelatin_pink',
      entry[1]
    ]), `candy/gelatin_${entry[0]}_color`)
  })

  selfRecipe(create.mixing(Fluid.of('alexscaves:purple_soda', 1000), [
    'minecraft:sugar',
    'minecraft:sweet_berries',
    '#alexscaves:gelatins',
    Fluid.of('minecraft:water', 250)
  ]), 'candy/purple_soda_primary')
  selfRecipe(create.mixing(Fluid.of('alexscaves:purple_soda', 500), [
    '2x dimensionworks_cave_factory:crystal_residue',
    Fluid.of('minecraft:water', 500)
  ]), 'candy/purple_soda_recovery')
})
