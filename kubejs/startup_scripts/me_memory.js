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

const dwMemoryItems = [
  'dimensionworks_mek_stress:memory_drive_ddr1',
  'dimensionworks_mek_stress:memory_drive_ddr2',
  'dimensionworks_mek_stress:memory_drive_ddr3',
  'dimensionworks_mek_stress:memory_drive_ddr4',
  'dimensionworks_mek_stress:memory_drive_ddr5',
  'dimensionworks_mek_stress:memory_card_ddr1',
  'dimensionworks_mek_stress:memory_card_ddr2',
  'dimensionworks_mek_stress:memory_card_ddr3',
  'dimensionworks_mek_stress:memory_card_ddr4',
  'dimensionworks_mek_stress:memory_card_ddr5',
  'dimensionworks_mek_stress:stress_output_card'
]

StartupEvents.modifyCreativeTab('appliedcreate:main', event => {
  dwRetiredStressStorageItems.forEach(id => event.remove(id))
  dwMemoryItems.forEach(id => event.add(id))
})
