// Keep Mekanism's data-preserving factory crafting and its installer upgrade route
// at the same material cost. The installer is also usable on other tiered blocks.
ServerEvents.recipes(event => {
  const types = {
    smelting: 'energized_smelter', enriching: 'enrichment_chamber', crushing: 'crusher',
    compressing: 'osmium_compressor', combining: 'combiner', purifying: 'purification_chamber',
    injecting: 'chemical_injection_chamber', infusing: 'metallurgic_infuser', sawing: 'precision_sawmill'
  }
  const tiers = [
    { name: 'basic', alloy: 'mekanism:alloys/basic', circuit: 'forge:circuits/basic', create: 'create:andesite_casing' },
    { name: 'advanced', alloy: 'mekanism:alloys/infused', circuit: 'forge:circuits/advanced', create: 'create:brass_casing' },
    { name: 'elite', alloy: 'mekanism:alloys/reinforced', circuit: 'forge:circuits/elite', create: 'create:precision_mechanism' },
    { name: 'ultimate', alloy: 'mekanism:alloys/atomic', circuit: 'forge:circuits/ultimate', create: 'create:precision_mechanism' }
  ]
  tiers.forEach((tier, index) => {
    const key = {
      A: { tag: tier.alloy }, C: { tag: tier.circuit },
      I: { item: tier.create }, P: { tag: 'minecraft:planks' }
    }
    const installerId = 'mekanism:tier_installer/' + tier.name
    event.remove({ id: installerId })
    event.custom({ type: 'minecraft:crafting_shaped', pattern: ['ACA', 'IPI', 'ACA'], key: key,
      result: { item: 'mekanism:' + tier.name + '_tier_installer' } }).id(installerId)
    Object.keys(types).forEach(type => {
      const id = 'mekanism:factory/' + tier.name + '/' + type
      const previous = index === 0 ? types[type] : tiers[index - 1].name + '_' + type + '_factory'
      event.remove({ id: id })
      event.custom({ type: 'mekanism:mek_data', pattern: ['ACA', 'IPI', 'ACA'],
        key: { A: key.A, C: key.C, I: key.I, P: { item: 'mekanism:' + previous } },
        result: { item: 'mekanism:' + tier.name + '_' + type + '_factory' } }).id(id)
    })
  })
})
