const memoryTiers = [
  { tier: 1, plate: 'ae2:logic_processor', alloy: 'mekanism:alloy_infused', circuit: 'mekanism:basic_control_circuit', storage: 'ae2:cell_component_1k' },
  { tier: 2, plate: 'ae2:calculation_processor', alloy: 'mekanism:alloy_reinforced', circuit: 'mekanism:advanced_control_circuit', storage: 'ae2:cell_component_4k' },
  { tier: 3, plate: 'ae2:engineering_processor', alloy: 'mekanism:alloy_reinforced', circuit: 'mekanism:elite_control_circuit', storage: 'ae2:cell_component_16k' },
  { tier: 4, plate: 'ae2:engineering_processor', alloy: 'mekanism:alloy_atomic', circuit: 'mekanism:ultimate_control_circuit', storage: 'ae2:cell_component_64k' },
  { tier: 5, plate: 'ae2:engineering_processor', alloy: 'mekanism:pellet_antimatter', circuit: 'mekanism:ultimate_control_circuit', storage: 'ae2:cell_component_256k' }
]

const retiredAppliedCreateItems = [
  'appliedcreate:stress_storage_cell_1k',
  'appliedcreate:stress_storage_cell_4k',
  'appliedcreate:stress_storage_cell_16k',
  'appliedcreate:stress_storage_cell_64k',
  'appliedcreate:stress_storage_cell_256k',
  'appliedcreate:stress_storage_cell_1m',
  'appliedcreate:stress_storage_cell_4m',
  'appliedcreate:stress_storage_cell_16m',
  'appliedcreate:stress_storage_cell_64m',
  'appliedcreate:stress_storage_cell_256m',
  'appliedcreate:stress_storage_component_1k',
  'appliedcreate:stress_storage_component_4k',
  'appliedcreate:stress_storage_component_16k',
  'appliedcreate:stress_storage_component_64k',
  'appliedcreate:stress_storage_component_256k',
  'appliedcreate:stress_storage_component_1m',
  'appliedcreate:stress_storage_component_4m',
  'appliedcreate:stress_storage_component_16m',
  'appliedcreate:stress_storage_component_64m',
  'appliedcreate:stress_storage_component_256m',
  'appliedcreate:andesite_stress_cell_housing',
  'appliedcreate:brass_stress_cell_housing',
  'appliedcreate:creative_stress_cell'
]

ServerEvents.recipes(event => {
  memoryTiers.forEach(definition => {
    const tier = definition.tier
    event.shapeless(`4x dimensionworks_mek_stress:memory_card_ddr${tier}_economy`, [
      definition.plate,
      definition.alloy,
      definition.alloy,
      definition.circuit
    ]).id(`dimensionworks_mek_stress:memory_card_ddr${tier}_economy`)

    event.shaped(`4x dimensionworks_mek_stress:memory_card_ddr${tier}_balanced`, [
      'PAP',
      'ACA',
      'PAP'
    ], {
      P: definition.plate,
      A: definition.alloy,
      C: definition.circuit
    }).id(`dimensionworks_mek_stress:memory_card_ddr${tier}_balanced`)

    event.shaped(`4x dimensionworks_mek_stress:memory_card_ddr${tier}_high_speed`, [
      'PAP',
      'ACA',
      'PAP'
    ], {
      P: definition.plate,
      A: definition.alloy,
      C: 'ae2:speed_card'
    }).id(`dimensionworks_mek_stress:memory_card_ddr${tier}_high_speed`)

    event.shaped(`4x dimensionworks_mek_stress:memory_card_ddr${tier}_high_storage`, [
      'PAP',
      'ACA',
      'PAP'
    ], {
      P: definition.plate,
      A: definition.alloy,
      C: definition.storage
    }).id(`dimensionworks_mek_stress:memory_card_ddr${tier}_high_storage`)
  })

  for (let tier = 1; tier <= 5; tier++) {
    event.remove({ output: `dimensionworks_mek_stress:memory_card_ddr${tier}` })
  }

  event.shaped('dimensionworks_mek_stress:memory_drive_ddr1', [
    'SAS',
    'ADA',
    'SCS'
  ], {
    S: 'mekanism:ingot_steel',
    A: 'mekanism:alloy_infused',
    D: 'ae2:drive',
    C: 'mekanism:basic_control_circuit'
  })

  for (let tier = 2; tier <= 5; tier++) {
    const tierDefinition = memoryTiers[tier - 1]
    event.shaped(`dimensionworks_mek_stress:memory_drive_ddr${tier}`, [
      'PAP',
      'ADA',
      'PAP'
    ], {
      P: `dimensionworks_mek_stress:memory_drive_ddr${tier - 1}`,
      A: tierDefinition.alloy,
      D: tierDefinition.plate
    })
  }

  retiredAppliedCreateItems.forEach(id => event.remove({ output: id }))
  event.remove({ id: 'appliedcreate:creative_motor_from_stress_cell' })
})

ItemEvents.canPickUp(event => {
  if (retiredAppliedCreateItems.includes(event.item.id)) {
    event.cancel()
  }
})

ItemEvents.rightClicked(event => {
  if (retiredAppliedCreateItems.includes(event.item.id)) {
    event.cancel()
  }
})

PlayerEvents.inventoryChanged(event => {
  if (retiredAppliedCreateItems.includes(event.item.id)) {
    event.item.setCount(0)
  }
})
