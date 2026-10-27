// DimensionWorks shared crushing/grinding API.
// Supported semantics: material crushing and milling, plus CEI mechanical grinding.
// Recipe IDs are explicit; unsupported machine capabilities fail loudly instead of being ignored.
(function () {
  const adapters = {}
  const registeredIds = {}

  function fail(message) {
    throw new Error('[DimensionWorks Grinding] ' + message)
  }

  function normalizeFluid(value, name) {
    if (value === undefined || value === null) return null
    if (!value || typeof value.fluid !== 'string' || !value.fluid) {
      fail(name + '.fluid must be a fluid id')
    }
    const amount = Number(value.amount)
    if (!Number.isInteger(amount) || amount < 1) {
      fail(name + '.amount must be a positive integer')
    }
    return { fluid: value.fluid, amount: amount }
  }

  function normalizeRecipe(recipe) {
    if (!recipe || typeof recipe !== 'object') fail('recipe must be an object')
    if (typeof recipe.id !== 'string' || !/^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(recipe.id)) {
      fail('id must be a namespaced recipe id')
    }
    if (typeof recipe.machine !== 'string' || !recipe.machine) fail('machine is required')
    if (typeof recipe.input !== 'string' || !recipe.input) {
      fail('input must be a single item id or #tag string supported by the selected machine')
    }
    if (recipe.processingTime !== undefined &&
        (!Number.isInteger(Number(recipe.processingTime)) || Number(recipe.processingTime) < 1)) {
      fail('processingTime must be a positive integer')
    }

    const outputs = (recipe.outputs === undefined ? [] : recipe.outputs).map((output, index) => {
      if (!output || typeof output.item !== 'string' || !output.item) {
        fail('outputs[' + index + '].item is required')
      }
      const count = output.count === undefined ? 1 : Number(output.count)
      const chance = output.chance === undefined ? 1 : Number(output.chance)
      if (!Number.isInteger(count) || count < 1) fail('outputs[' + index + '].count must be a positive integer')
      if (!(chance > 0 && chance <= 1)) fail('outputs[' + index + '].chance must be in (0, 1]')
      return { item: output.item, count: count, chance: chance }
    })

    const fluidInput = normalizeFluid(recipe.fluidInput, 'fluidInput')
    const fluidOutput = normalizeFluid(recipe.fluidOutput, 'fluidOutput')
    const isCreateGrinding = recipe.machine === 'create_enchantment_industry:grinding'

    if (outputs.length === 0 && !fluidOutput) fail('at least one item output or a fluidOutput is required')
    if (isCreateGrinding && outputs.length > 4) fail('Create Enchantment Industry grinding supports up to four item outputs')
    if (isCreateGrinding && fluidInput && fluidOutput) {
      fail('Create Enchantment Industry grinding supports either one fluid input or one fluid output, not both')
    }
    if (!isCreateGrinding && (fluidInput || fluidOutput)) {
      fail('fluid inputs/outputs are only supported by create_enchantment_industry:grinding')
    }

    return {
      id: recipe.id,
      machine: recipe.machine,
      input: recipe.input,
      outputs: outputs,
      fluidInput: fluidInput,
      fluidOutput: fluidOutput,
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
        if (recipe.fluidInput || recipe.fluidOutput) {
          fail(recipe.machine + ' does not support fluid input/output')
        }
        if (recipe.outputs.length === 0) fail(recipe.machine + ' requires item outputs')
        const outputs = recipe.outputs.map(createOutputStack)
        let builder
        if (recipeType === 'crushing') {
          builder = event.recipes.create.crushing(outputs, recipe.input)
        } else {
          builder = event.recipes.create.milling(outputs, recipe.input)
        }
        if (recipe.processingTime !== undefined) builder.processingTime(Number(recipe.processingTime))
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

  // The installed Create: Enchantment Industry version has its own Grinding recipe type.
  // It can yield item outputs, one fluid output, or consume one fluid input; it is not
  // interchangeable with Create's Crushing Wheels or Millstone recipe types.
  adapters['create_enchantment_industry:grinding'] = {
    register: function (event, recipe) {
      const ingredients = [
        recipe.input.charAt(0) === '#'
          ? { tag: recipe.input.slice(1) }
          : { item: recipe.input }
      ]
      if (recipe.fluidInput) {
        // Create 6.0.8 processing recipes encode fluid ingredients as { fluid, amount }.
        ingredients.push({
          amount: recipe.fluidInput.amount,
          fluid: recipe.fluidInput.fluid
        })
      }

      const results = recipe.outputs.map(output => {
        const result = { item: output.item }
        if (output.count !== 1) result.count = output.count
        if (output.chance < 1) result.chance = output.chance
        return result
      })
      if (recipe.fluidOutput) {
        results.push({
          fluid: recipe.fluidOutput.fluid,
          amount: recipe.fluidOutput.amount
        })
      }

      const json = {
        type: 'create_enchantment_industry:grinding',
        ingredients: ingredients,
        results: results
      }
      if (recipe.processingTime !== undefined) json.processing_time = Number(recipe.processingTime)
      return event.custom(json).id(recipe.id)
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

  const api = {
    register: register,
    registerAdapter: registerAdapter,
    getAdapters: function () {
      return Object.keys(adapters).sort()
    }
  }

  // The Grinding name is the generic API. Keep Crushing as an alias for existing scripts.
  global.DimensionWorksGrinding = api
  global.DimensionWorksCrushing = api
  console.info('[DimensionWorks] Grinding API ready adapters=' + Object.keys(adapters).sort().join(','))
})()
