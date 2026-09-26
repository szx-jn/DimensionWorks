ItemEvents.rightClicked('kubejs:dimension_key', event => {
  if (event.hand.name() !== 'MAIN_HAND') {
    return
  }

  const player = event.player
  const unlockTag = 'dimensionworks_transfer.unlocked'
  const alreadyUnlocked = player.getTags().contains(unlockTag)

  if (alreadyUnlocked) {
    player.tell(Text.translate('message.kubejs.dimension_key.already_unlocked'))
    return
  }

  player.addTag(unlockTag)
  if (!player.getTags().contains(unlockTag)) {
    player.tell(Text.translate('message.kubejs.dimension_key.failed'))
    return
  }
  event.item.shrink(1)
  player.tell(Text.translate('message.kubejs.dimension_key.unlocked'))
})
