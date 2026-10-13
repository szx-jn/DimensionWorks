const repairFragmentIds = Array.from({ length: 7 }, (_, index) =>
  `kubejs:gear_heart_repair_fragment_${index + 1}`
)

ServerEvents.recipes(event => {
  const register = (json, id) => {
    event.custom(JsonIO.of(json)).id(id)
  }

  register({
    type: 'create:pressing',
    ingredients: [{ item: 'create:andesite_alloy' }],
    results: [{ item: repairFragmentIds[0] }]
  }, repairFragmentIds[0])

  register({
    type: 'create:compacting',
    ingredients: [
      { item: 'create:iron_sheet' },
      { item: 'create:brass_sheet' }
    ],
    results: [{ item: repairFragmentIds[1] }]
  }, repairFragmentIds[1])

  register({
    type: 'mekanism:enriching',
    input: {
      ingredient: { item: 'create:precision_mechanism' }
    },
    output: { item: repairFragmentIds[2] }
  }, repairFragmentIds[2])

  register({
    type: 'create:mechanical_crafting',
    acceptMirrored: false,
    pattern: ['LCL', 'EPE', 'LCL'],
    key: {
      L: { item: 'ae2:logic_processor' },
      C: { item: 'mekanism:basic_control_circuit' },
      E: { item: 'create:electron_tube' },
      P: { item: 'create:precision_mechanism' }
    },
    result: { item: repairFragmentIds[3] }
  }, repairFragmentIds[3])

  register({
    type: 'create:mechanical_crafting',
    acceptMirrored: false,
    pattern: ['SAS', 'ADA', 'SCS'],
    key: {
      S: { item: 'mekanism:ingot_steel' },
      A: { item: 'mekanism:alloy_infused' },
      D: { item: 'dimensionworks_mek_stress:memory_drive_ddr1' },
      C: { item: 'mekanism:basic_control_circuit' }
    },
    result: { item: repairFragmentIds[4] }
  }, repairFragmentIds[4])

  register({
    type: 'create:mechanical_crafting',
    acceptMirrored: false,
    pattern: ['GUG', 'EAE', 'GUG'],
    key: {
      G: { item: 'mekanism:universal_cable' },
      U: { item: 'ae2:fluix_crystal' },
      E: { item: 'create:electron_tube' },
      A: { item: 'appliedcreate:me_gearbox' }
    },
    result: { item: repairFragmentIds[5] }
  }, repairFragmentIds[5])

  register({
    type: 'create:mechanical_crafting',
    acceptMirrored: false,
    pattern: ['APA', 'MCM', 'APA'],
    key: {
      A: { item: 'ae2:engineering_processor' },
      P: { item: 'dimensionworks_mek_stress:memory_card_ddr3_balanced' },
      M: { item: 'mekanism:alloy_atomic' },
      C: { item: 'create:precision_mechanism' }
    },
    result: { item: repairFragmentIds[6] }
  }, repairFragmentIds[6])
})
