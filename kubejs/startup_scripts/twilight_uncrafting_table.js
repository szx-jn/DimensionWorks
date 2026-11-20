StartupEvents.modifyCreativeTab('twilightforest:blocks', event => {
  event.remove('twilightforest:uncrafting_table')
})

// Chunk load may occur during world generation, so modify full chunks on the
// server thread after the load event. Only scan sections containing the block.
const $DWLevelChunk = Java.loadClass('net.minecraft.world.level.chunk.LevelChunk')
const $DWBlockPos = Java.loadClass('net.minecraft.core.BlockPos')
const $DWBlocks = Java.loadClass('net.minecraft.world.level.block.Blocks')
const dwUncraftingTableId = 'twilightforest:uncrafting_table'

NativeEvents.onEvent('net.neoforged.neoforge.event.level.ChunkEvent$Load', event => {
  const chunk = event.chunk
  const level = event.level
  if (!(chunk instanceof $DWLevelChunk) || level.isClientSide()) return

  const chunkPos = chunk.getPos()
  level.getServer().execute(() => {
    if (!level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z)) return
    const sections = chunk.getSections()
    var section
    var baseY
    for (var index = 0; index < sections.length; index++) {
      section = sections[index]
      if (section.hasOnlyAir() || !section.maybeHas(state => state.getBlock().id === dwUncraftingTableId)) continue
      baseY = level.getMinBuildHeight() + index * 16
      for (let y = 0; y < 16; y++) {
        for (let z = 0; z < 16; z++) {
          for (let x = 0; x < 16; x++) {
            if (section.getBlockState(x, y, z).getBlock().id === dwUncraftingTableId) {
              level.setBlock(new $DWBlockPos(chunkPos.x * 16 + x, baseY + y, chunkPos.z * 16 + z), $DWBlocks.AIR.defaultBlockState(), 3)
            }
          }
        }
      }
    }
  })
})
