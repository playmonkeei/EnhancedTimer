#!/usr/bin/env python3
"""Freeze the four distributable JARs before testing Minecraft patch versions."""
import hashlib
import json
import shutil
from game_matrix import ROOT, CACHE, MOD_VERSION, gradle, run

candidate = CACHE / 'candidate'
candidate.mkdir(parents=True, exist_ok=True)
run(gradle(':client-26.1:jar', ':client-26.2:jar', ':client-26.3:jar', ':paper:jar'), candidate / 'build.log')
for game in ('26.1', '26.2', '26.3'):
    shutil.copy2(ROOT / f'versions/{game}/build/libs/enhancedtimer-{game}-{MOD_VERSION}.jar', candidate / f'enhancedtimer-{game}.jar')
shutil.copy2(ROOT / f'paper/build/libs/enhancedtimer-server-{MOD_VERSION}.jar', candidate / 'enhancedtimer-server.jar')
(candidate / 'checksums.json').write_text(json.dumps({p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(candidate.glob('*.jar'))}, indent=2) + '\n')
print('Frozen release candidates:', candidate)
