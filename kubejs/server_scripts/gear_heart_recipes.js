const repairFragmentIds = [
  'kubejs:gear_heart_repair_fragment_1',
  'kubejs:gear_heart_repair_fragment_2',
  'kubejs:gear_heart_repair_fragment_3',
  'kubejs:gear_heart_repair_fragment_4',
  'kubejs:gear_heart_repair_fragment_5',
  'kubejs:gear_heart_repair_fragment_6',
  'kubejs:gear_heart_repair_fragment_7'
]

ServerEvents.recipes(event => {
  const register = (json, id) => {
    event.custom(json).id(id)
  }

  const incompleteFragment = { item: 'kubejs:incomplete_gear_heart_repair_fragment_1' }
  const metalCrystals = ['iron', 'copper', 'zinc', 'gold', 'tin', 'osmium']

  register({
    type: 'create:sequenced_assembly',
    ingredient: { item: 'create:andesite_alloy' },
    transitional_item: 'kubejs:incomplete_gear_heart_repair_fragment_1',
    loops: 1,
    sequence: [
      ...metalCrystals.map(metal => ({
        type: 'create:deploying',
        ingredients: [
          incompleteFragment,
          {
            type: 'neoforge:components',
            items: 'dimensionworks_pressure_crystals:low_pressure_crystal',
            components: {
              'minecraft:custom_data': {
                DimensionWorks: { Metal: metal }
              }
            }
          }
        ],
        results: [{ item: 'kubejs:incomplete_gear_heart_repair_fragment_1' }]
      })),
      {
        type: 'create:pressing',
        ingredients: [incompleteFragment],
        results: [{ item: 'kubejs:incomplete_gear_heart_repair_fragment_1' }]
      }
    ],
    results: [{ id: repairFragmentIds[0] }]
  }, repairFragmentIds[0])

  register({
    type: 'create:compacting',
    ingredients: [
      { item: 'create:iron_sheet' },
      { item: 'create:brass_sheet' }
    ],
    results: [{ id: repairFragmentIds[1] }]
  }, repairFragmentIds[1])

  register({
    type: 'mekanism:enriching',
    input: { item: 'create:precision_mechanism', count: 1 },
    output: { id: repairFragmentIds[2] }
  }, repairFragmentIds[2])

  register({
    type: 'create:mechanical_crafting',
    accept_mirrored: false,
    pattern: ['LCL', 'EPE', 'LCL'],
    key: {
      L: { item: 'ae2:logic_processor' },
      C: { item: 'mekanism:basic_control_circuit' },
      E: { item: 'create:electron_tube' },
      P: { item: 'create:precision_mechanism' }
    },
    result: { id: repairFragmentIds[3] }
  }, repairFragmentIds[3])

  register({
    type: 'create:mechanical_crafting',
    accept_mirrored: false,
    pattern: ['SAS', 'ADA', 'LCL'],
    key: {
      S: { item: 'mekanism:ingot_steel' },
      A: { item: 'mekanism:alloy_infused' },
      D: { item: 'dimensionworks_mek_stress:memory_drive_ddr1' },
      C: { item: 'mekanism:basic_control_circuit' },
      L: { item: 'mekanism:basic_control_circuit' }
    },
    result: { id: repairFragmentIds[4] }
  }, repairFragmentIds[4])

  register({
    type: 'create:mechanical_crafting',
    accept_mirrored: false,
    pattern: ['GUG', 'EAE', 'GUG'],
    key: {
      G: { item: 'mekanism:basic_universal_cable' },
      U: { item: 'ae2:fluix_crystal' },
      E: { item: 'create:electron_tube' },
      A: { item: 'appliedcreate:me_gearbox' }
    },
    result: { id: repairFragmentIds[5] }
  }, repairFragmentIds[5])

  register({
    type: 'create:mechanical_crafting',
    accept_mirrored: false,
    pattern: ['APA', 'MCM', 'APA'],
    key: {
      A: { item: 'ae2:engineering_processor' },
      P: { item: 'dimensionworks_mek_stress:memory_card_ddr3_balanced' },
      M: { item: 'mekanism:alloy_atomic' },
      C: { item: 'create:precision_mechanism' }
    },
    result: { id: repairFragmentIds[6] }
  }, repairFragmentIds[6])
})
