param(
    [string] $ServerJar,
    [string] $JavaHome = $env:JAVA_HOME,
    [string] $ReuseRunDir,
    [string] $ExternalArtifactsDir,
    [switch] $AdditionalOptimizationMods,
    [switch] $AdditionalMekanism,
    [switch] $AdditionalWorldEdit,
    [switch] $UseDefaultThreading,
    [int] $StoredChunkX = -1,
    [int] $StoredChunkZ = -1,
    [int] $TimeoutSeconds = 240
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$libs = Join-Path $repo 'projects/youer/build/libs'
if (-not $ServerJar) {
    $ServerJar = (Get-ChildItem -LiteralPath $libs -Filter 'TanukiMC-1.21.1-*-server.jar' |
        Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1).FullName
}
$java = Join-Path $JavaHome 'bin/java.exe'
if (-not (Test-Path -LiteralPath $java)) {
    throw 'Set JAVA_HOME or pass -JavaHome with a Java 21 installation.'
}
$plugin = Get-ChildItem -LiteralPath $libs -Filter 'threading-phase-plugin-*.jar' |
    Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
$mod = Get-ChildItem -LiteralPath $libs -Filter 'threading-phase-mod-*.jar' |
    Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
if (-not $plugin -or -not $mod -or -not (Test-Path -LiteralPath $ServerJar)) {
    throw 'Build :youer:youerJar :youer:threadingSmokePluginJar :youer:threadingSmokeModJar first.'
}
if (($AdditionalOptimizationMods -or $AdditionalMekanism -or $AdditionalWorldEdit) -and -not $ExternalArtifactsDir) {
    throw 'Use additional MOD switches with -ExternalArtifactsDir.'
}
if (($StoredChunkX -ge 0) -ne ($StoredChunkZ -ge 0)) {
    throw 'Provide both stored chunk coordinates or neither.'
}

if ($ReuseRunDir) {
    $runDir = (Resolve-Path -LiteralPath $ReuseRunDir).Path
    if (-not (Test-Path -LiteralPath "$runDir/world/level.dat")) {
        throw 'The reused phase probe directory must contain a saved world.'
    }
    if (-not $AdditionalOptimizationMods -and
        ((Test-Path -LiteralPath "$runDir/mods/ferritecore.jar") -or (Test-Path -LiteralPath "$runDir/mods/modernfix.jar"))) {
        throw 'This reused directory contains optimization MODs. Use -AdditionalOptimizationMods or another directory.'
    }
    if (-not $AdditionalMekanism -and (Test-Path -LiteralPath "$runDir/mods/mekanism.jar")) {
        throw 'This reused directory contains Mekanism. Use -AdditionalMekanism or another directory.'
    }
    if (-not $AdditionalWorldEdit -and (Test-Path -LiteralPath "$runDir/plugins/WorldEdit.jar")) {
        throw 'This reused directory contains WorldEdit. Use -AdditionalWorldEdit or another directory.'
    }
} else {
    $runDir = Join-Path $repo ('build/threading-smoke/' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $runDir, "$runDir/plugins", "$runDir/mods", "$runDir/youer-config" | Out-Null
}
Copy-Item -LiteralPath $ServerJar -Destination "$runDir/server.jar"
Copy-Item -LiteralPath $plugin.FullName -Destination "$runDir/plugins/phase-probe.jar"
Copy-Item -LiteralPath $mod.FullName -Destination "$runDir/mods/phase-probe.jar"
if ($ExternalArtifactsDir) {
    $create = Join-Path $ExternalArtifactsDir 'create-1.21.1-6.0.9-216.jar'
    $luckPerms = Join-Path $ExternalArtifactsDir 'LuckPerms-Bukkit-5.5.85.jar'
    if (-not (Test-Path -LiteralPath $create) -or -not (Test-Path -LiteralPath $luckPerms)) {
        throw 'The external artifact directory must contain the pinned Create and LuckPerms jars.'
    }
    Copy-Item -LiteralPath $create -Destination "$runDir/mods/create.jar"
    Copy-Item -LiteralPath $luckPerms -Destination "$runDir/plugins/LuckPerms.jar"
    if ($AdditionalWorldEdit) {
        $worldEdit = Join-Path $ExternalArtifactsDir 'worldedit-bukkit-7.3.8.jar'
        if (-not (Test-Path -LiteralPath $worldEdit)) {
            throw 'The pinned WorldEdit jar is missing.'
        }
        Copy-Item -LiteralPath $worldEdit -Destination "$runDir/plugins/WorldEdit.jar"
    }
    if ($AdditionalOptimizationMods) {
        $ferriteCore = Join-Path $ExternalArtifactsDir 'ferritecore-7.0.3-neoforge.jar'
        $modernFix = Join-Path $ExternalArtifactsDir 'modernfix-neoforge-5.27.24+mc1.21.1.jar'
        if (-not (Test-Path -LiteralPath $ferriteCore) -or -not (Test-Path -LiteralPath $modernFix)) {
            throw 'The external artifact directory must contain the pinned FerriteCore and ModernFix jars.'
        }
        Copy-Item -LiteralPath $ferriteCore -Destination "$runDir/mods/ferritecore.jar"
        Copy-Item -LiteralPath $modernFix -Destination "$runDir/mods/modernfix.jar"
    }
    if ($AdditionalMekanism) {
        $mekanism = Join-Path $ExternalArtifactsDir 'Mekanism-1.21.1-10.7.19.85.jar'
        if (-not (Test-Path -LiteralPath $mekanism)) {
            throw 'The pinned Mekanism jar is missing.'
        }
        Copy-Item -LiteralPath $mekanism -Destination "$runDir/mods/mekanism.jar"
    }
}
Copy-Item -LiteralPath "$PSScriptRoot/server.properties" -Destination $runDir
$configFixture = if ($UseDefaultThreading) { 'default-threading.yml' } else { 'youer.yml' }
Copy-Item -LiteralPath (Join-Path $PSScriptRoot $configFixture) -Destination "$runDir/youer-config/youer.yml"
'eula=true' | Set-Content -LiteralPath "$runDir/eula.txt" -Encoding ASCII
Write-Output "Phase probe server: $runDir"
$javaArgs = @('-Xms512m', '-Xmx2g')
if ($ExternalArtifactsDir) { $javaArgs += '-Dyouer.phase.with_create=true' }
if ($AdditionalOptimizationMods) {
    $javaArgs += '-Dyouer.phase.with_ferritecore=true'
    $javaArgs += '-Dyouer.phase.with_modernfix=true'
}
if ($AdditionalMekanism) { $javaArgs += '-Dyouer.phase.with_mekanism=true' }
if ($AdditionalWorldEdit) { $javaArgs += '-Dyouer.phase.with_worldedit=true' }
if ($StoredChunkX -ge 0) {
    $javaArgs += "-Dyouer.phase.stored_chunk_x=$StoredChunkX"
    $javaArgs += "-Dyouer.phase.stored_chunk_z=$StoredChunkZ"
}
$javaArgs += @('-jar', 'server.jar', 'nogui')
$process = Start-Process -FilePath $java -ArgumentList $javaArgs `
    -WorkingDirectory $runDir -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput "$runDir/stdout.log" -RedirectStandardError "$runDir/stderr.log"
$null = $process.Handle
try {
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    while (-not $process.WaitForExit(1000)) {
        if ([DateTime]::UtcNow -gt $deadline) {
            throw "Phase probe timed out. Inspect $runDir"
        }
    }
    $process.WaitForExit()
    $output = Get-Content -LiteralPath "$runDir/stdout.log" -Raw
    $markers = [regex]::Matches($output, 'PHASE_PROBE (PASS|FAIL)[^\r\n]*')
    foreach ($marker in $markers) {
        Write-Output $marker.Value
    }
    if ($markers.Count -ne 1 -or $markers[0].Groups[1].Value -ne 'PASS' -or $process.ExitCode -ne 0) {
        throw "Phase probe failed (exit $($process.ExitCode)). Inspect $runDir"
    }
    if ($ExternalArtifactsDir) {
        $log = Get-Content -LiteralPath "$runDir/logs/latest.log" -Raw
        $createLoaded = $log -match 'Create 6\.0\.9 initializing'
        $luckPermsLoaded = $log -match '\[LuckPerms\]: Successfully enabled'
        $createBlocksTicked = $markers[0].Value -match 'createBlocks=4'
        $optimizationsLoaded = -not $AdditionalOptimizationMods -or (
            $markers[0].Value -match 'ferriteCoreLoaded=true' -and $markers[0].Value -match 'modernFixLoaded=true')
        $mekanismTicked = -not $AdditionalMekanism -or ($markers[0].Value -match 'mekanismTickRegions=4')
        $worldEditWorked = -not $AdditionalWorldEdit -or (
            $markers[0].Value -match 'worldEditBlocks=4' -and
            $markers[0].Value -match 'worldEditRegionEdits=4' -and
            $log -match 'Enabling WorldEdit v7\.3\.8')
        $serverError = $log -match '/ERROR\]'
        if (-not ($createLoaded -and $luckPermsLoaded -and $createBlocksTicked -and $optimizationsLoaded -and $mekanismTicked -and $worldEditWorked) -or $serverError) {
            throw "External MOD/plugin smoke test failed. Inspect $runDir"
        }
    }
} finally {
    if (-not $process.HasExited) {
        $process.Kill()
        $process.WaitForExit()
    }
    $process.Dispose()
}
