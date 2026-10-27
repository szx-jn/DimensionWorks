// DimensionWorks seeded mineral propagation.
// Iron now uses a serial Create -> Mekanism line; the other mineral lines remain provisional.
// Generic residues and residue-recovery recipes are not used.
const BASIC_MINERAL_RECIPES = 'dimensionworks_cave_factory:self_propagation/basic_minerals/'

const BASIC_MINERAL_LINES = [
  {
    "item": "minecraft:raw_iron",
    "path": "iron",
    "compactingInputs": [
      "2x create:andesite_alloy"
    ],
    "mekanismCarrier": "mekanism:dust_iron",
    "mekanismCarrierCount": 1
  },
  {
    "item": "minecraft:raw_copper",
    "path": "copper",
    "compactingInputs": [
      "2x minecraft:gravel",
      "minecraft:calcite"
    ],
    "mekanismCarrier": "minecraft:flint",
    "mekanismCarrierCount": 2
  },
  {
    "item": "create:raw_zinc",
    "path": "zinc",
    "compactingInputs": [
      "2x minecraft:cobblestone",
      "minecraft:quartz"
    ],
    "mekanismCarrier": "create:brass_ingot",
    "mekanismCarrierCount": 2
  },
  {
    "item": "minecraft:raw_gold",
    "path": "gold",
    "compactingInputs": [
      "2x minecraft:netherrack",
      "minecraft:quartz"
    ],
    "mekanismCarrier": "minecraft:blaze_powder",
    "mekanismCarrierCount": 3
  },
  {
    "item": "minecraft:coal",
    "path": "coal",
    "compactingInputs": [
      "2x minecraft:charcoal",
      "minecraft:gravel"
    ],
    "mekanismCarrier": "minecraft:charcoal",
    "mekanismCarrierCount": 2
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
    "mekanismCarrierCount": 2
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
    "mekanismCarrierCount": 2
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
    "mekanismCarrierCount": 2
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
    "mekanismCarrierCount": 2
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
    "mekanismCarrierCount": 2
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
    "mekanismCarrierCount": 2
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
  // Raw iron already has a native Create Crushing Wheels recipe. Use the Millstone
  // adapter for this extra probabilistic iron-dust recipe to avoid duplicate wheel recipes.
  global.DimensionWorksCrushing.register(event, {
    id: BASIC_MINERAL_RECIPES + 'iron/dust_from_raw_iron',
    machine: 'create:milling',
    input: 'minecraft:raw_iron',
    outputs: [
      { item: 'mekanism:dust_iron', count: 2 },
      { item: 'mekanism:dust_iron', count: 1, chance: 0.7 }
    ]
  })

  BASIC_MINERAL_LINES.forEach(mineral => {
    const item = mineral.item
    const recipePath = mineral.path + '/'
    const isIronLine = mineral.path === 'iron'

    // Create mechanical compaction. Iron creates an intermediate that must go through Mekanism.
    const compactingOutput = isIronLine ? 'kubejs:iron_concentrate' : '2x ' + item
    const compactingRecipe = event.recipes.create.compacting(
      compactingOutput,
      expandMineralIngredients([item].concat(mineral.compactingInputs))
    )
    mineralRecipe(compactingRecipe, recipePath + 'mechanical_growth')

    // For iron, the Combiner consumes the Create intermediate plus iron dust.
    // Other target lines retain their existing provisional standalone Combiner recipes.
    const combiningInput = isIronLine ? 'kubejs:iron_concentrate' : item
    const combiningRecipe = event.recipes.mekanism.combining(
      '2x ' + item,
      combiningInput,
      mineral.mekanismCarrierCount + 'x ' + mineral.mekanismCarrier
    )
    mineralRecipe(combiningRecipe, recipePath + 'mekanism_combining')
  })

  console.info('[DimensionWorks] BASIC_MINERAL_RECIPES_REGISTERED targets=' + BASIC_MINERAL_LINES.length + ' recipes=' + (BASIC_MINERAL_LINES.length * 2 + 1))
})
