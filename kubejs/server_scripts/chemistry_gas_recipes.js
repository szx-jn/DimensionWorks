// Existing chemistry recipe IDs, ingredients, and output amounts are preserved.
// CCB GasStacks use GU; those values are not Mekanism mB.

ServerEvents.recipes(event => {
  const item = id => ({ item: id, count: 1 })
  const gasInput = (id, amount) => ({ amount: amount, ingredient: { gas: id } })
  const gasOutput = (id, amount) => ({ id: id, amount: amount })
  const fluid = (id, amount) => ({ type: 'neoforge:single', fluid: id, amount: amount })
  const kettle = (id, data) => event.custom(Object.assign({
    type: 'createcraftedbeginning:reactor_kettle'
  }, data)).id('dimensionworks:chemistry/' + id)
  const heatedKettle = (id, data, temperature) => kettle(id, Object.assign(data, {
    temperature: temperature,
    temperature_matching: 'exact'
  }))

  // Keep the historical gas amounts while moving their registry to CCB.
  heatedKettle('create/carbon_monoxide_from_charcoal', {
    ingredients: [item('minecraft:charcoal')],
    gas_results: [gasOutput('dimensionworks:carbon_monoxide', 125)]
  }, 'heated')
  heatedKettle('create/carbon_dioxide_from_charcoal', {
    ingredients: [item('minecraft:charcoal')],
    gas_results: [gasOutput('dimensionworks:carbon_dioxide', 250)]
  }, 'superheated')

  // Water electrolysis supplies the CCB gas chain with hydrogen and oxygen.
  heatedKettle('hydrogen_oxygen_from_water', {
    ingredients: [fluid('minecraft:water', 1000)],
    gas_results: [
      gasOutput('dimensionworks:hydrogen', 2000),
      gasOutput('dimensionworks:oxygen', 1000)
    ],
    processing_time: 100
  }, 'superheated')

  // Nitrogen: gunpowder is the pack's feedstock for this separation step.
  kettle('nitrogen_from_gunpowder', {
    ingredients: [item('minecraft:gunpowder')],
    gas_results: [gasOutput('dimensionworks:nitrogen', 1000)]
  })

  // Haber process: N2 + 3H2 -> 2NH3.
  heatedKettle('ammonia_haber_process', {
    gas_ingredients: [
      gasInput('dimensionworks:nitrogen', 1000),
      gasInput('dimensionworks:hydrogen', 3000)
    ],
    gas_results: [gasOutput('dimensionworks:ammonia', 2000)],
    processing_time: 100
  }, 'heated')

  // Nitrogen oxides and nitrous oxide.
  heatedKettle('nitric_oxide_from_nitrogen', {
    gas_ingredients: [
      gasInput('dimensionworks:nitrogen', 1000),
      gasInput('dimensionworks:oxygen', 1000)
    ],
    gas_results: [gasOutput('dimensionworks:nitric_oxide', 2000)],
    processing_time: 100
  }, 'superheated')
  heatedKettle('nitrogen_dioxide_from_nitric_oxide', {
    gas_ingredients: [
      gasInput('dimensionworks:nitric_oxide', 1000),
      gasInput('dimensionworks:oxygen', 500)
    ],
    gas_results: [gasOutput('dimensionworks:nitrogen_dioxide', 1000)],
    processing_time: 100
  }, 'heated')
  heatedKettle('nitrous_oxide_from_ammonia', {
    gas_ingredients: [
      gasInput('dimensionworks:ammonia', 1000),
      gasInput('dimensionworks:oxygen', 1000)
    ],
    gas_results: [gasOutput('dimensionworks:nitrous_oxide', 500)],
    processing_time: 100
  }, 'heated')

  heatedKettle('hydrogen_sulfide_prc', {
    ingredients: [
      { tag: 'c:dusts/sulfur', count: 1 },
      fluid('minecraft:water', 1000)
    ],
    gas_ingredients: [gasInput('dimensionworks:hydrogen', 1000)],
    gas_results: [gasOutput('dimensionworks:hydrogen_sulfide', 1000)],
    processing_time: 100
  }, 'heated')
  heatedKettle('methane_prc', {
    ingredients: [
      item('minecraft:coal'),
      fluid('minecraft:water', 1000)
    ],
    gas_ingredients: [gasInput('dimensionworks:hydrogen', 2000)],
    gas_results: [gasOutput('dimensionworks:methane', 1000)],
    processing_time: 160
  }, 'heated')
  heatedKettle('ethyne_prc', {
    ingredients: [
      item('minecraft:charcoal'),
      fluid('minecraft:water', 1000)
    ],
    gas_ingredients: [gasInput('dimensionworks:hydrogen', 1000)],
    gas_results: [gasOutput('dimensionworks:ethyne', 500)],
    processing_time: 160
  }, 'superheated')
  heatedKettle('ozone_activation', {
    gas_ingredients: [gasInput('dimensionworks:oxygen', 1000)],
    gas_results: [gasOutput('dimensionworks:ozone', 500)],
    processing_time: 100
  }, 'superheated')
  heatedKettle('nitric_acid_prc', {
    ingredients: [
      item('minecraft:iron_ingot'),
      fluid('minecraft:water', 1000)
    ],
    gas_ingredients: [gasInput('dimensionworks:nitrogen_dioxide', 1000)],
    gas_results: [gasOutput('dimensionworks:nitric_acid', 1000)],
    processing_time: 200
  }, 'heated')
})
