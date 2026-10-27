// DimensionWorks unified crushing/milling recipe API.
// Shared contract: machine + input + [{ item, count, chance }].
// Chance is per output entry. Use a guaranteed base output plus a probabilistic bonus
// when the desired outcomes are mutually exclusive (e.g. 2 dust + 70% chance for +1 dust).

(function () {
  const adapters = {}
  const registeredIds = {}

  function fail(message) {
    throw new Error('[DimensionWorks Crushing] ' + message)
  }

  function normalizeRecipe(recipe) {
    if (!recipe || typeof recipe !== 'object') fail('recipe must be an object')
    if (typeof recipe.id !== 'string' || !/^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(recipe.id)) {
      fail('id must be a namespaced recipe id')
    }
    if (typeof recipe.machine !== 'string' || !recipe.machine) fail('machine is required')
    if (typeof recipe.input !== 'string' || !recipe.input) {
      fail('input must be a single item id or tag string supported by the selected machine')
    }
    if (recipe.processingTime !== undefined &&
        (!Number.isInteger(Number(recipe.processingTime)) || Number(recipe.processingTime) < 1)) {
      fail('processingTime must be a positive integer')
    }
    if (!Array.isArray(recipe.outputs) || recipe.outputs.length === 0) {
      fail('outputs must be a non-empty array')
    }

    const outputs = recipe.outputs.map((output, index) => {
      if (!output || typeof output.item !== 'string' || !output.item) {
        fail('outputs[' + index + '].item is required')
      }
      const count = output.count === undefined ? 1 : Number(output.count)
      const chance = output.chance === undefined ? 1 : Number(output.chance)
      if (!Number.isInteger(count) || count < 1) fail('outputs[' + index + '].count must be a positive integer')
      if (!(chance > 0 && chance <= 1)) fail('outputs[' + index + '].chance must be in (0, 1]')
      return { item: output.item, count: count, chance: chance }
    })

    return {
      id: recipe.id,
      machine: recipe.machine,
      input: recipe.input,
      outputs: outputs,
      processingTime: recipe.processingTime
    }
  }

  function createOutputStack(output) {
    const stack = Item.of(output.item, output.count)
    return output.chance < 1 ? stack.withChance(output.chance) : stack
  }

  function createAdapter(recipeType) {
    return {
      register: function (event, recipe) {
        const outputs = recipe.outputs.map(createOutputStack)
        let builder
        if (recipeType === 'crushing') {
          builder = event.recipes.create.crushing(outputs, recipe.input)
        } else {
          builder = event.recipes.create.milling(outputs, recipe.input)
        }
        if (recipe.processingTime !== undefined) builder.processingTime(recipe.processingTime)
        return builder.id(recipe.id)
      }
    }
  }

  adapters['create:crushing'] = createAdapter('crushing')
  adapters['create:milling'] = createAdapter('milling')

  adapters['mekanism:crushing'] = {
    register: function (event, recipe) {
      if (recipe.outputs.length !== 1 || recipe.outputs[0].chance !== 1) {
        fail('mekanism:crushing only supports one guaranteed output; use a chance-capable adapter for probabilistic outputs')
      }
      const output = recipe.outputs[0]
      return event.recipes.mekanism.crushing(output.count + 'x ' + output.item, recipe.input).id(recipe.id)
    }
  }

  function registerAdapter(machine, adapter) {
    if (typeof machine !== 'string' || !machine) fail('adapter name is required')
    if (!adapter || typeof adapter.register !== 'function') fail('adapter must provide register(event, recipe)')
    if (Object.prototype.hasOwnProperty.call(adapters, machine)) fail('adapter already registered: ' + machine)
    adapters[machine] = adapter
  }

  function register(event, recipeInput) {
    const recipe = normalizeRecipe(recipeInput)
    const adapter = adapters[recipe.machine]
    if (!adapter) {
      fail('unsupported machine "' + recipe.machine + '"; available adapters: ' + Object.keys(adapters).sort().join(', '))
    }
    if (Object.prototype.hasOwnProperty.call(registeredIds, recipe.id)) fail('duplicate recipe id: ' + recipe.id)
    const result = adapter.register(event, recipe)
    registeredIds[recipe.id] = true
    return result
  }

  global.DimensionWorksCrushing = {
    register: register,
    registerAdapter: registerAdapter,
    getAdapters: function () {
      return Object.keys(adapters).sort()
    }
  }

  console.info('[DimensionWorks] Crushing API ready adapters=' + Object.keys(adapters).sort().join(','))
})()
