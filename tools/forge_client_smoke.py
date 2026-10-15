#!/usr/bin/env python3
"""Launch the official installed Forge production profile for an offline localhost test."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import uuid

ROOT = Path(__file__).resolve().parents[1]
HOME = ROOT / '.cache/matrix/production'


def digest(path):
    return hashlib.sha1(path.read_bytes()).hexdigest()


def obtain(artifact):
    path = HOME / 'libraries' / artifact['path']
    if path.exists() and digest(path) == artifact['sha1']:
        return path
    # Reuse official, checksum-matching artifacts already resolved by Gradle.
    filename = path.name
    for cache in (ROOT / '.cache/gradle-home/caches/modules-2/files-2.1', ROOT / '.cache/gradle-home/caches/minecraftforge'):
        candidate = next((p for p in cache.rglob(filename) if p.is_file() and digest(p) == artifact['sha1']), None)
        if candidate:
            path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(candidate, path)
            return path
    if not artifact.get('url'):
        raise RuntimeError(f'Missing installer-generated artifact: {path}')
    path.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(['curl', '-fLsS', '--retry', '2', artifact['url'], '-o', str(path)], check=True)
    if digest(path) != artifact['sha1']:
        path.unlink()
        raise RuntimeError(f'Checksum mismatch: {path}')
    return path


def allowed(rules):
    if not rules:
        return True
    enabled = False
    for rule in rules:
        os_rule = rule.get('os', {})
        current_os = {'Darwin': 'osx', 'Windows': 'windows', 'Linux': 'linux'}[platform.system()]
        if os_rule.get('name', current_os) != current_os or 'features' in rule:
            continue
        if os_rule.get('arch') and os_rule['arch'] != platform.machine():
            continue
        enabled = rule['action'] == 'allow'
    return enabled


def install(game, version):
    vanilla = HOME / 'versions' / game
    vanilla.mkdir(parents=True, exist_ok=True)
    (HOME / 'launcher_profiles.json').write_text('{"profiles":{}}')
    shutil.copy2(ROOT / f'.cache/gradle-home/caches/fabric-loom/{game}/mojang_minecraft_info.json', vanilla / f'{game}.json')
    shutil.copy2(ROOT / f'.cache/gradle-home/caches/fabric-loom/{game}/minecraft-client.jar', vanilla / f'{game}.jar')
    profile = HOME / f'versions/{game}-forge-{version.split("-", 1)[1]}/{game}-forge-{version.split("-", 1)[1]}.json'
    if not profile.exists():
        installer = ROOT / f'.cache/matrix/forge-installer-{game}.jar'
        if not installer.exists():
            subprocess.run(['curl', '-fLsS', '--retry', '2', f'https://maven.minecraftforge.net/net/minecraftforge/forge/{version}/forge-{version}-installer.jar', '-o', str(installer)], check=True)
        with (ROOT / f'.cache/matrix/forge-install-{game}.log').open('w') as log:
            subprocess.run(['java', '-Xmx2G', '-jar', str(installer), '--installClient', str(HOME)], stdout=log, stderr=subprocess.STDOUT, check=True)
    return json.loads((vanilla / f'{game}.json').read_text()), json.loads(profile.read_text())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--game', required=True)
    parser.add_argument('--version', required=True)
    parser.add_argument('--run-dir', type=Path, required=True)
    parser.add_argument('--mod', type=Path, required=True)
    parser.add_argument('--driver', type=Path, required=True)
    parser.add_argument('--address', default='127.0.0.1:25600')
    args = parser.parse_args()
    vanilla, forge = install(args.game, args.version)
    libraries = {}
    for library in [*vanilla['libraries'], *forge['libraries']]:
        if allowed(library.get('rules')):
            parts = library['name'].split(':')
            key = ':'.join(parts[:2] + parts[3:])
            libraries[key] = library
    classpath = [str(obtain(library['downloads']['artifact'])) for library in libraries.values()]
    classpath.append(str(HOME / f'versions/{args.game}/{args.game}.jar'))
    game_dir = args.run_dir.resolve()
    mods = game_dir / 'mods'
    mods.mkdir(parents=True, exist_ok=True)
    for old in mods.glob('*.jar'):
        old.unlink()
    shutil.copy2(args.mod, mods / 'enhancedtimer.jar')
    shutil.copy2(args.driver, mods / args.driver.name)
    values = {
        'auth_player_name': 'ETimerSmoke', 'version_name': forge['id'], 'game_directory': str(game_dir),
        'assets_root': str(ROOT / '.cache/gradle-home/caches/fabric-loom/assets'),
        'assets_index_name': args.game + '-' + vanilla['assetIndex']['id'],
        'auth_uuid': uuid.uuid3(uuid.NAMESPACE_DNS, 'ETimerSmoke').hex, 'auth_access_token': '0',
        'clientid': '', 'auth_xuid': '', 'version_type': 'release', 'natives_directory': str(game_dir / 'natives'),
        'launcher_name': 'EnhancedTimerLocalTest', 'launcher_version': '1', 'classpath': os.pathsep.join(classpath),
    }
    def expand(arguments):
        result = []
        for item in arguments:
            if isinstance(item, dict):
                if not allowed(item.get('rules')):
                    continue
                item = item['value']
            for value in item if isinstance(item, list) else [item]:
                for key, replacement in values.items():
                    value = value.replace('${' + key + '}', replacement)
                if '${' in value:
                    raise RuntimeError(f'Unresolved launch variable: {value}')
                result.append(value)
        return result
    command = ['java', '-Xmx2G', *expand(vanilla['arguments']['jvm']), *expand(forge['arguments'].get('jvm', [])),
               '-Denhancedtimer.smoke=true', '-Denhancedtimer.smokeMenus=true', '-Denhancedtimer.smokePluginMenus=true',
               f'-Denhancedtimer.smokeAddress={args.address}', forge['mainClass'],
               *expand(vanilla['arguments']['game']), *expand(forge['arguments'].get('game', [])),
               '--quickPlayMultiplayer', args.address]
    print('Launching installed production Forge', forge['id'], flush=True)
    os.chdir(game_dir)
    os.execvp(command[0], command)


if __name__ == '__main__':
    main()
