const memoryTiers = [
  { tier: 1, cards: 4, plate: 'ae2:logic_processor', alloy: 'mekanism:alloy_infused', circuit: 'mekanism:basic_control_circuit' },
  { tier: 2, cards: 8, plate: 'ae2:calculation_processor', alloy: 'mekanism:alloy_reinforced', circuit: 'mekanism:advanced_control_circuit' },
  { tier: 3, cards: 16, plate: 'ae2:engineering_processor', alloy: 'mekanism:alloy_reinforced', circuit: 'mekanism:elite_control_circuit' },
  { tier: 4, cards: 32, plate: 'ae2:engineering_processor', alloy: 'mekanism:alloy_atomic', circuit: 'mekanism:ultimate_control_circuit' },
  { tier: 5, cards: 64, plate: 'ae2:engineering_processor', alloy: 'mekanism:pellet_antimatter', circuit: 'mekanism:ultimate_control_circuit' }
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
    event.shaped(`${definition.cards}x dimensionworks_mek_stress:memory_card_ddr${tier}`, [
      'PAP',
      'ACA',
      'PAP'
    ], {
      P: definition.plate,
      A: definition.alloy,
      C: definition.circuit
    })
  })

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
