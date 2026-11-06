StartupEvents.modifyCreativeTab('twilightforest:blocks', event => {
  event.remove('twilightforest:uncrafting_table')
})

// Chunk load may occur during world generation, so modify full chunks on the
// server thread after the load event. Only scan sections containing the block.
const $DWLevelChunk = Java.loadClass('net.minecraft.world.level.chunk.LevelChunk')
const $DWBlockPos = Java.loadClass('net.minecraft.core.BlockPos')
const $DWResourceLocation = Java.loadClass('net.minecraft.resources.ResourceLocation')
const $DWForgeRegistries = Java.loadClass('net.minecraftforge.registries.ForgeRegistries')
const $DWBlocks = Java.loadClass('net.minecraft.world.level.block.Blocks')

ForgeEvents.onEvent('net.minecraftforge.event.level.ChunkEvent$Load', event => {
  const chunk = event.chunk
  const level = event.level
  if (!(chunk instanceof $DWLevelChunk) || level.isClientSide()) return

  const table = $DWForgeRegistries.BLOCKS.getValue(new $DWResourceLocation('twilightforest:uncrafting_table'))
  if (table == null) return
  const chunkPos = chunk.getPos()
  level.getServer().execute(() => {
    if (!level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z)) return
    const sections = chunk.getSections()
    // Rhino 2001.2.3 can mis-handle block-scoped declarations in repeated loop execution.
    var section
    var baseY
    for (var index = 0; index < sections.length; index++) {
      section = sections[index]
      if (section.hasOnlyAir() || !section.maybeHas(state => state.is(table))) continue
      baseY = level.getMinBuildHeight() + index * 16
      for (let y = 0; y < 16; y++) {
        for (let z = 0; z < 16; z++) {
          for (let x = 0; x < 16; x++) {
            if (section.getBlockState(x, y, z).is(table)) {
              level.setBlock(new $DWBlockPos(chunkPos.x * 16 + x, baseY + y, chunkPos.z * 16 + z), $DWBlocks.AIR.defaultBlockState(), 3)
            }
          }
        }
      }
    }
  })
})
