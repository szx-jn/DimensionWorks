// 暮色现场原料。可运输、存储，生产条件由自研浸出槽检查。
StartupEvents.registry('fluid', event => {
  event.create('dimensionworks:forest_extract')
    .stillTexture('dimensionworks_cave_factory:block/forest_extract_still')
    .flowingTexture('dimensionworks_cave_factory:block/forest_extract_flow')
    .bucketColor(0x709450)
    .displayName('林地原液')
})
