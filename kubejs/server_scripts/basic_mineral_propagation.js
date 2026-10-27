// DimensionWorks seeded mineral propagation: three distinct production styles per target.
// First obtain one finite sample of each target; every route consumes its sample and returns two.
const BASIC_MINERAL_RECIPES = 'dimensionworks_cave_factory:self_propagation/basic_minerals/'

const BASIC_MINERAL_LINES = [
  {
    "item": "minecraft:raw_iron",
    "path": "iron",
    "compactingInputs": [
      "2x minecraft:cobblestone",
      "minecraft:gravel"
    ],
    "mekanismCarrier": "create:andesite_alloy",
    "mekanismCarrierCount": 2,
    "residueCount": 2,
    "recoveryInputs": [
      "minecraft:iron_nugget",
      "minecraft:gravel"
    ],
    "recoveryFluid": "minecraft:water",
    "fluidAmount": 250,
    "heat": "heated"
  },
  {
    "item": "minecraft:raw_copper",
    "path": "copper",
    "compactingInputs": [
      "2x minecraft:gravel",
      "minecraft:calcite"
    ],
    "mekanismCarrier": "minecraft:flint",
    "mekanismCarrierCount": 2,
    "residueCount": 2,
    "recoveryInputs": [
      "minecraft:copper_ingot",
      "minecraft:gravel"
    ],
    "recoveryFluid": "minecraft:water",
    "fluidAmount": 250,
    "heat": "heated"
  },
  {
    "item": "create:raw_zinc",
    "path": "zinc",
    "compactingInputs": [
      "2x minecraft:cobblestone",
      "minecraft:quartz"
    ],
    "mekanismCarrier": "create:brass_ingot",
    "mekanismCarrierCount": 2,
    "residueCount": 3,
    "recoveryInputs": [
      "create:zinc_ingot",
      "minecraft:gravel"
    ],
    "recoveryFluid": "minecraft:water",
    "fluidAmount": 500,
    "heat": "heated"
  },
  {
    "item": "minecraft:raw_gold",
    "path": "gold",
    "compactingInputs": [
      "2x minecraft:netherrack",
      "minecraft:quartz"
    ],
    "mekanismCarrier": "minecraft:blaze_powder",
    "mekanismCarrierCount": 3,
    "residueCount": 3,
    "recoveryInputs": [
      "minecraft:gold_nugget",
      "minecraft:netherrack"
    ],
    "recoveryFluid": "minecraft:water",
    "fluidAmount": 500,
    "heat": "heated"
  },
  {
    "item": "minecraft:coal",
    "path": "coal",
    "compactingInputs": [
      "2x minecraft:charcoal",
      "minecraft:gravel"
    ],
    "mekanismCarrier": "minecraft:charcoal",
    "mekanismCarrierCount": 2,
    "residueCount": 2,
    "recoveryInputs": [
      "minecraft:charcoal",
      "minecraft:gravel"
    ],
    "recoveryFluid": "minecraft:water",
    "fluidAmount": 250,
    "heat": "heated"
  },
  {
    "item": "minecraft:redstone",
    "path": "redstone",
    "compactingInputs": [
      "minecraft:clay_ball",
      "minecraft:iron_nugget",
      "minecraft:flint"
    ],
    "mekanismCarrier": "minecraft:glowstone_dust",
    "mekanismCarrierCount": 2,
    "residueCount": 3,
    "recoveryInputs": [
      "minecraft:clay_ball",
      "minecraft:flint"
    ],
    "recoveryFluid": "minecraft:water",
    "fluidAmount": 250,
    "heat": "heated"
  },
  {
    "item": "minecraft:lapis_lazuli",
    "path": "lapis",
    "compactingInputs": [
      "minecraft:clay_ball",
      "minecraft:bone_meal",
      "minecraft:gravel"
    ],
    "mekanismCarrier": "minecraft:clay_ball",
    "mekanismCarrierCount": 2,
    "residueCount": 3,
    "recoveryInputs": [
      "minecraft:clay_ball",
      "minecraft:bone_meal",
      "minecraft:gravel"
    ],
    "recoveryFluid": "minecraft:water",
    "fluidAmount": 500,
    "heat": "heated"
  },
  {
    "item": "minecraft:quartz",
    "path": "quartz",
    "compactingInputs": [
      "2x minecraft:netherrack",
      "minecraft:soul_sand"
    ],
    "mekanismCarrier": "minecraft:soul_sand",
    "mekanismCarrierCount": 2,
    "residueCount": 2,
    "recoveryInputs": [
      "minecraft:netherrack",
      "minecraft:soul_sand"
    ],
    "recoveryFluid": "minecraft:water",
    "fluidAmount": 250,
    "heat": "heated"
  },
  {
    "item": "minecraft:amethyst_shard",
    "path": "amethyst",
    "compactingInputs": [
      "2x minecraft:calcite",
      "2x minecraft:quartz"
    ],
    "mekanismCarrier": "minecraft:glowstone_dust",
    "mekanismCarrierCount": 2,
    "residueCount": 3,
    "recoveryInputs": [
      "minecraft:calcite",
      "minecraft:quartz",
      "minecraft:glowstone_dust"
    ],
    "recoveryFluid": "minecraft:water",
    "fluidAmount": 500,
    "heat": "heated"
  },
  {
    "item": "mekanism:raw_tin",
    "path": "tin",
    "compactingInputs": [
      "2x minecraft:cobblestone",
      "minecraft:gravel",
      "minecraft:copper_ingot"
    ],
    "mekanismCarrier": "minecraft:deepslate",
    "mekanismCarrierCount": 2,
    "residueCount": 2,
    "recoveryInputs": [
      "minecraft:copper_ingot",
      "minecraft:cobblestone"
    ],
    "recoveryFluid": "minecraft:water",
    "fluidAmount": 500,
    "heat": "heated"
  },
  {
    "item": "mekanism:raw_lead",
    "path": "lead",
    "compactingInputs": [
      "2x minecraft:gravel",
      "minecraft:flint",
      "minecraft:iron_ingot"
    ],
    "mekanismCarrier": "minecraft:flint",
    "mekanismCarrierCount": 2,
    "residueCount": 2,
    "recoveryInputs": [
      "minecraft:iron_ingot",
      "minecraft:gravel",
      "minecraft:flint"
    ],
    "recoveryFluid": "minecraft:water",
    "fluidAmount": 500,
    "heat": "heated"
  },
  {
    "item": "mekanism:raw_osmium",
    "path": "osmium",
    "compactingInputs": [
      "2x minecraft:deepslate",
      "minecraft:iron_ingot",
      "minecraft:quartz"
    ],
    "mekanismCarrier": "create:brass_ingot",
    "mekanismCarrierCount": 2,
    "residueCount": 3,
    "recoveryInputs": [
      "minecraft:iron_ingot",
      "minecraft:deepslate",
      "minecraft:quartz"
    ],
    "recoveryFluid": "minecraft:water",
    "fluidAmount": 500,
    "heat": "heated"
  },
  {
    "item": "mekanism:fluorite_gem",
    "path": "fluorite",
    "compactingInputs": [
      "2x minecraft:basalt",
      "minecraft:calcite",
      "minecraft:glowstone_dust"
    ],
    "mekanismCarrier": "minecraft:lapis_lazuli",
    "mekanismCarrierCount": 2,
    "residueCount": 3,
    "recoveryInputs": [
      "minecraft:calcite",
      "minecraft:glowstone_dust"
    ],
    "recoveryFluid": "minecraft:water",
    "fluidAmount": 500,
    "heat": "heated"
  }
]

function mineralRecipe(recipe, path) {
  return recipe.id(BASIC_MINERAL_RECIPES + path)
}

// Create processing ingredients are consumed per ingredient entry, not by "2x item"
// strings. Expand counts before registering the recipe so displayed and real costs match.
function expandMineralIngredients(inputs) {
  const expanded = []
  inputs.forEach(entry => {
    const match = typeof entry === 'string' ? /^(\d+)x (.+)$/.exec(entry) : null
    if (match) {
      for (let index = 0; index < Number(match[1]); index++) expanded.push(match[2])
    } else {
      expanded.push(entry)
    }
  })
  return expanded
}

ServerEvents.recipes(event => {
  BASIC_MINERAL_LINES.forEach(mineral => {
    const item = mineral.item
    const recipePath = mineral.path + '/'

    // Line 1 — Create mechanical compaction. The output includes a guaranteed residue.
    const compactingRecipe = event.recipes.create.compacting(
      ['2x ' + item, 'kubejs:mineral_residue'],
      expandMineralIngredients([item].concat(mineral.compactingInputs))
    )
    mineralRecipe(compactingRecipe, recipePath + 'mechanical_growth')

    // Line 2 — Mekanism Combiner. Use the KubeJS Mekanism integration so
    // both input quantities are encoded as real ingredient-with-amount values.
    const combiningRecipe = event.recipes.mekanism.combining(
      '2x ' + item,
      item,
      mineral.mekanismCarrierCount + 'x ' + mineral.mekanismCarrier
    )
    mineralRecipe(combiningRecipe, recipePath + 'mekanism_combining')

    // Line 3 — Create heated residue recovery. A mineral sample is still required:
    // universal residue is a feedstock, not a bypass around the finite-sample start.
    const recoveryInputs = [item, mineral.residueCount + 'x kubejs:mineral_residue']
      .concat(mineral.recoveryInputs)
    recoveryInputs.push(Fluid.of(mineral.recoveryFluid, mineral.fluidAmount))
    const recoveryRecipe = event.recipes.create.mixing(
      '2x ' + item,
      expandMineralIngredients(recoveryInputs)
    )
    if (mineral.heat === 'superheated') {
      recoveryRecipe.superheated()
    } else {
      recoveryRecipe.heated()
    }
    mineralRecipe(recoveryRecipe, recipePath + 'residue_recovery')
  })
  console.info('[DimensionWorks] BASIC_MINERAL_RECIPES_REGISTERED targets=' + BASIC_MINERAL_LINES.length + ' recipes=' + (BASIC_MINERAL_LINES.length * 3))
})
