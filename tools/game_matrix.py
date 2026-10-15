#!/usr/bin/env python3
"""Run real, isolated Minecraft clients against a matching localhost Spigot server.

Requires a locally built official Spigot JAR, Java 25 and explicit EULA acceptance.
The test driver is packaged separately and never distributed with EnhancedTimer.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import signal
import socket
import struct
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
CACHE = Path(os.environ.get('ET_MATRIX_CACHE', ROOT / '.cache/matrix'))
MOD_VERSION = next(line.split('=', 1)[1].strip() for line in (ROOT / 'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
GRADLE = ROOT / '.cache/gradle-9.6.0/bin/gradle'
if not GRADLE.exists():
    GRADLE = ROOT / 'gradlew'
VERSIONS = {
    '26.1': {'fabric': '0.19.5', 'quilt': '0.30.1', 'forge': '26.1-62.0.9', 'neoforge': '26.1.0.19-beta'},
    '26.1.1': {'fabric': '0.19.5', 'quilt': '0.30.1', 'forge': '26.1.1-63.0.2', 'neoforge': '26.1.1.15-beta'},
    '26.1.2': {'fabric': '0.19.5', 'quilt': '0.30.1', 'forge': '26.1.2-64.1.5', 'neoforge': '26.1.2.121'},
    '26.2': {'fabric': '0.19.5', 'quilt': '0.30.1', 'forge': '26.2-65.1.5', 'neoforge': '26.2.0.93'},
    '26.3': {'fabric': '0.19.5', 'quilt': '0.30.1', 'forge': '26.3-66.0.12', 'neoforge': '26.3.0.83'},
}
ENV = dict(os.environ, GRADLE_USER_HOME=str(ROOT / '.cache/gradle-home'))


def gradle(*args):
    return ['bash', str(GRADLE), '--console=plain', '--no-daemon', '-Dorg.gradle.jvmargs=-Xmx2G', *args]


def run(command, log, timeout=1800):
    print('RUN', log, flush=True)
    with log.open('w') as output:
        process = subprocess.Popen(command, cwd=ROOT, env=ENV, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            code = process.wait(timeout=timeout)
        except (subprocess.TimeoutExpired, KeyboardInterrupt):
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=20)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
            raise
    if code:
        raise RuntimeError(f'exit {code}: {log}')


def rcon(port, command):
    with socket.create_connection(('127.0.0.1', port), timeout=10) as connection:
        def exact(size):
            data = b''
            while len(data) < size:
                piece = connection.recv(size - len(data))
                if not piece:
                    raise RuntimeError('RCON closed')
                data += piece
            return data
        def packet(kind, value):
            body = struct.pack('<ii', 1, kind) + value.encode() + b'\0\0'
            connection.sendall(struct.pack('<i', len(body)) + body)
            length = struct.unpack('<i', exact(4))[0]
            if not 10 <= length <= 1_000_000:
                raise RuntimeError('Invalid RCON packet size')
            result = exact(length)
            if struct.unpack('<i', result[:4])[0] != 1:
                raise RuntimeError('RCON authentication failed')
            return result[8:-2].decode()
        packet(3, 'EnhancedTimerLocalSmokeOnly')
        return packet(2, command)


def server_config(directory, game_port, rcon_port):
    directory.mkdir(parents=True, exist_ok=True)
    (directory / 'eula.txt').write_text('eula=true\n')
    (directory / 'server.properties').write_text(f'''server-ip=127.0.0.1
server-port={game_port}
online-mode=false
white-list=false
enable-rcon=true
rcon.port={rcon_port}
rcon.password=EnhancedTimerLocalSmokeOnly
view-distance=2
simulation-distance=2
spawn-protection=0
max-players=6
level-seed=1
level-type=minecraft\\:flat
generate-structures=false
sync-chunk-writes=false
''')


def await_server(process, port, timeout=600):
    until = time.monotonic() + timeout
    while time.monotonic() < until:
        if process.poll() is not None:
            raise RuntimeError(f'Server exited: {process.returncode}')
        try:
            if 'EnhancedTimer' in rcon(port, 'plugins'):
                return
        except (OSError, RuntimeError):
            pass
        time.sleep(2)
    raise RuntimeError('Server startup timeout')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--game', required=True, choices=VERSIONS)
    parser.add_argument('--server-jar', type=Path, required=True)
    parser.add_argument('--port', type=int, default=25600)
    parser.add_argument('--rcon-port', type=int, default=25601)
    parser.add_argument('--server-kind', choices=['spigot', 'paper', 'purpur', 'folia'], default='spigot')
    parser.add_argument('--loaders', nargs='+', choices=['fabric', 'quilt', 'forge', 'neoforge'], default=['fabric', 'quilt', 'forge', 'neoforge'])
    parser.add_argument('--accept-eula', action='store_true', required=True)
    parser.add_argument('--skip-passed', action='store_true')
    args = parser.parse_args()
    game = args.game
    address = f'127.0.0.1:{args.port}'
    run_id = f'matrix-{args.port}'
    module = '26.1' if game.startswith('26.1') else game
    folder = CACHE / (game if args.server_kind == 'spigot' else f'{game}-{args.server_kind}')
    folder.mkdir(parents=True, exist_ok=True)
    patch = [f'-Pminecraft261={game}'] if module == '26.1' else []
    run(gradle(f':client-{module}:jar', f':client-{module}:smokeDriverJar', ':paper:jar', *patch), folder / 'prepare.log')
    timer = folder / 'enhancedtimer.jar'
    shutil.copy2(CACHE / f'candidate/enhancedtimer-{module}.jar', timer)
    driver = ROOT / f'build/test-tools/enhancedtimer-smoke-driver-{game}.jar'
    server_dir = folder / args.server_kind
    server_config(server_dir, args.port, args.rcon_port)
    (server_dir / 'plugins').mkdir(exist_ok=True)
    shutil.copy2(CACHE / 'candidate/enhancedtimer-server.jar', server_dir / 'plugins/EnhancedTimer.jar')
    with (folder / f'{args.server_kind}-console.log').open('w') as output:
        server = subprocess.Popen(['java', '-Xmx1500M', '-Dterminal.jline=false', '-Dterminal.ansi=false', '-jar', str(args.server_jar.resolve()), '--nogui', '--noconsole'], cwd=server_dir, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
        try:
            await_server(server, args.rcon_port)
            run(['python3', str(ROOT / 'tools/server_command_smoke.py'), '--port', str(args.rcon_port)], folder / f'{args.server_kind}-commands.log', 90)
            results_path = folder / 'results.json'
            results = json.loads(results_path.read_text()) if results_path.exists() else {}
            results[args.server_kind] = {'status': 'PASS', 'plugin_sha256': hashlib.sha256((server_dir / 'plugins/EnhancedTimer.jar').read_bytes()).hexdigest(), 'server_sha256': hashlib.sha256(args.server_jar.read_bytes()).hexdigest(), 'version': rcon(args.rcon_port, 'version').strip(), 'commands_log': str(folder / f'{args.server_kind}-commands.log')}
            results_path.write_text(json.dumps(results, indent=2) + '\n')
            for loader in args.loaders:
                if args.skip_passed and results.get(loader, {}).get('status') == 'PASS' and results[loader].get('mod_sha256') == hashlib.sha256(timer.read_bytes()).hexdigest():
                    continue
                print(f'TEST {game} {loader} {VERSIONS[game][loader]}', flush=True)
                if loader in ('fabric', 'quilt'):
                    run_dir = ROOT / f'versions/{module}/build' / (f'run-smoke-fabric-{game}-{run_id}' if loader == 'fabric' else f'run-quilt-{game}-{run_id}')
                    task = 'runFabricSmoke' if loader == 'fabric' else 'runQuiltClient'
                    command = gradle(f':client-{module}:{task}', '-PquiltSmoke', '-PpluginSmoke', f'-PsmokeAddress={address}', f'-PsmokeRunId={run_id}', f'-PsmokeModJar={timer}', *patch)
                else:
                    run_dir = ROOT / f'tools/loader-smoke/build/run-{loader}-{game}-client'
                    run_dir.mkdir(parents=True, exist_ok=True)
                    # Avoid NeoForge's optional early GL window competing with SDL on macOS.
                    (run_dir / 'config').mkdir(exist_ok=True)
                    (run_dir / 'config/fml.toml').write_text('earlyWindowControl=false\n')
                    if loader == 'forge':
                        command = ['python3', str(ROOT / 'tools/forge_client_smoke.py'), '--game', game, '--version', VERSIONS[game][loader], '--run-dir', str(run_dir), '--mod', str(timer), '--driver', str(driver), '--address', address]
                    else:
                        command = gradle('-p', 'tools/loader-smoke', 'runClient', f'-Pgame={game}', f'-Ploader={loader}', f'-PloaderVersion={VERSIONS[game][loader]}', f'-PmodJar={timer}', f'-PdriverJar={driver}', f'-PjoinServer={address}', '-PpluginSmoke')
                result_file = run_dir / 'enhancedtimer-smoke-result.txt'
                result_file.unlink(missing_ok=True)
                started = time.monotonic()
                status, detail = 'FAIL', ''
                try:
                    run(command, folder / f'{loader}-client.log')
                    detail = result_file.read_text().strip() if result_file.exists() else 'Test driver did not produce a fresh result'
                    if not detail.startswith('PASS:'):
                        raise RuntimeError(detail)
                    status = 'PASS'
                except (RuntimeError, subprocess.TimeoutExpired) as error:
                    detail = str(error)
                results[loader] = {'status': status, 'loader_version': VERSIONS[game][loader], 'seconds': round(time.monotonic()-started), 'detail': detail, 'mod_sha256': hashlib.sha256(timer.read_bytes()).hexdigest(), 'log': str(folder / f'{loader}-client.log')}
                results_path.write_text(json.dumps(results, indent=2) + '\n')
                print(f'{status} {game} {loader}: {detail}', flush=True)
        finally:
            if server.poll() is None:
                try:
                    rcon(args.rcon_port, 'stop')
                    server.wait(timeout=40)
                except (OSError, RuntimeError, subprocess.TimeoutExpired):
                    os.killpg(server.pid, signal.SIGTERM)
                    server.wait(timeout=20)

    return int(any(results.get(loader, {}).get('status') != 'PASS' for loader in args.loaders))


if __name__ == '__main__':
    raise SystemExit(main())
