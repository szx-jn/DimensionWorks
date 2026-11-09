// DimensionWorks：JEI 中不展示精妙存储的网络控制、连接与物流组件。
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

JEIEvents.hideItems(event => {
  dwRetiredSophisticatedStorageNetworkItems.forEach(id => event.hide(id))
})
