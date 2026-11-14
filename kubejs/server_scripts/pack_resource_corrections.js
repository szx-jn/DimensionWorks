// 修正当前固定版本第三方数据的失效引用；不注册旧名称或迁移数据。
ServerEvents.tags('item', event => {
  event.remove('forge:foods/meat/cooked', 'kaleidoscope_cookery:cooked_donkey_meat')
  event.remove('kaleidoscope_nether:mod_items', [
    'kaleidoscope_nether:giant_beast_croissant',
    'kaleidoscope_nether:magma_sweet_and_sour_pork'
  ])
})
