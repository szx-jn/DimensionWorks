// 普通工业元件，不注册能量储存能力；工序进度由 Create 顺序装配记录。
StartupEvents.registry('item', event => {
  event.create('energy_plate')
    .displayName(Text.translate('item.kubejs.energy_plate'))
    .tooltip(Text.translate('item.kubejs.energy_plate.tooltip'))
    .texture('kubejs:item/energy_plate')

  event.create('incomplete_energy_plate', 'create:sequenced_assembly')
    .displayName(Text.translate('item.kubejs.incomplete_energy_plate'))
    .tooltip(Text.translate('item.kubejs.incomplete_energy_plate.tooltip'))
    .texture('kubejs:item/incomplete_energy_plate')
})
