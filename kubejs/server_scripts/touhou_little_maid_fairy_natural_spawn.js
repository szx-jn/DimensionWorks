EntityEvents.checkSpawn(event => {
  if (event.entity.type == 'touhou_little_maid:fairy' && event.type.name() == 'NATURAL') {
    event.cancel()
  }
})
