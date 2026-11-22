// Both glue items use vanilla durability. Preserve other components on existing stacks.
const GLUE_IDS = ['create:super_glue', 'simulated:honey_glue']
const $DWGlueComponents = Java.loadClass('net.minecraft.core.component.DataComponents')

function protectGlue(stack) {
  if (!stack || !GLUE_IDS.includes(stack.id)) return false
  if (stack.getComponents().has($DWGlueComponents.UNBREAKABLE)) return false
  stack.setUnbreakableWithTooltip()
  return true
}

function protectCarriedSuperGlue(player) {
  let changed = false
  for (let slot = 0; slot <= 40; slot++) {
    if (protectGlue(player.inventory.getStackInSlot(slot))) changed = true
  }
  if (changed) player.sendInventoryUpdate()
}

ServerEvents.recipes(event => {
  // Applies to recipe outputs, including machine recipes supported by KubeJS.
  for (const glueId of GLUE_IDS) {
    const unbreakableGlue = Item.of(glueId)
    unbreakableGlue.setUnbreakableWithTooltip()
    event.replaceOutput({}, glueId, unbreakableGlue)
  }
})

PlayerEvents.inventoryChanged(event => protectGlue(event.item))
PlayerEvents.loggedIn(event => protectCarriedSuperGlue(event.player))
PlayerEvents.tick(event => {
  if (event.player.age % 20 === 0) protectCarriedSuperGlue(event.player)
})
for (const glueId of GLUE_IDS) {
  ItemEvents.rightClicked(glueId, event => protectGlue(event.item))
}
