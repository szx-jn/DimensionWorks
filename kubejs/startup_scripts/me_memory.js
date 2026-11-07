const dwRetiredStressStorageItems = [
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

const dwRetiredMekanismStorageItems = [
  'mekanism:basic_energy_cube',
  'mekanism:advanced_energy_cube',
  'mekanism:elite_energy_cube',
  'mekanism:ultimate_energy_cube',
  'mekanism:creative_energy_cube',
  'mekanism:basic_induction_cell',
  'mekanism:advanced_induction_cell',
  'mekanism:elite_induction_cell',
  'mekanism:ultimate_induction_cell',
  'mekanism:basic_induction_provider',
  'mekanism:advanced_induction_provider',
  'mekanism:elite_induction_provider',
  'mekanism:ultimate_induction_provider'
]

const dwMemoryCardTypes = [
  'economy',
  'balanced',
  'high_speed',
  'high_storage',
  'defective',
  'final'
]

const dwMemoryItems = [
  'dimensionworks_mek_stress:memory_drive_ddr1',
  'dimensionworks_mek_stress:memory_drive_ddr2',
  'dimensionworks_mek_stress:memory_drive_ddr3',
  'dimensionworks_mek_stress:memory_drive_ddr4',
  'dimensionworks_mek_stress:memory_drive_ddr5'
]

for (let tier = 1; tier <= 5; tier++) {
  dwMemoryCardTypes.forEach(type => {
    dwMemoryItems.push(`dimensionworks_mek_stress:memory_card_ddr${tier}_${type}`)
  })
}

StartupEvents.modifyCreativeTab('appliedcreate:main', event => {
  dwRetiredStressStorageItems.forEach(id => event.remove(id))
})

StartupEvents.modifyCreativeTab('mekanism:mekanism', event => {
  dwRetiredMekanismStorageItems.forEach(id => event.remove(id))
})
