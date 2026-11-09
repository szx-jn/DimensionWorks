// DimensionWorks：精妙存储只保留独立存储容器，禁用网络控制与连接组件。
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

StartupEvents.modifyCreativeTab('sophisticatedstorage:main', event => {
  dwRetiredSophisticatedStorageNetworkItems.forEach(id => event.remove(id))
})

StartupEvents.modifyCreativeTab('sophisticatedcore:main', event => {
  event.remove('sophisticatedcore:ender_linker')
})
