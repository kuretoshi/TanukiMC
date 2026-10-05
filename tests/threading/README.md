# TanukiMC Threading Tests

These fixtures exercise region ownership, scheduling, Minecraft tick queues, MOD block entities, plugin operations, client login, and persistence. Generated jars, dependencies, worlds, and logs belong under the ignored `build/` directory.

## Core Checks

With JDK 21 configured:

```powershell
.\gradlew :youer:threadingTopologyTest :youer:youerJar :youer:threadingSmokePluginJar :youer:threadingSmokeModJar :youer:threadingSmokeClientModJar
```

`threadingTopologyTest` includes event queues, scheduled ticks, chunk lifecycle, region execution, random sources, neighbor updates, task cancellation, and topology partitioning.

## Server MOD and Plugin Probe

```powershell
.\tests\threading\run-phase-probe.ps1
```

The runner creates an isolated localhost, offline-mode flat-world server and expects `PHASE_PROBE PASS`. It places test blocks and entities, checks parallel execution and ownership, and stops the server. `-ReuseRunDir` reuses a previously saved test world.

Optional external artifacts are supplied through `-ExternalArtifactsDir`; see the parameters and pinned filenames in `run-phase-probe.ps1`. Third-party MOD and plugin jars are not committed to this repository.

## Synthetic Client Probe

Install the protocol test dependency into the ignored build directory:

```powershell
npm install --prefix build/threading-client minecraft-protocol@1.68.0 --no-save --no-package-lock
.\tests\threading\run-login-probe.ps1 -WithProbeArtifacts -SplitPlayerRegions -HoldSeconds 600
```

This checks two clients, movement, block breaking, region crossings, and clean shutdown. The protocol client can emit registry parsing warnings for modded packets; use an actual NeoForge client as well when verifying MOD registry compatibility.

## Actual NeoForge Client Probe

`run-neoforge-client-probe.py` requires a prepared NeoForge 21.1.252 client installation in `build/threading-client-neoforge/minecraft` and `minecraft-launcher-lib` in `build/threading-client-neoforge/python`. Install the NeoForge client using the official installer into that isolated directory, then install vanilla 1.21.1 assets using the launcher library. The current runner expects Java at `C:\Program Files\Java\jdk-21\bin\java.exe`.

The launcher library used during validation was installed with:

```powershell
py -3.12 -m pip install --target build/threading-client-neoforge/python minecraft-launcher-lib==8.0
```

With the pinned external artifacts available under `build/threading-external/artifacts`:

```powershell
py -3.12 tests/threading/run-neoforge-client-probe.py --hybrid-mods --region-phase --optimization-mods --exercise-mod-blocks --hold-seconds 600
```

This uses a development test account against a localhost offline-mode server. It launches a visible game window; first-run accessibility prompts may need dismissal. `--region-phase` installs separate server and client test MOD jars with matching registry keys. The client fixture contains no Bukkit dependencies. `--op-client` enables manual player-command checks and removes the temporary operator permission on shutdown.

The region phase is a finite parallel workload followed by a connection hold, not a claim of ten minutes of continuously repeated peak load. A PASS requires client login, the requested region probe, MOD block checks, no server ERROR logs, and a clean server save and exit. The test directories are reused between runs and are separate from the user's normal `.minecraft` installation.
