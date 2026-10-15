#!/usr/bin/env python3
"""Test dedicated mod servers, then join each with a real Fabric client."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import signal
import subprocess
import time

from game_matrix import ROOT, CACHE, VERSIONS, ENV, gradle, run, rcon, server_config


def install(loader, game, version, directory):
    marker = directory / 'run.sh'
    if not marker.exists():
        directory.mkdir(parents=True, exist_ok=True)
        libraries = directory / 'libraries'
        if not libraries.exists():
            libraries.symlink_to(CACHE / 'production/libraries', target_is_directory=True)
        installer = CACHE / f'{loader}-installer-{game}.jar'
        if not installer.exists():
            base = 'https://maven.minecraftforge.net/net/minecraftforge/forge' if loader == 'forge' else 'https://maven.neoforged.net/releases/net/neoforged/neoforge'
            artifact = 'forge' if loader == 'forge' else 'neoforge'
            subprocess.run(['curl', '-fLsS', '--retry', '2', f'{base}/{version}/{artifact}-{version}-installer.jar', '-o', str(installer)], check=True)
        with (directory.parent / 'install.log').open('w') as output:
            subprocess.run(['java', '-Xmx2G', '-jar', str(installer), '--installServer', str(directory)], cwd=ROOT, stdout=output, stderr=subprocess.STDOUT, check=True)
    (directory / 'user_jvm_args.txt').write_text('-Xmx1500M\n-Dterminal.jline=false\n-Dterminal.ansi=false\n')
    return ['bash', str(marker), '--nogui']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--accept-eula', action='store_true', required=True)
    parser.add_argument('--games', nargs='+', choices=VERSIONS, default=list(VERSIONS))
    parser.add_argument('--loaders', nargs='+', choices=['fabric', 'quilt', 'forge', 'neoforge'], default=['fabric', 'quilt', 'forge', 'neoforge'])
    parser.add_argument('--prepare-only', action='store_true')
    args = parser.parse_args()
    results_file = CACHE / 'native-results.json'
    results = json.loads(results_file.read_text()) if results_file.exists() else {}
    for game in args.games:
        module = '26.1' if game.startswith('26.1') else game
        patch = [f'-Pminecraft261={game}'] if module == '26.1' else []
        for loader in args.loaders:
            key = f'{game}/{loader}'
            folder = CACHE / f'native-{game}-{loader}'
            folder.mkdir(parents=True, exist_ok=True)
            directory = folder / 'server'
            print('NATIVE', key, flush=True)
            try:
                if loader in ('forge', 'neoforge'):
                    command = install(loader, game, VERSIONS[game][loader], directory)
                    if args.prepare_only:
                        print('PREPARED', key, flush=True)
                        continue
                    run(gradle(f':client-{module}:jar', f':client-{module}:smokeDriverJar', *patch), folder / 'prepare.log')
                    (directory / 'mods').mkdir(exist_ok=True)
                    for jar in (directory / 'mods').glob('*.jar'):
                        jar.unlink()
                    shutil.copy2(CACHE / f'candidate/enhancedtimer-{module}.jar', directory / 'mods/EnhancedTimer.jar')
                else:
                    launch = folder / 'launch.json'
                    task = 'runFabricServerSmoke' if loader == 'fabric' else 'runQuiltServerSmoke'
                    run(gradle(f':client-{module}:{task}', f':client-{module}:smokeDriverJar', '-PquiltSmoke', f'-PexportServerSmoke={launch}', f'-PsmokeModJar={CACHE}/candidate/enhancedtimer-{module}.jar', *patch), folder / 'prepare.log')
                    data = json.loads(launch.read_text())
                    command, directory = data['command'], Path(data['workingDir'])
                server_config(directory, 25610, 25611)
                with (folder / 'server-console.log').open('w') as output:
                    server = subprocess.Popen(command, cwd=directory, env=ENV, stdin=subprocess.PIPE, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
                    try:
                        until = time.monotonic() + 240
                        while True:
                            if server.poll() is not None:
                                raise RuntimeError(f'Server exited {server.returncode}')
                            try:
                                rcon(25611, 'list')
                                break
                            except (OSError, RuntimeError):
                                if time.monotonic() >= until:
                                    raise RuntimeError('Dedicated server startup timeout')
                                time.sleep(2)
                        run(['python3', str(ROOT / 'tools/server_command_smoke.py'), '--port', '25611'], folder / 'commands.log', 90)
                        result_file = ROOT / f'versions/{module}/build/run-smoke-fabric-{game}-native/enhancedtimer-smoke-result.txt'
                        result_file.unlink(missing_ok=True)
                        run(gradle(f':client-{module}:runFabricSmoke', '-PsmokeAddress=127.0.0.1:25610', '-PsmokeRunId=native', f'-PsmokeModJar={CACHE}/candidate/enhancedtimer-{module}.jar', *patch), folder / 'client.log', 300)
                        detail = result_file.read_text().strip() if result_file.exists() else 'Missing fresh result'
                        if not detail.startswith('PASS:'):
                            raise RuntimeError(detail)
                        results[key] = {'status': 'PASS', 'mod_sha256': hashlib.sha256(next((directory / 'mods').glob('*.jar')).read_bytes()).hexdigest(), 'loader_version': VERSIONS[game][loader], 'detail': detail, 'commands_log': str(folder / 'commands.log'), 'client_log': str(folder / 'client.log')}
                    finally:
                        if server.poll() is None:
                            try:
                                rcon(25611, 'stop')
                                server.wait(timeout=40)
                            except (OSError, RuntimeError, subprocess.TimeoutExpired):
                                os.killpg(server.pid, signal.SIGTERM)
                                server.wait(timeout=20)
                print('PASS', key, flush=True)
            except (RuntimeError, OSError, subprocess.SubprocessError) as error:
                results[key] = {'status': 'FAIL', 'detail': str(error), 'logs': str(folder)}
                print('FAIL', key, error, flush=True)
            if not args.prepare_only:
                results_file.write_text(json.dumps(results, indent=2) + '\n')

    return 0 if args.prepare_only else int(any(results.get(f'{game}/{loader}', {}).get('status') != 'PASS' for game in args.games for loader in args.loaders))


if __name__ == '__main__':
    raise SystemExit(main())
