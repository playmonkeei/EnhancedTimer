#!/usr/bin/env python3
"""Validate installable archives, metadata, embedded libraries and translation coverage."""
import json
import re
import tomllib
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VERSION = re.search(r'^mod_version=(.+)$', (ROOT / 'gradle.properties').read_text(), re.M)[1]
expected = {f'enhancedtimer-{game}-{VERSION}.jar' for game in ('26.1', '26.2', '26.3', 'server')}
archives = list((ROOT / 'build/releases').glob('*.jar'))
assert {p.name for p in archives} == expected, 'Missing, stale or unexpected release JARs'
for path in sorted(archives):
    with zipfile.ZipFile(path) as jar:
        assert jar.testzip() is None, path
        names = set(jar.namelist())
        assert not any('bettertimer' in n.lower() for n in names), 'Old project namespace in release'
        assert len(names) == len(jar.namelist()), f'{path}: duplicate entries'
        assert 'dev/enhancedtimer/core/TimerHub.class' in names
        assert 'dev/enhancedtimer/text/RichText.class' in names
        assert 'dev/enhancedtimer/vendor/kyori/adventure/text/minimessage/MiniMessage.class' in names
        assert 'dev/enhancedtimer/vendor/gson/Gson.class' in names
        assert not any(n.startswith(('net/kyori/', 'com/google/gson/')) for n in names), 'Unrelocated libraries'
        assert not any(n.endswith('module-info.class') for n in names), 'Embedded module descriptor'
        for name in names:
            if name.startswith('META-INF/services/') and not name.endswith('/'):
                for provider in jar.read(name).decode().splitlines():
                    provider = provider.split('#')[0].strip()
                    if provider:
                        assert provider.replace('.', '/') + '.class' in names, f'Missing service provider: {provider}'
        if '-server-' in path.name:
            assert 'plugin.yml' in names and 'dev/enhancedtimer/paper/EnhancedTimerPlugin.class' in names
            assert 'name: EnhancedTimer' in jar.read('plugin.yml').decode()
            assert re.search(r'^version: [\x27\x22]?' + re.escape(VERSION) + r'[\x27\x22]?$', jar.read('plugin.yml').decode(), re.M), 'Wrong plugin version'
        else:
            assert 'pack.mcmeta' in names, 'Missing Minecraft resource pack metadata'
            assert json.loads(jar.read('pack.mcmeta'))['pack']['min_format'] == 84
            assert tomllib.loads(jar.read('META-INF/mods.toml').decode())['loaderVersion'] == '[62,)'
            assert not any(n.startswith('dev/enhancedtimer/smoke/') for n in names), 'Test driver in release'
            metadata = json.loads(jar.read('fabric.mod.json'))
            assert metadata['id'] == 'enhancedtimer' and metadata['name'] == 'Enhanced Timer'
            assert metadata['icon'] in names, 'Missing branded mod icon'
            assert metadata['version'] == VERSION and metadata['environment'] == '*'
            for descriptor in ('META-INF/mods.toml', 'META-INF/neoforge.mods.toml'):
                mod = tomllib.loads(jar.read(descriptor).decode())['mods'][0]
                assert mod['modId'] == 'enhancedtimer' and mod['version'] == VERSION, 'Wrong mod version'
            assert b'MixinConfigs: enhancedtimer.mixins.json' in jar.read('META-INF/MANIFEST.MF')
            mixins = json.loads(jar.read('enhancedtimer.mixins.json'))
            for mixin in mixins['client'] + mixins['mixins']:
                assert (mixins['package'] + '.' + mixin).replace('.', '/') + '.class' in names
        print(f'OK {path.name}')

langs = {p.stem: json.loads(p.read_text()) for p in (ROOT / 'client/src/main/resources/assets/enhancedtimer/lang').glob('*.json')}
for code, values in langs.items():
    assert values.keys() == langs['en_us'].keys(), f'Translation keys differ: {code}'
    assert all(isinstance(v, str) and v.strip() for v in values.values()), f'Empty translation: {code}'
for source in (ROOT / 'client/src/main/java').rglob('*.java'):
    for key in re.findall(r'(?:I18n\.(?:text|component)|row)\("([a-zA-Z0-9_.]+)"', source.read_text()):
        if not key.endswith('.'):
            assert key in langs['en_us'], f'Missing translation {key} in {source}'
print(f'OK {len(langs)} languages, {len(langs["en_us"])} keys each')
