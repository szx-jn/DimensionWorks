ServerEvents.recipes(event => {
  event.remove({ output: 'twilightforest:uncrafting_table' })
})

PlayerEvents.inventoryChanged(event => {
  if (event.item.id === 'twilightforest:uncrafting_table') event.item.setCount(0)
})

PlayerEvents.loggedIn(event => {
  let removed = false
  for (let slot = 0; slot <= 40; slot++) {
    const stack = event.player.inventory.getStackInSlot(slot)
    if (stack.id === 'twilightforest:uncrafting_table') {
      stack.setCount(0)
      removed = true
    }
  }
  if (removed) event.player.sendInventoryUpdate()
})

BlockEvents.placed('twilightforest:uncrafting_table', event => event.cancel())
BlockEvents.rightClicked('twilightforest:uncrafting_table', event => event.cancel())
