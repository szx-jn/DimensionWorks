// Create's glue uses vanilla durability. Preserve other components on existing stacks.
const SUPER_GLUE_ID = 'create:super_glue'
const $DWGlueComponents = Java.loadClass('net.minecraft.core.component.DataComponents')

function protectSuperGlue(stack) {
  if (!stack || stack.id !== SUPER_GLUE_ID) return false
  if (stack.getComponents().has($DWGlueComponents.UNBREAKABLE)) return false
  stack.setUnbreakableWithTooltip()
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
  const unbreakableGlue = Item.of(SUPER_GLUE_ID)
  unbreakableGlue.setUnbreakableWithTooltip()
  event.replaceOutput({}, SUPER_GLUE_ID, unbreakableGlue)
})

PlayerEvents.inventoryChanged(event => protectSuperGlue(event.item))
PlayerEvents.loggedIn(event => protectCarriedSuperGlue(event.player))
PlayerEvents.tick(event => {
  if (event.player.age % 20 === 0) protectCarriedSuperGlue(event.player)
})
ItemEvents.rightClicked(SUPER_GLUE_ID, event => protectSuperGlue(event.item))
