// 弹力工具由 Transfer 注册；本文件只注册配方中的可储运工件。
StartupEvents.registry('item', event => {
  event.create('elastic_compound').texture('kubejs:item/elastic_compound')
  event.create('steeleaf_seal_strip').texture('kubejs:item/steeleaf_seal_strip')
  event.create('incomplete_bounce_tape', 'create:sequenced_assembly')
    .texture('kubejs:item/incomplete_bounce_tape')
  event.create('bounce_tape').texture('kubejs:item/bounce_tape')
})
