// Use Applied Create's kinetic energy acceptor as the pack's sole ME stress input.
const dwRetiredAe2EnergyAcceptors = [
  'ae2:energy_acceptor',
  'ae2:cable_energy_acceptor'
]

StartupEvents.modifyCreativeTab('ae2:main', event => {
  dwRetiredAe2EnergyAcceptors.forEach(id => event.remove(id))
})
