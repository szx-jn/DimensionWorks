// CCB owns the active chemistry gas definitions. Legacy Mekanism entries are
// kept only so existing ChemicalStacks in saves can still resolve.
const DW_CHEMISTRY_GASES = [
  { id: 'dimensionworks:carbon_dioxide', tint: 0x9AA5B1, legacyMekanism: true },
  { id: 'dimensionworks:carbon_monoxide', tint: 0x7D8791, legacyMekanism: true },
  { id: 'dimensionworks:nitrogen', tint: 0x9EB9D8, legacyMekanism: true },
  { id: 'dimensionworks:ammonia', tint: 0xB8E0E8, legacyMekanism: true },
  { id: 'dimensionworks:nitric_oxide', tint: 0xA5B8C8, legacyMekanism: true },
  { id: 'dimensionworks:nitrogen_dioxide', tint: 0xB86A35, legacyMekanism: true },
  { id: 'dimensionworks:nitrous_oxide', tint: 0xB8C7D5, legacyMekanism: true },
  { id: 'dimensionworks:hydrogen_sulfide', tint: 0xD5D56B, legacyMekanism: true },
  { id: 'dimensionworks:methane', tint: 0xB6C8A0, legacyMekanism: true },
  { id: 'dimensionworks:ethyne', tint: 0xC6B5A5, legacyMekanism: true },
  { id: 'dimensionworks:ozone', tint: 0x8A9CFF, legacyMekanism: true },
  { id: 'dimensionworks:nitric_acid', tint: 0xD8C5A8, legacyMekanism: true },
  { id: 'dimensionworks:hydrogen', tint: 0xD8EAF5, legacyMekanism: false },
  { id: 'dimensionworks:oxygen', tint: 0x8CAEFF, legacyMekanism: false }
]

StartupEvents.registry('createcraftedbeginning:gas', event => {
  DW_CHEMISTRY_GASES.forEach(gas => {
    event.create(gas.id)
      .tint(gas.tint)
      .alpha(255)
      .usableInEquipment(true)
  })
})

StartupEvents.registry('mekanism:chemical', event => {
  DW_CHEMISTRY_GASES
    .filter(gas => gas.legacyMekanism)
    .forEach(gas => {
      const hex = ('000000' + gas.tint.toString(16)).slice(-6)
      event.create(gas.id).tint('#' + hex).gaseous()
    })
})
