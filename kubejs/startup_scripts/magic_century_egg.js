StartupEvents.registry('item', event => {
  event.create('century_egg_mixture')
    .displayName(Text.translate('item.kubejs.century_egg_mixture'))
    .texture('kaleidoscope_chinesefood:item/century_egg')

  event.create('century_egg_smelted')
    .displayName(Text.translate('item.kubejs.century_egg_smelted'))
    .texture('kaleidoscope_chinesefood:item/century_egg')

  event.create('century_egg_washed')
    .displayName(Text.translate('item.kubejs.century_egg_washed'))
    .texture('kaleidoscope_chinesefood:item/century_egg')

  event.create('century_egg_pressed')
    .displayName(Text.translate('item.kubejs.century_egg_pressed'))
    .texture('kaleidoscope_chinesefood:item/century_egg')

  event.create('century_egg_vanilla_enriched')
    .displayName(Text.translate('item.kubejs.century_egg_vanilla_enriched'))
    .texture('kaleidoscope_chinesefood:item/century_egg')

  event.create('century_egg_industrial_enriched')
    .displayName(Text.translate('item.kubejs.century_egg_industrial_enriched'))
    .texture('kaleidoscope_chinesefood:item/century_egg')

  event.create('century_egg_ae_enriched')
    .displayName(Text.translate('item.kubejs.century_egg_ae_enriched'))
    .texture('kaleidoscope_chinesefood:item/century_egg')

  event.create('century_egg_eco_enriched')
    .displayName(Text.translate('item.kubejs.century_egg_eco_enriched'))
    .texture('kaleidoscope_chinesefood:item/century_egg')

  event.create('century_egg_eco_components_enriched')
    .displayName(Text.translate('item.kubejs.century_egg_eco_components_enriched'))
    .texture('kaleidoscope_chinesefood:item/century_egg')

  event.create('incomplete_magic_century_egg', 'create:sequenced_assembly')
    .displayName(Text.translate('item.kubejs.incomplete_magic_century_egg'))
    .tooltip(Text.translate('item.kubejs.incomplete_magic_century_egg.tooltip'))
    .texture('kaleidoscope_chinesefood:item/century_egg')
    .maxStackSize(1)

  event.create('incomplete_magic_century_egg_final', 'create:sequenced_assembly')
    .displayName(Text.translate('item.kubejs.incomplete_magic_century_egg_final'))
    .tooltip(Text.translate('item.kubejs.incomplete_magic_century_egg_final.tooltip'))
    .texture('kaleidoscope_chinesefood:item/century_egg')
    .maxStackSize(1)

  event.create('magic_century_egg')
    .displayName(Text.translate('item.kubejs.magic_century_egg'))
    .tooltip(Text.translate('item.kubejs.magic_century_egg.tooltip.1'))
    .tooltip(Text.translate('item.kubejs.magic_century_egg.tooltip.2'))
    .texture('kaleidoscope_chinesefood:item/century_egg')
    .rarity('epic')
    .glow(true)
    .food(food => food
      .hunger(20)
      .saturation(20)
      .alwaysEdible()
    )
})

StartupEvents.modifyCreativeTab('kaleidoscope_chinesefood:kaleidoscope_chinesefood_tab', event => {
  event.remove('kaleidoscope_chinesefood:century_egg')
})
