const AETHER_EQUIPMENT = new Set([
  // Tools and weapons
  'aether:candy_cane_sword',
  'aether:cloud_staff',
  'aether:enchanted_dart',
  'aether:enchanted_dart_shooter',
  'aether:flaming_sword',
  'aether:golden_dart',
  'aether:golden_dart_shooter',
  'aether:gravitite_axe',
  'aether:gravitite_hoe',
  'aether:gravitite_pickaxe',
  'aether:gravitite_shovel',
  'aether:gravitite_sword',
  'aether:hammer_of_kingbdogz',
  'aether:holy_sword',
  'aether:holystone_axe',
  'aether:holystone_hoe',
  'aether:holystone_pickaxe',
  'aether:holystone_shovel',
  'aether:holystone_sword',
  'aether:lightning_knife',
  'aether:lightning_sword',
  'aether:nature_staff',
  'aether:phoenix_bow',
  'aether:pig_slayer',
  'aether:poison_dart',
  'aether:poison_dart_shooter',
  'aether:skyroot_axe',
  'aether:skyroot_hoe',
  'aether:skyroot_pickaxe',
  'aether:skyroot_shovel',
  'aether:skyroot_sword',
  'aether:valkyrie_axe',
  'aether:valkyrie_hoe',
  'aether:valkyrie_lance',
  'aether:valkyrie_pickaxe',
  'aether:valkyrie_shovel',
  'aether:vampire_blade',
  'aether:zanite_axe',
  'aether:zanite_hoe',
  'aether:zanite_pickaxe',
  'aether:zanite_shovel',
  'aether:zanite_sword',

  // Armor
  'aether:gravitite_boots',
  'aether:gravitite_chestplate',
  'aether:gravitite_helmet',
  'aether:gravitite_leggings',
  'aether:neptune_boots',
  'aether:neptune_chestplate',
  'aether:neptune_helmet',
  'aether:neptune_leggings',
  'aether:obsidian_boots',
  'aether:obsidian_chestplate',
  'aether:obsidian_helmet',
  'aether:obsidian_leggings',
  'aether:phoenix_boots',
  'aether:phoenix_chestplate',
  'aether:phoenix_helmet',
  'aether:phoenix_leggings',
  'aether:sentry_boots',
  'aether:valkyrie_boots',
  'aether:valkyrie_chestplate',
  'aether:valkyrie_helmet',
  'aether:valkyrie_leggings',
  'aether:zanite_boots',
  'aether:zanite_chestplate',
  'aether:zanite_helmet',
  'aether:zanite_leggings',

  // Accessories
  'aether:agility_cape',
  'aether:blue_cape',
  'aether:chainmail_gloves',
  'aether:diamond_gloves',
  'aether:golden_feather',
  'aether:golden_gloves',
  'aether:golden_pendant',
  'aether:golden_ring',
  'aether:gravitite_gloves',
  'aether:ice_pendant',
  'aether:ice_ring',
  'aether:invisibility_cloak',
  'aether:iron_bubble',
  'aether:iron_gloves',
  'aether:iron_pendant',
  'aether:iron_ring',
  'aether:leather_gloves',
  'aether:neptune_gloves',
  'aether:netherite_gloves',
  'aether:obsidian_gloves',
  'aether:phoenix_gloves',
  'aether:red_cape',
  'aether:regeneration_stone',
  'aether:shield_of_repulsion',
  'aether:swet_cape',
  'aether:valkyrie_cape',
  'aether:valkyrie_gloves',
  'aether:white_cape',
  'aether:yellow_cape',
  'aether:zanite_gloves',
  'aether:zanite_pendant',
  'aether:zanite_ring',

  // Deployable equipment
  'aether:cold_parachute',
  'aether:golden_parachute'
])

function isLockedEquipment(stack) {
  return stack && AETHER_EQUIPMENT.has(stack.id)
}

function purgeEquipmentSlots(player, firstSlot, lastSlot) {
  const inventory = player.inventory
  let removed = false

  for (let slot = firstSlot; slot <= lastSlot; slot++) {
    const stack = inventory.getStackInSlot(slot)
    if (isLockedEquipment(stack)) {
      stack.setCount(0)
      removed = true
    }
  }

  if (removed) {
    player.sendInventoryUpdate()
  }
}

ServerEvents.recipes(event => {
  AETHER_EQUIPMENT.forEach(id => {
    event.remove({ output: id })
    event.remove({ id: `${id}_repairing` })
  })
})

ItemEvents.canPickUp(event => {
  if (isLockedEquipment(event.item)) {
    event.cancel()
  }
})

ItemEvents.rightClicked(event => {
  if (isLockedEquipment(event.item)) {
    event.cancel()
  }
})

PlayerEvents.inventoryChanged(event => {
  if (isLockedEquipment(event.item)) {
    event.item.setCount(0)
  }
})

PlayerEvents.loggedIn(event => {
  purgeEquipmentSlots(event.player, 0, 40)
})

PlayerEvents.tick(event => {
  if (event.player.age % 20 === 0) {
    purgeEquipmentSlots(event.player, 36, 40)
  }
})
