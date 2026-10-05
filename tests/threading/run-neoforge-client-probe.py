"""Launch an isolated NeoForge client against a local TanukiMC test server."""

import argparse
import shutil
import socket
import subprocess
import sys
import time
from pathlib import Path


REPO = Path(__file__).resolve().parents[2]
TEST_ROOT = REPO / "build" / "threading-client-neoforge"
MINECRAFT = TEST_ROOT / "minecraft"
sys.path.insert(0, str(TEST_ROOT / "python"))

import minecraft_launcher_lib as launcher  # noqa: E402


def free_port():
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        return listener.getsockname()[1]


def wait_for(process, log_path, marker, timeout):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise RuntimeError(f"Process exited with code {process.returncode}; inspect {log_path}")
        if log_path.exists() and marker in log_path.read_text(encoding="utf-8", errors="replace"):
            return
        time.sleep(1)
    raise TimeoutError(f"Timed out waiting for {marker!r}; inspect {log_path}")


def stop(process, server=False):
    if process.poll() is not None:
        return
    if server:
        try:
            process.stdin.write(b"stop\n")
            process.stdin.flush()
            process.wait(timeout=60)
            return
        except (BrokenPipeError, subprocess.TimeoutExpired):
            pass
    process.terminate()
    try:
        process.wait(timeout=20)
    except subprocess.TimeoutExpired:
        process.kill()
        process.wait(timeout=20)


def send_command(server, command):
    server.stdin.write((command + "\n").encode("ascii"))
    server.stdin.flush()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--hold-seconds", type=int, default=20)
    parser.add_argument("--join-timeout", type=int, default=360)
    parser.add_argument("--hybrid-mods", action="store_true")
    parser.add_argument("--exercise-mod-blocks", action="store_true")
    parser.add_argument("--op-client", action="store_true")
    parser.add_argument("--region-phase", action="store_true")
    parser.add_argument("--optimization-mods", action="store_true")
    args = parser.parse_args()
    if not 1 <= args.hold_seconds <= 600:
        parser.error("--hold-seconds must be between 1 and 600")
    if not 30 <= args.join_timeout <= 900:
        parser.error("--join-timeout must be between 30 and 900")
    if args.exercise_mod_blocks and not args.hybrid_mods:
        parser.error("--exercise-mod-blocks requires --hybrid-mods")
    if args.region_phase and not args.hybrid_mods:
        parser.error("--region-phase requires --hybrid-mods")
    if args.optimization_mods and not args.hybrid_mods:
        parser.error("--optimization-mods requires --hybrid-mods")
    suffix = "-phase" if args.region_phase else "" if args.hybrid_mods else "-basic"
    if args.optimization_mods:
        suffix += "-opt"
    game = TEST_ROOT / f"game{suffix}"
    server_dir = TEST_ROOT / f"server{suffix}"

    jars = sorted((REPO / "projects" / "youer" / "build" / "libs").glob("TanukiMC-1.21.1-*-server.jar"))
    if not jars:
        raise FileNotFoundError("Build :youer:youerJar first")
    java = Path(r"C:\Program Files\Java\jdk-21\bin\java.exe")
    luckperms = REPO / "build" / "threading-external" / "artifacts" / "LuckPerms-Bukkit-5.5.85.jar"
    if not java.exists() or not luckperms.exists():
        raise FileNotFoundError("Java 21 or the pinned LuckPerms jar is missing")

    vanilla = MINECRAFT / "versions" / "1.21.1"
    if not (vanilla / "1.21.1.json").exists() or not (vanilla / "1.21.1.jar").exists():
        launcher.install.install_minecraft_version("1.21.1", MINECRAFT)
    game.mkdir(parents=True, exist_ok=True)
    if (args.region_phase or args.optimization_mods) and not (game / "options.txt").exists():
        shutil.copy2(TEST_ROOT / "game" / "options.txt", game / "options.txt")
    (server_dir / "plugins").mkdir(parents=True, exist_ok=True)
    shutil.copy2(jars[-1], server_dir / "server.jar")
    shutil.copy2(luckperms, server_dir / "plugins" / "LuckPerms.jar")
    if args.hybrid_mods:
        artifacts = REPO / "build" / "threading-external" / "artifacts"
        for filename in ("create-1.21.1-6.0.9-216.jar", "Mekanism-1.21.1-10.7.19.85.jar"):
            source = artifacts / filename
            if not source.exists():
                raise FileNotFoundError(source)
            for mods in (server_dir / "mods", game / "mods"):
                mods.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, mods / filename)
        worldedit = artifacts / "worldedit-bukkit-7.3.8.jar"
        if not worldedit.exists():
            raise FileNotFoundError(worldedit)
        shutil.copy2(worldedit, server_dir / "plugins" / "WorldEdit.jar")
    if args.optimization_mods:
        artifacts = REPO / "build" / "threading-external" / "artifacts"
        for filename in ("ferritecore-7.0.3-neoforge.jar", "modernfix-neoforge-5.27.24+mc1.21.1.jar"):
            source = artifacts / filename
            if not source.exists():
                raise FileNotFoundError(source)
            for mods in (server_dir / "mods", game / "mods"):
                mods.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, mods / filename)
    if args.region_phase:
        libs = REPO / "projects" / "youer" / "build" / "libs"
        phase_plugin = next(iter(sorted(libs.glob("threading-phase-plugin-*.jar"))), None)
        phase_mod = next(iter(sorted(libs.glob("threading-phase-mod-*.jar"))), None)
        phase_client_mod = next(iter(sorted(libs.glob("threading-phase-client-mod-*.jar"))), None)
        if phase_plugin is None or phase_mod is None or phase_client_mod is None:
            raise FileNotFoundError("Build the phase probe plugin, server mod, and client mod jars first")
        shutil.copy2(phase_plugin, server_dir / "plugins" / "phase-probe.jar")
        (server_dir / "mods").mkdir(parents=True, exist_ok=True)
        shutil.copy2(phase_mod, server_dir / "mods" / "phase-probe.jar")
        (game / "mods").mkdir(parents=True, exist_ok=True)
        shutil.copy2(phase_client_mod, game / "mods" / "phase-probe.jar")
    port = free_port()
    fixture = (REPO / "tests" / "threading" / "server.properties").read_text(encoding="ascii")
    (server_dir / "server.properties").write_text(fixture.replace("server-port=0", f"server-port={port}"), encoding="ascii")
    (server_dir / "eula.txt").write_text("eula=true\n", encoding="ascii")

    options = launcher.utils.generate_test_options()
    options.update({
        "username": "TanukiProbe",
        "executablePath": str(java),
        "gameDirectory": str(game),
        "server": "127.0.0.1",
        "port": str(port),
        "quickPlayMultiplayer": f"127.0.0.1:{port}",
        "jvmArguments": ["-Xmx2G"],
    })
    client_command = launcher.command.get_minecraft_command("neoforge-21.1.252", MINECRAFT, options)
    server_stdout = server_dir / "probe-stdout.log"
    server_stderr = server_dir / "probe-stderr.log"
    client_stdout = game / "probe-stdout.log"
    client_stderr = game / "probe-stderr.log"
    print(f"NeoForge client probe on 127.0.0.1:{port}", flush=True)

    with server_stdout.open("wb") as server_out, server_stderr.open("wb") as server_err, \
            client_stdout.open("wb") as client_out, client_stderr.open("wb") as client_err:
        server_command = [str(java), "-Xms512m", "-Xmx2g"]
        if args.region_phase:
            phase_flags = ["wait_for_client", "with_create", "with_mekanism", "with_worldedit", "with_luckperms"]
            if args.optimization_mods:
                phase_flags.extend(("with_ferritecore", "with_modernfix"))
            server_command.extend(f"-Dyouer.phase.{flag}=true" for flag in phase_flags)
        server_command.extend(("-jar", "server.jar", "nogui"))
        server = subprocess.Popen(
            server_command,
            cwd=server_dir, stdin=subprocess.PIPE, stdout=server_out, stderr=server_err,
            creationflags=subprocess.CREATE_NO_WINDOW,
        )
        client = None
        try:
            wait_for(server, server_stdout, "Done (", 240)
            send_command(server, "difficulty peaceful")
            send_command(server, "time set day")
            send_command(server, "gamerule doDaylightCycle false")
            if not args.op_client:
                send_command(server, "deop TanukiProbe")
            client = subprocess.Popen(client_command, cwd=game, stdout=client_out, stderr=client_err)
            wait_for(client, server_dir / "logs" / "latest.log", "TanukiProbe joined the game", args.join_timeout)
            print("NEOFORGE_CLIENT_JOIN PASS", flush=True)
            if args.region_phase:
                wait_for(server, server_dir / "logs" / "latest.log", "PHASE_PROBE ", 240)
                phase_log = (server_dir / "logs" / "latest.log").read_text(encoding="utf-8", errors="replace")
                if "PHASE_PROBE PASS" not in phase_log:
                    raise RuntimeError(f"Region phase probe failed; inspect {server_dir / 'logs' / 'latest.log'}")
                print("NEOFORGE_REGION_PHASE PASS", flush=True)
            if args.op_client:
                send_command(server, "op TanukiProbe")
                wait_for(server, server_dir / "logs" / "latest.log", "Made TanukiProbe a server operator", 30)
            if args.exercise_mod_blocks:
                for x, block, marker in (
                        (2, "create:andesite_casing", "CREATE_BLOCK_OK"),
                        (3, "mekanism:steel_casing", "MEKANISM_BLOCK_OK")):
                    send_command(server, f"setblock {x} -60 4 minecraft:air")
                    send_command(server, f"setblock {x} -60 4 {block}")
                    send_command(server, f"execute if block {x} -60 4 {block} run say {marker}")
                    wait_for(server, server_dir / "logs" / "latest.log", marker, 30)
                print("NEOFORGE_MOD_BLOCKS PASS", flush=True)
            hold_deadline = time.monotonic() + args.hold_seconds
            while time.monotonic() < hold_deadline:
                if client.poll() is not None or server.poll() is not None:
                    raise RuntimeError("Client or server exited during the hold period")
                time.sleep(1)
            live_log = (server_dir / "logs" / "latest.log").read_text(encoding="utf-8", errors="replace")
            if "TanukiProbe was slain" in live_log or "TanukiProbe lost connection" in live_log:
                raise RuntimeError("The test player died or disconnected during the hold period")
        finally:
            if client is not None:
                stop(client)
            if args.op_client and server.poll() is None:
                send_command(server, "deop TanukiProbe")
            stop(server, server=True)

    log = (server_dir / "logs" / "latest.log").read_text(encoding="utf-8", errors="replace")
    if server.returncode != 0 or "Stopping server" not in log or "All dimensions are saved" not in log:
        raise RuntimeError(f"Server did not stop and save cleanly; inspect {server_stdout}")
    if "/ERROR]" in log:
        raise RuntimeError(f"Server logged an error; inspect {server_dir / 'logs' / 'latest.log'}")
    if args.hybrid_mods and not all(marker in log for marker in (
            "Create 6.0.9 initializing", "Mekanism", "Enabling WorldEdit v7.3.8",
            "[LuckPerms]: Successfully enabled")):
        raise RuntimeError(f"Hybrid MOD/plugin load was incomplete; inspect {server_dir / 'logs' / 'latest.log'}")
    if args.hybrid_mods:
        client_log = (game / "logs" / "latest.log").read_text(encoding="utf-8", errors="replace")
        if "Create 6.0.9 initializing" not in client_log or "Mekanism" not in client_log:
            raise RuntimeError(f"Client MOD load was incomplete; inspect {game / 'logs' / 'latest.log'}")
    print("NEOFORGE_CLIENT_PROBE PASS", flush=True)


if __name__ == "__main__":
    main()
