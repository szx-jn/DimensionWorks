// Keep Mekanism 10.4 JSON intact where the installed KubeJS addon cannot read it.
// Combining/crushing and other working typed schemas remain registered by the addon.
StartupEvents.recipeSchemaRegistry(event => {
  const mekanism = event.namespace('mekanism')
  ;['rotary', 'washing', 'reaction', 'separating', 'evaporating'].forEach(type => {
    mekanism.registerBasic(type)
  })
})
