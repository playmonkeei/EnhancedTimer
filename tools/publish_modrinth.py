#!/usr/bin/env python3
"""Publish verified GitHub release JARs to Modrinth; dry-run performs no network writes."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import ssl
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid

API = 'https://api.modrinth.com/v2'
ROOT = Path(__file__).resolve().parents[1]
TARGETS = (
    ('26.1', ['26.1', '26.1.1', '26.1.2'], ['fabric', 'quilt', 'forge', 'neoforge']),
    ('26.2', ['26.2'], ['fabric', 'quilt', 'forge', 'neoforge']),
    ('26.3', ['26.3'], ['fabric', 'quilt', 'forge', 'neoforge']),
    ('server', ['26.1', '26.1.1', '26.1.2', '26.2', '26.3'], ['bukkit', 'spigot', 'paper', 'purpur', 'folia']),
)


def plan(version, artifact_dir, mod_project, plugin_project):
    if not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+', version):
        raise ValueError('Version must use MAJOR.MINOR.PATCH')
    checksums = {}
    for line in (artifact_dir / 'SHA256SUMS').read_text().splitlines():
        digest, filename = line.split()
        if filename in checksums or Path(filename).name != filename or not re.fullmatch(r'[0-9a-f]{64}', digest):
            raise ValueError('Invalid checksum manifest')
        checksums[filename] = digest
    expected = {f'enhancedtimer-{family}-{version}.jar' for family, _, _ in TARGETS}
    if set(checksums) != expected:
        raise ValueError('Checksum manifest must contain exactly the four release JARs')
    notes = (ROOT / f'release/{version}.md').read_text()
    result = []
    for family, games, loaders in TARGETS:
        filename = f'enhancedtimer-{family}-{version}.jar'
        path = artifact_dir / filename
        content = path.read_bytes()
        if hashlib.sha256(content).hexdigest() != checksums[filename]:
            raise ValueError(f'Checksum mismatch: {filename}')
        project = plugin_project if family == 'server' else mod_project
        if not re.fullmatch(r'[A-Za-z0-9_-]+', project):
            raise ValueError('A valid Modrinth project ID or slug is required')
        suffix = 'server' if family == 'server' else f'mc{family}'
        data = {
            'name': f'Enhanced Timer {version} · {suffix}',
            'version_number': f'{version}+{suffix}',
            'changelog': notes,
            'dependencies': [],
            'game_versions': games,
            'version_type': 'release',
            'loaders': loaders,
            'featured': True,
            'project_id': project,
            'file_parts': ['file'],
            'primary_file': 'file',
            'environment': 'dedicated_server_only' if family == 'server' else 'client_or_server',
        }
        result.append({'data': data, 'path': path, 'sha512': hashlib.sha512(content).hexdigest()})
    return result


class Modrinth:
    def __init__(self, token):
        self.token = token
        self.context = ssl.create_default_context()

    def request(self, path, body=None, content_type=None):
        headers = {'User-Agent': 'playmonkeei/EnhancedTimer (https://github.com/playmonkeei/EnhancedTimer)',
                   'Authorization': self.token, 'Accept': 'application/json'}
        if content_type:
            headers['Content-Type'] = content_type
        req = urllib.request.Request(API + path, data=body, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=90, context=self.context) as response:
                return json.load(response)
        except urllib.error.HTTPError as error:
            raise RuntimeError(f'Modrinth HTTP {error.code}: {path}; no automatic upload retry') from None

    def upload(self, item):
        boundary = 'EnhancedTimer-' + uuid.uuid4().hex
        data = json.dumps(item['data'], ensure_ascii=False).encode()
        path = item['path']
        body = (f'--{boundary}\r\nContent-Disposition: form-data; name="data"\r\n'
                'Content-Type: application/json\r\n\r\n').encode() + data
        body += (f'\r\n--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{path.name}"\r\n'
                 'Content-Type: application/java-archive\r\n\r\n').encode() + path.read_bytes()
        body += f'\r\n--{boundary}--\r\n'.encode()
        return self.request('/version', body, f'multipart/form-data; boundary={boundary}')


def preflight(api, items):
    """Check identity, all targets and immutable retries before any upload."""
    user = api.request('/user')
    if user['username'].lower() != 'playmonkeei':
        raise ValueError('Refusing to publish: Modrinth token must belong to playmonkeei')
    loaders = {item['name'] for item in api.request('/tag/loader')}
    games = {item['version'] for item in api.request('/tag/game_version')}
    projects = {}
    for item in items:
        data = item['data']
        if not set(data['loaders']) <= loaders or not set(data['game_versions']) <= games:
            raise ValueError('Target platform/version not recognized by Modrinth')
        slug = data['project_id']
        if slug not in projects:
            project = api.request('/project/' + urllib.parse.quote(slug, safe=''))
            members = api.request('/project/' + project['id'] + '/members')
            # Modrinth v2 permissions are a bitfield; UPLOAD_VERSION is bit 0.
            if not any(member['user']['id'] == user['id'] and member.get('accepted')
                       and (member.get('permissions') or 0) & 1 for member in members):
                raise ValueError(f'playmonkeei lacks upload_version permission for project {slug}')
            projects[slug] = (project['id'], api.request('/project/' + project['id'] + '/version'))
        data['project_id'], versions = projects[slug]
        found = next((v for v in versions if v['version_number'] == data['version_number']), None)
        if found:
            hashes = {f['hashes']['sha512'] for f in found['files']}
            if (hashes != {item['sha512']} or set(found['loaders']) != set(data['loaders'])
                    or set(found['game_versions']) != set(data['game_versions'])
                    or found['version_type'] != 'release' or found['status'] != 'listed'):
                raise ValueError(f'Existing version differs; refusing to replace {data["version_number"]}')
            item['existing'] = found['id']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--version', required=True)
    parser.add_argument('--artifact-dir', type=Path, required=True)
    parser.add_argument('--mod-project', required=True)
    parser.add_argument('--plugin-project', required=True)
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    items = plan(args.version, args.artifact_dir, args.mod_project, args.plugin_project)
    if args.dry_run:
        for item in items:
            print(json.dumps({'file': item['path'].name, 'metadata': item['data']}, ensure_ascii=False))
        print('Dry-run complete: four verified JARs; no network requests or uploads')
        return
    token = os.environ.get('MODRINTH_TOKEN', '')
    if not token:
        raise ValueError('Set the MODRINTH_TOKEN repository secret before publishing')
    api = Modrinth(token)
    preflight(api, items)
    for item in items:
        if item.get('existing'):
            print(f'Already published with identical hash: {item["data"]["version_number"]}')
            continue
        published = api.upload(item)
        if item['sha512'] not in {f['hashes']['sha512'] for f in published['files']}:
            raise RuntimeError('Uploaded version returned an unexpected file hash')
        print(f'Published {item["data"]["version_number"]}: https://modrinth.com/version/{published["id"]}')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, RuntimeError, OSError, KeyError) as error:
        print(f'Error: {error}', file=sys.stderr)
        raise SystemExit(1)
