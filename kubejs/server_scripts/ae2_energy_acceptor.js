ServerEvents.recipes(event => {
  // Retire the block and cable variants; recipes that used them now use the
  // Applied Create kinetic receiver as the ME network's power interface.
  event.remove({ output: 'ae2:energy_acceptor' })
  event.remove({ output: 'ae2:cable_energy_acceptor' })
  event.replaceInput({}, 'ae2:energy_acceptor', 'appliedcreate:kinetic_energy_acceptor')
  event.replaceInput({}, 'ae2:cable_energy_acceptor', 'appliedcreate:kinetic_energy_acceptor')
})
