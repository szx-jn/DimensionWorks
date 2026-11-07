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

const retiredMekanismStorageItems = [
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

JEIEvents.hideItems(event => {
  retiredAppliedCreateItems.forEach(id => event.hide(id))
  retiredMekanismStorageItems.forEach(id => event.hide(id))
})
