// Create's glue uses vanilla durability. Preserve every other tag on existing stacks.
const SUPER_GLUE_ID = 'create:super_glue'

function protectSuperGlue(stack) {
  if (!stack || stack.id !== SUPER_GLUE_ID) return false
  const tag = stack.getOrCreateTag()
  if (tag.getBoolean('Unbreakable')) return false
  tag.putBoolean('Unbreakable', true)
  return true
}

function protectCarriedSuperGlue(player) {
  let changed = false
  for (let slot = 0; slot <= 40; slot++) {
    if (protectSuperGlue(player.inventory.getStackInSlot(slot))) changed = true
  }
  if (changed) player.sendInventoryUpdate()
}

ServerEvents.recipes(event => {
  // Applies to recipe outputs, including machine recipes supported by KubeJS.
  event.replaceOutput({}, SUPER_GLUE_ID, Item.of(SUPER_GLUE_ID, '{Unbreakable:1b}'))
})

PlayerEvents.inventoryChanged(event => protectSuperGlue(event.item))
PlayerEvents.loggedIn(event => protectCarriedSuperGlue(event.player))
PlayerEvents.tick(event => {
  // Restore the flag if another operation removes it while the stack is carried.
  if (event.player.age % 20 === 0) protectCarriedSuperGlue(event.player)
})
ItemEvents.rightClicked(SUPER_GLUE_ID, event => protectSuperGlue(event.item))
