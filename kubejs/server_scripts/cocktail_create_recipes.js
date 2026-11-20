// Cocktails Delight 1.1.12 publishes these native recipes with obsolete Create fields.
// Each published pair uses one container and 250 mB; keep the three tankard drinks as tankards.
const dwCocktailBeverages = [
  'blue_curacao', 'bock', 'cream_ale', 'gin', 'hard_cider', 'hard_lemonade',
  'ipa', 'kraken_rum', 'milk_stout', 'moonshine', 'mulled_wine', 'nut_brown_ale',
  'peach_wine', 'pilsner', 'plum_wine', 'porter', 'red_ale', 'red_wine',
  'rum', 'stout', 'sweet_red_wine', 'tequila', 'whiskey', 'white_wine'
]
const dwCocktailTankardBeverages = ['kraken_rum', 'nut_brown_ale', 'rum']

ServerEvents.generateData('after_mods', event => {
  dwCocktailBeverages.forEach(beverage => {
    const id = 'cocktailsdelight:' + beverage
    const container = dwCocktailTankardBeverages.includes(beverage)
      ? 'brewinandchewin:tankard' : 'minecraft:glass_bottle'
    const conditions = [{ type: 'neoforge:mod_loaded', modid: 'create' }]
    event.json('cocktailsdelight:recipe/filling/' + beverage + '.json', {
      'neoforge:conditions': conditions,
      type: 'create:filling',
      ingredients: [{ item: container }, { type: 'neoforge:single', fluid: id, amount: 250 }],
      results: [{ id: id }]
    })
    event.json('cocktailsdelight:recipe/emptying/' + beverage + '.json', {
      'neoforge:conditions': conditions,
      type: 'create:emptying',
      ingredients: [{ item: id }],
      results: [{ id: container }, { id: id, amount: 250 }]
    })
  })
  console.info('[DimensionWorks] Native cocktail Create recipes corrected=' + dwCocktailBeverages.length * 2)
})
