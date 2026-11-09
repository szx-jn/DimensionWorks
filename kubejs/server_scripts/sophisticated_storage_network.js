// DimensionWorks：移除精妙存储的网络控制、连接与物流组件配方。
const dwRetiredSophisticatedStorageNetworkItems = [
  'sophisticatedstorage:controller',
  'sophisticatedstorage:storage_connector',
  'sophisticatedstorage:storage_input',
  'sophisticatedstorage:storage_io',
  'sophisticatedstorage:storage_link',
  'sophisticatedstorage:storage_output',
  'sophisticatedstorage:storage_tool',
  'sophisticatedcore:ender_linker'
]

ServerEvents.recipes(event => {
  dwRetiredSophisticatedStorageNetworkItems.forEach(id => {
    event.remove({ output: id })
  })
})
