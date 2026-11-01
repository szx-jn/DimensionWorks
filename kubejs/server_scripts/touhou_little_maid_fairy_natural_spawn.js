// Block every spawn reason, including spawn eggs and mod-created entities.
EntityEvents.checkSpawn('touhou_little_maid:fairy', event => {
  event.cancel()
})

// Entity addition is the final gate; checkSpawn alone does not cover every path.
// This also rejects saved fairies when their chunks are loaded again.
EntityEvents.spawned('touhou_little_maid:fairy', event => {
  event.cancel()
})
