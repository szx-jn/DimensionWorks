#!/usr/bin/env node
// Packaging-time validation for kubejs/server_scripts/cave_self_propagation.js.
//
// Runs the KubeJS script against a stub Create recipe API and re-checks the
// rules the file promises:
//   1. every generated recipe id is unique
//   2. every <target>_primary recipe produces its target without consuming it
//   3. every <target>_recovery recipe consumes exactly 2 of its own dimension's residue
//
// Prints a single success line, or "SELF-PROPAGATION FAIL" plus the problems.

const fs = require('fs')
const path = require('path')
const vm = require('vm')

const ROOT = path.resolve(__dirname, '..')
const SCRIPT = path.join(ROOT, 'kubejs', 'server_scripts', 'cave_self_propagation.js')
const PREFIX = 'dimensionworks_cave_factory:self_propagation/'

const RESIDUE_BY_DIMENSION = {
  magnetic: 'dimensionworks_cave_factory:polarity_residue',
  primordial: 'dimensionworks_cave_factory:genesis_residue',
  toxic: 'dimensionworks_cave_factory:decay_residue',
  abyssal: 'dimensionworks_cave_factory:pressure_residue',
  forlorn: 'dimensionworks_cave_factory:umbral_residue',
  candy: 'dimensionworks_cave_factory:crystal_residue'
}

const RECIPE_TYPES = [
  'mixing',
  'compacting',
  'crushing',
  'milling',
  'filling',
  'haunting'
]

const problems = []
const recipes = []

function record(type, results, ingredients) {
  const entry = { type, results, ingredients, id: null }
  recipes.push(entry)
  return {
    id(value) {
      entry.id = value
      return entry
    }
  }
}

const namespace = {}
for (const type of RECIPE_TYPES) {
  namespace[type] = (results, ingredients) => record(type, results, ingredients)
}

const sandbox = {
  console,
  ServerEvents: { recipes: (callback) => callback({ recipes: { create: namespace } }) },
  Fluid: { of: (id, amount) => ({ fluid: id, amount }) },
  Item: { of: (id) => ({ item: id, withChance: (chance) => ({ item: id, chance }) }) }
}

vm.createContext(sandbox)
vm.runInContext(fs.readFileSync(SCRIPT, 'utf8'), sandbox, { filename: SCRIPT })

function itemId(entry) {
  if (entry && entry.item) return entry.item
  const match = /^(?:\d+x )?([a-z_]+:[a-z_0-9/]+)$/.exec(String(entry))
  return match ? match[1] : null
}

function itemCount(entry) {
  const match = /^(\d+)x /.exec(String(entry))
  return match ? Number(match[1]) : 1
}

const ids = new Set()
for (const recipe of recipes) {
  if (!recipe.id) {
    problems.push(`recipe without id: ${recipe.type}`)
    continue
  }
  if (!recipe.id.startsWith(PREFIX)) {
    problems.push(`${recipe.id}: outside ${PREFIX}`)
  }
  if (ids.has(recipe.id)) {
    problems.push(`duplicate recipe id: ${recipe.id}`)
  }
  ids.add(recipe.id)

  const path = recipe.id.slice(PREFIX.length)
  const parts = path.split('/')
  const isSupport = parts[0] === 'support'
  const dimension = isSupport ? parts[1] : parts[0]
  const name = parts[parts.length - 1]
  const ingredients = Array.isArray(recipe.ingredients) ? recipe.ingredients : [recipe.ingredients]
  const results = Array.isArray(recipe.results) ? recipe.results : [recipe.results]

  if (name.endsWith('_primary')) {
    const produced = results.map(itemId).filter(Boolean)
    for (const ingredient of ingredients) {
      const consumed = itemId(ingredient)
      if (consumed && produced.includes(consumed)) {
        problems.push(`${recipe.id}: primary recipe consumes its own product ${consumed}`)
      }
    }
  }

  if (name.endsWith('_recovery')) {
    const expected = RESIDUE_BY_DIMENSION[dimension]
    if (!expected) {
      problems.push(`${recipe.id}: unknown dimension ${dimension}`)
      continue
    }
    const consumed = ingredients
      .filter((ingredient) => itemId(ingredient) === expected)
      .reduce((total, ingredient) => total + itemCount(ingredient), 0)
    if (consumed !== 2) {
      problems.push(`${recipe.id}: consumes ${consumed}x ${expected}, expected 2`)
    }
  }
}

if (problems.length) {
  console.log('SELF-PROPAGATION FAIL')
  problems.forEach((problem) => console.log(`- ${problem}`))
  process.exit(1)
}

const primary = recipes.filter((recipe) => recipe.id.endsWith('_primary')).length
const recovery = recipes.filter((recipe) => recipe.id.endsWith('_recovery')).length
console.log(
  `SELF-PROPAGATION OK: ${recipes.length} recipes (${primary} primary, ${recovery} recovery, ${recipes.length - primary - recovery} support)`
)
