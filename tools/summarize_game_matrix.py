#!/usr/bin/env python3
"""Verify every requested runtime result against the frozen, distributable JARs."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path

from game_matrix import ROOT, CACHE, MOD_VERSION, VERSIONS

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--partial', action='store_true', help='Show progress without writing a release report')
args = parser.parse_args()
loaders = ('fabric', 'quilt', 'forge', 'neoforge')
checksums = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in (CACHE / 'candidate').glob('*.jar')}
report = {'date': datetime.now(timezone.utc).date().isoformat(), 'generated_at': datetime.now(timezone.utc).isoformat(), 'environment': 'macOS ARM64, OpenJDK 25; isolated localhost servers, offline test accounts', 'artifacts_sha256': {name.replace('.jar', f'-{MOD_VERSION}.jar'): sha for name, sha in checksums.items()}, 'client_to_spigot': {}, 'dedicated_mod_servers': {}, 'plugin_servers': {}}
issues = []


def read(path):
    return json.loads(path.read_text()) if path.exists() else {}


def verify(key, row, sha, log_key='log'):
    if row.get('status') != 'PASS':
        issues.append(f'{key}: {row.get("detail", "missing PASS")}')
        return False
    if row.get('mod_sha256') != sha:
        issues.append(f'{key}: previous artifact')
        return False
    log = Path(row[log_key])
    if not log.exists() or 'PASS: timer synchronization, 13 rendered' not in log.read_text(errors='replace'):
        issues.append(f'{key}: missing fresh driver evidence')
        return False
    row['finished_at'] = datetime.fromtimestamp(log.stat().st_mtime, timezone.utc).isoformat()
    return True


for game in VERSIONS:
    module = '26.1' if game.startswith('26.1') else game
    sha = checksums[f'enhancedtimer-{module}.jar']
    rows = read(CACHE / game / 'results.json')
    report['client_to_spigot'][game] = rows
    for loader in loaders:
        verify(f'client {game}/{loader}', rows.get(loader, {}), sha)

native = read(CACHE / 'native-results.json')
report['dedicated_mod_servers'] = native
for game in VERSIONS:
    module = '26.1' if game.startswith('26.1') else game
    for loader in loaders:
        key = f'{game}/{loader}'
        row = native.get(key, {})
        if verify(f'native {key}', row, checksums[f'enhancedtimer-{module}.jar'], 'client_log'):
            command_log = Path(row['commands_log'])
            if sum(line.startswith('PASS ') for line in command_log.read_text().splitlines()) != 12:
                issues.append(f'native {key}: incomplete RCON checks')

for kind, games in [('paper', ['26.1.1', '26.1.2', '26.2', '26.3']), ('purpur', ['26.1.2', '26.2', '26.3']), ('folia', ['26.1.2', '26.2', '26.3'])]:
    for game in games:
        module = '26.1' if game.startswith('26.1') else game
        key = f'{kind}/{game}'
        rows = read(CACHE / f'{game}-{kind}' / 'results.json')
        report['plugin_servers'][key] = rows
        verify(key, rows.get('fabric', {}), checksums[f'enhancedtimer-{module}.jar'])
        if rows.get(kind, {}).get('plugin_sha256') != checksums['enhancedtimer-server.jar']:
            issues.append(f'{key}: missing/current plugin artifact proof')
        command_path = rows.get(kind, {}).get('commands_log')
        if not command_path or sum(line.startswith('PASS ') for line in Path(command_path).read_text().splitlines()) != 12:
            issues.append(f'{key}: incomplete RCON checks')

for game, rows in report['client_to_spigot'].items():
    server = rows.get('spigot', {})
    if server.get('status') != 'PASS':
        issues.append(f'spigot/{game}: missing server PASS')
    command_path = server.get('commands_log')
    if not command_path or sum(line.startswith('PASS ') for line in Path(command_path).read_text().splitlines()) != 12:
        issues.append(f'spigot/{game}: incomplete RCON checks')
    if server.get('plugin_sha256') != checksums['enhancedtimer-server.jar']:
        issues.append(f'spigot/{game}: current plugin artifact proof missing')

# Keep source-controlled evidence portable; raw logs stay in the ignored test cache.
def portable(value):
    if isinstance(value, dict):
        return {k: portable(v) for k, v in value.items()}
    if isinstance(value, list):
        return [portable(v) for v in value]
    if isinstance(value, str) and value.startswith(str(ROOT) + '/'):
        return str(Path(value).relative_to(ROOT))
    return value

print(f'Outstanding checks: {len(issues)}')
for issue in issues:
    print('OPEN', issue)
if args.partial:
    raise SystemExit(0)
if issues:
    raise SystemExit(1)
report['successful_client_server_scenarios'] = 50
path = ROOT / 'docs/COMPATIBILITY_TESTS.json'
path.write_text(json.dumps(portable(report), indent=2, ensure_ascii=False) + '\n')
print('PASS: 20 client/loader combinations, 20 dedicated mod servers, 10 additional plugin-server scenarios')
print(path)
