const mc = require('../../build/threading-client/node_modules/minecraft-protocol')

const port = Number(process.argv[2])
if (!Number.isInteger(port) || port < 1 || port > 65535) {
  throw new Error('Pass the server port as the first argument')
}
const holdMs = Number(process.argv[3] || 10000)
if (!Number.isInteger(holdMs) || holdMs < 1000) {
  throw new Error('Pass a hold duration of at least 1000 ms')
}

const names = ['ThreadProbeA', 'ThreadProbeB']
const clients = []
const joined = new Set()
const positioned = new Set()
const moved = new Set()
const dug = new Set()
const movementTimers = new Map()
const positionTimers = new Map()
const digTimers = new Map()
let finished = false

function finish(error) {
  if (finished) return
  finished = true
  clearTimeout(timeout)
  for (const timer of movementTimers.values()) clearInterval(timer)
  for (const timer of positionTimers.values()) clearTimeout(timer)
  for (const timer of digTimers.values()) clearTimeout(timer)
  for (const client of clients) client.end()
  if (error) {
    console.error(`LOGIN_PROBE FAIL ${error.message}`)
    process.exitCode = 1
  } else {
    console.log(`LOGIN_PROBE PASS players=${joined.size} positioned=${positioned.size} moved=${moved.size} dug=${dug.size}`)
  }
  setTimeout(() => process.exit(), 1000)
}

const timeout = setTimeout(() => finish(new Error(`Timed out; joined=${joined.size} positioned=${positioned.size} moved=${moved.size}`)), Math.max(60000, holdMs + 30000))

for (const username of names) {
  const client = mc.createClient({ host: '127.0.0.1', port, version: '1.21.1', username, auth: 'offline' })
  clients.push(client)
  client.on('ping', packet => client.write('pong', { id: packet.id }))
  client.on('position', packet => {
    console.log(`LOGIN_PROBE_POSITION player=${username} id=${packet.teleportId} x=${packet.x}`)
    client.write('teleport_confirm', { teleportId: packet.teleportId })
    clearTimeout(positionTimers.get(username))
    clearInterval(movementTimers.get(username))
    clearTimeout(digTimers.get(username))
    positioned.delete(username)
    moved.delete(username)
    dug.delete(username)
    positionTimers.set(username, setTimeout(() => {
      if (finished) return
      positioned.add(username)
      const targetX = (Math.floor(packet.x / 16) + 1) * 16 + 1
      console.log(`LOGIN_PROBE_TARGET player=${username} from=${packet.x},${packet.y},${packet.z} toX=${targetX}`)
      const steps = Math.ceil((targetX - packet.x) / 0.5)
      let step = 0
      const timer = setInterval(() => {
        step++
        client.write('position_look', {
          x: Math.min(targetX, packet.x + step * 0.5),
          y: packet.y,
          z: packet.z,
          yaw: packet.yaw,
          pitch: packet.pitch,
          onGround: true
        })
        if (step === steps) {
          clearInterval(timer)
          moved.add(username)
          const location = { x: Math.floor(targetX), y: Math.floor(packet.y) - 1, z: Math.floor(packet.z) }
          client.write('block_dig', { status: 0, location, face: 1, sequence: 1 })
          digTimers.set(username, setTimeout(() => {
            if (finished) return
            client.write('block_dig', { status: 2, location, face: 1, sequence: 2 })
            dug.add(username)
          }, 1200))
        }
      }, 100)
      movementTimers.set(username, timer)
    }, 500))
  })
  client.on('playerJoin', () => {
    joined.add(username)
    if (joined.size === names.length) setTimeout(() => {
      if (positioned.size !== names.length || moved.size !== names.length || dug.size !== names.length) {
        finish(new Error(`Player actions incomplete; positioned=${positioned.size} moved=${moved.size} dug=${dug.size}`))
      } else {
        finish()
      }
    }, holdMs)
  })
  client.on('error', error => finish(error))
  client.on('end', reason => {
    if (!finished) finish(new Error(`${username} disconnected: ${reason}`))
  })
  client.on('kick_disconnect', packet => finish(new Error(`${username} kicked: ${JSON.stringify(packet)}`)))
}
