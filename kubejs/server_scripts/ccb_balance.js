const CCB_BALANCE_GAS_MULTIPLIER = 8
const CCB_BALANCE_MINIMUM_PRESSURE_PA = 200000

ServerEvents.recipes(event => {
  // Keep CCB's recipe fields intact while tightening its stress-free gas injection machine.
  event.forEachRecipe({ mod: 'createcraftedbeginning' }, recipe => {
    const json = recipe.json
    if (json.get('type').getAsString() !== 'createcraftedbeginning:gas_injection')
      return
    if (!json.has('gas_ingredients'))
      return

    const gasIngredients = json.getAsJsonArray('gas_ingredients')
    for (let index = 0; index < gasIngredients.size(); index++) {
      const requirement = gasIngredients.get(index).getAsJsonObject()
      const amount = requirement.get('amount').getAsLong()
      const existingMinimum = requirement.has('minimum_pressure')
        ? requirement.get('minimum_pressure').getAsLong()
        : 0

      requirement.addProperty('amount', amount * CCB_BALANCE_GAS_MULTIPLIER)
      // Raise the absolute recipe floor while the planner enforces pump boost relative to ambient.
      requirement.addProperty(
        'minimum_pressure',
        Math.max(existingMinimum, CCB_BALANCE_MINIMUM_PRESSURE_PA)
      )
    }
  })
})
