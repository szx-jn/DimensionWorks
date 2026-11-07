// 顺序装配中间物；成品直接产出 Mekanism 能量板。
StartupEvents.registry('item', event => {
  event.create('incomplete_energy_plate', 'create:sequenced_assembly')
    .displayName(Text.translate('item.kubejs.incomplete_energy_plate'))
    .tooltip(Text.translate('item.kubejs.incomplete_energy_plate.tooltip'))
    .texture('kubejs:item/incomplete_energy_plate')
})
