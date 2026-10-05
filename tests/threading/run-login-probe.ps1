param(
    [string] $ServerJar,
    [string] $JavaHome = $env:JAVA_HOME,
    [string] $RunRoot,
    [string] $ReuseRunDir,
    [string] $ExternalArtifactsDir,
    [ValidateSet('Create', 'Lithium', 'FerriteCore')] [string] $ExternalMod = 'Create',
    [switch] $AdditionalModernFix,
    [switch] $SplitPlayerRegions,
    [switch] $WithProbeArtifacts,
    [switch] $UseDefaultThreading,
    [ValidateRange(0, 3600)] [int] $HoldSeconds = 0
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
if (-not $ServerJar) {
    $ServerJar = (Get-ChildItem -LiteralPath (Join-Path $repo 'projects/youer/build/libs') -Filter 'TanukiMC-1.21.1-*-server.jar' |
        Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1).FullName
}
$java = Join-Path $JavaHome 'bin/java.exe'
$client = Join-Path $PSScriptRoot 'client-probe.cjs'
if (-not (Test-Path -LiteralPath $java) -or -not (Test-Path -LiteralPath $ServerJar)) {
    throw 'Build the server jar and provide Java 21 through JAVA_HOME or -JavaHome.'
}
if (-not (Test-Path -LiteralPath (Join-Path $repo 'build/threading-client/node_modules/minecraft-protocol'))) {
    throw 'Install the test client with npm install --prefix build/threading-client minecraft-protocol@1.68.0 --no-save --no-package-lock.'
}
if ($ExternalArtifactsDir -and -not $WithProbeArtifacts) {
    throw 'Use -WithProbeArtifacts when testing external MODs and plugins.'
}
if ($AdditionalModernFix -and (-not $ExternalArtifactsDir -or $ExternalMod -ne 'FerriteCore')) {
    throw 'Use -AdditionalModernFix with FerriteCore and -ExternalArtifactsDir.'
}
if ($SplitPlayerRegions -and -not $WithProbeArtifacts) {
    throw 'Use -SplitPlayerRegions with -WithProbeArtifacts.'
}

$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$listener.Start()
$port = ([System.Net.IPEndPoint] $listener.LocalEndpoint).Port
$listener.Stop()

if ($ReuseRunDir) {
    $runDir = (Resolve-Path -LiteralPath $ReuseRunDir).Path
    if (-not (Test-Path -LiteralPath "$runDir/world/level.dat")) {
        throw 'The reused probe directory must contain a saved world.'
    }
} else {
    if (-not $RunRoot) { $RunRoot = Join-Path $repo 'build/threading-login' }
    New-Item -ItemType Directory -Path $RunRoot -Force | Out-Null
    $runDir = Join-Path (Resolve-Path -LiteralPath $RunRoot).Path ([Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $runDir, "$runDir/youer-config" | Out-Null
}
Copy-Item -LiteralPath $ServerJar -Destination "$runDir/server.jar"
$configFixture = if ($UseDefaultThreading) { 'default-threading.yml' } else { 'youer.yml' }
Copy-Item -LiteralPath (Join-Path $PSScriptRoot $configFixture) -Destination "$runDir/youer-config/youer.yml"
if ($ExternalArtifactsDir -and $ExternalMod -eq 'Lithium') {
    $config = Get-Content -LiteralPath "$runDir/youer-config/youer.yml" -Raw
    $config = $config.Replace('  check_update: false', "  check_update: false`n  auto_delete_mods: false")
    Set-Content -LiteralPath "$runDir/youer-config/youer.yml" -Value $config -Encoding UTF8
}
if ($WithProbeArtifacts) {
    New-Item -ItemType Directory -Path "$runDir/plugins", "$runDir/mods" -Force | Out-Null
    $libs = Join-Path $repo 'projects/youer/build/libs'
    $plugin = Get-ChildItem -LiteralPath $libs -Filter 'threading-phase-plugin-*.jar' |
        Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
    $mod = Get-ChildItem -LiteralPath $libs -Filter 'threading-phase-mod-*.jar' |
        Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
    if (-not $plugin -or -not $mod) { throw 'Build the threading smoke plugin and mod jars first.' }
    Copy-Item -LiteralPath $plugin.FullName -Destination "$runDir/plugins/phase-probe.jar"
    Copy-Item -LiteralPath $mod.FullName -Destination "$runDir/mods/phase-probe.jar"
    if ($ExternalArtifactsDir) {
        $luckPerms = Join-Path $ExternalArtifactsDir 'LuckPerms-Bukkit-5.5.85.jar'
        $modJar = switch ($ExternalMod) {
            'Create' { Join-Path $ExternalArtifactsDir 'create-1.21.1-6.0.9-216.jar' }
            'Lithium' { Join-Path $ExternalArtifactsDir 'lithium-neoforge-0.15.2+mc1.21.1.jar' }
            'FerriteCore' { Join-Path $ExternalArtifactsDir 'ferritecore-7.0.3-neoforge.jar' }
        }
        if (-not (Test-Path -LiteralPath $modJar) -or -not (Test-Path -LiteralPath $luckPerms)) {
            throw 'The external artifact directory must contain the selected MOD and pinned LuckPerms jars.'
        }
        Copy-Item -LiteralPath $modJar -Destination "$runDir/mods/$($ExternalMod.ToLowerInvariant()).jar"
        Copy-Item -LiteralPath $luckPerms -Destination "$runDir/plugins/LuckPerms.jar"
        if ($AdditionalModernFix) {
            $modernFix = Join-Path $ExternalArtifactsDir 'modernfix-neoforge-5.27.24+mc1.21.1.jar'
            if (-not (Test-Path -LiteralPath $modernFix)) { throw 'The pinned ModernFix jar is missing.' }
            Copy-Item -LiteralPath $modernFix -Destination "$runDir/mods/modernfix.jar"
        }
    }
}
$properties = (Get-Content -LiteralPath "$PSScriptRoot/server.properties" -Raw).Replace('server-port=0', "server-port=$port")
Set-Content -LiteralPath "$runDir/server.properties" -Value $properties -Encoding ASCII
'eula=true' | Set-Content -LiteralPath "$runDir/eula.txt" -Encoding ASCII
Write-Output "Login probe server: $runDir (port $port)"

$javaArgs = @('-Xms512m', '-Xmx2g')
if ($WithProbeArtifacts) { $javaArgs += '-Dyouer.phase.wait_for_players=true' }
if ($SplitPlayerRegions) { $javaArgs += '-Dyouer.phase.split_player_regions=true' }
if ($ExternalArtifactsDir -and $ExternalMod -eq 'Create') { $javaArgs += '-Dyouer.phase.with_create=true' }
if ($ExternalArtifactsDir -and $ExternalMod -eq 'Lithium') { $javaArgs += '-Dyouer.phase.with_lithium=true' }
if ($ExternalArtifactsDir -and $ExternalMod -eq 'FerriteCore') { $javaArgs += '-Dyouer.phase.with_ferritecore=true' }
if ($AdditionalModernFix) { $javaArgs += '-Dyouer.phase.with_modernfix=true' }
if ($ExternalArtifactsDir) { $javaArgs += '-Dyouer.phase.with_luckperms=true' }
$javaArgs += @('-jar', 'server.jar', 'nogui')
$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = $java
$startInfo.Arguments = $javaArgs -join ' '
$startInfo.WorkingDirectory = $runDir
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo
$started = $false
$stdout = $null
$stderr = $null
$stdoutCopy = $null
$stderrCopy = $null
try {
    if (-not $process.Start()) { throw 'Could not start the login probe server.' }
    $started = $true
    $stdout = [System.IO.File]::Create("$runDir/stdout.log")
    $stderr = [System.IO.File]::Create("$runDir/stderr.log")
    $stdoutCopy = $process.StandardOutput.BaseStream.CopyToAsync($stdout)
    $stderrCopy = $process.StandardError.BaseStream.CopyToAsync($stderr)
    $deadline = [DateTime]::UtcNow.AddSeconds(120)
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($process.HasExited) { throw "Server exited early. Inspect $runDir" }
        $log = Join-Path $runDir 'logs/latest.log'
        if (Test-Path -LiteralPath $log) {
            $startupLog = Get-Content -LiteralPath $log -Raw
            $currentPort = $null -ne $startupLog -and $startupLog.Contains("Starting Minecraft server on 127.0.0.1:$port")
            if ($currentPort -and $startupLog -match 'Done \(') { break }
        }
        Start-Sleep -Seconds 1
    }
    if ([DateTime]::UtcNow -ge $deadline) { throw "Server startup timed out. Inspect $runDir" }
    $holdMs = if ($HoldSeconds -gt 0) { $HoldSeconds * 1000 }
        elseif ($SplitPlayerRegions) { 60000 }
        elseif ($WithProbeArtifacts) { 30000 }
        else { 10000 }
    & node $client $port $holdMs
    if ($LASTEXITCODE -ne 0) { throw "Login probe failed. Inspect $runDir" }
    $log = Join-Path $runDir 'logs/latest.log'
    if ($WithProbeArtifacts) {
        $phaseDeadline = [DateTime]::UtcNow.AddSeconds(90)
        while ([DateTime]::UtcNow -lt $phaseDeadline) {
            if ($process.HasExited) { throw "Server exited before the phase probe completed. Inspect $runDir" }
            $phaseLog = Get-Content -LiteralPath $log -Raw
            if ($phaseLog -match 'PHASE_PROBE (PASS|FAIL)') { break }
            Start-Sleep -Seconds 1
        }
    }
    $logText = Get-Content -LiteralPath $log -Raw
    if ($logText -notmatch 'This server is running TanukiMC version') {
        throw "Server brand was not TanukiMC. Inspect $runDir"
    }
    if ($logText -notmatch 'ThreadProbeA joined the game' -or $logText -notmatch 'ThreadProbeB joined the game') {
        throw "Both players did not join the server. Inspect $runDir"
    }
    if ($WithProbeArtifacts -and $logText -notmatch 'PHASE_PROBE PASS') {
        throw "The MOD and plugin phase probe did not pass. Inspect $runDir"
    }
    if ($SplitPlayerRegions -and $logText -notmatch 'splitPlayerCrossed=true') {
        throw "The distant player did not cross a region boundary. Inspect $runDir"
    }
    if ($ExternalArtifactsDir) {
        $luckPermsLoaded = $logText -match '\[LuckPerms\]: Successfully enabled'
        $modPassed = switch ($ExternalMod) {
            'Create' { $logText -match 'Create 6\.0\.9 initializing' -and $logText -match 'createBlocks=4' }
            'Lithium' { $logText -match 'lithiumLoaded=true' }
            'FerriteCore' { $logText -match 'ferriteCoreLoaded=true' }
        }
        if (-not ($luckPermsLoaded -and $modPassed)) {
            throw "External MOD/plugin login probe failed. Inspect $runDir"
        }
        if ($AdditionalModernFix -and $logText -notmatch 'modernFixLoaded=true') {
            throw "ModernFix did not load in the external MOD/plugin login probe. Inspect $runDir"
        }
    }
    if ($WithProbeArtifacts) {
        Write-Output ([regex]::Match($logText, 'PHASE_PROBE PASS[^\r\n]*').Value)
    }
    if ($logText -match 'Entity threw exception|Server thread/ERROR|Region Worker.*ERROR') {
        throw "Server logged an error during login. Inspect $runDir"
    }
    if ($logText -match 'PLAYER_PROBE_DEATH') {
        throw "A synthetic probe player died. Inspect $runDir"
    }
    $stopBytes = [System.Text.Encoding]::ASCII.GetBytes("`nstop`n")
    $process.StandardInput.BaseStream.Write($stopBytes, 0, $stopBytes.Length)
    $process.StandardInput.BaseStream.Flush()
    $process.StandardInput.Close()
    if (-not $process.WaitForExit(60000)) { throw "Server did not stop gracefully. Inspect $runDir" }
    $null = $stdoutCopy.GetAwaiter().GetResult()
    $null = $stderrCopy.GetAwaiter().GetResult()
    $shutdownLog = Get-Content -LiteralPath $log -Raw
    $stopped = $shutdownLog -match 'Stopping server'
    $saved = $shutdownLog -match 'Saving chunks for level'
    if ($process.ExitCode -ne 0 -or -not $stopped -or -not $saved) {
        throw "Server did not save and exit cleanly. Inspect $runDir"
    }
    Write-Output "SHUTDOWN_PROBE PASS exitCode=$($process.ExitCode)"
} finally {
    if ($started -and -not $process.HasExited) {
        try {
            if ($process.StandardInput.BaseStream.CanWrite) {
                $stopBytes = [System.Text.Encoding]::ASCII.GetBytes("`nstop`n")
                $process.StandardInput.BaseStream.Write($stopBytes, 0, $stopBytes.Length)
                $process.StandardInput.BaseStream.Flush()
            }
        } catch {
            # A failed probe may have already closed the server's input stream.
        }
        if (-not $process.WaitForExit(15000)) {
            $process.Kill()
            $process.WaitForExit()
        }
    }
    if ($stdoutCopy) { $null = $stdoutCopy.GetAwaiter().GetResult() }
    if ($stderrCopy) { $null = $stderrCopy.GetAwaiter().GetResult() }
    if ($stdout) { $stdout.Dispose() }
    if ($stderr) { $stderr.Dispose() }
    $process.Dispose()
}
