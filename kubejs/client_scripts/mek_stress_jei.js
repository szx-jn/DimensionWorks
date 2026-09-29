const hiddenMekanismEnergyBlocks = [
  'mekanism:basic_energy_cube',
  'mekanism:advanced_energy_cube',
  'mekanism:elite_energy_cube',
  'mekanism:ultimate_energy_cube',
  'mekanism:creative_energy_cube',
  'mekanism:basic_universal_cable',
  'mekanism:advanced_universal_cable',
  'mekanism:elite_universal_cable',
  'mekanism:ultimate_universal_cable',
  'mekanism:basic_thermodynamic_conductor',
  'mekanism:advanced_thermodynamic_conductor',
  'mekanism:elite_thermodynamic_conductor',
  'mekanism:ultimate_thermodynamic_conductor',
  'mekanism:induction_casing',
  'mekanism:induction_port',
  'mekanism:basic_induction_cell',
  'mekanism:advanced_induction_cell',
  'mekanism:elite_induction_cell',
  'mekanism:ultimate_induction_cell',
  'mekanism:basic_induction_provider',
  'mekanism:advanced_induction_provider',
  'mekanism:elite_induction_provider',
  'mekanism:ultimate_induction_provider',
  'mekanism:quantum_entangloporter',
  'mekanism:chargepad'
]

JEIEvents.hideItems(event => {
  hiddenMekanismEnergyBlocks.forEach(id => event.hide(id))
})
