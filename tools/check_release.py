#!/usr/bin/env python3
"""Validate release version/tag and require reviewable release notes before publishing."""
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
version = re.search(r'^mod_version=(.+)$', (ROOT / 'gradle.properties').read_text(), re.M)[1]
if not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+', version):
    raise SystemExit('Release version must use MAJOR.MINOR.PATCH')
tag = os.environ.get('RELEASE_TAG', '')
if tag and tag != f'v{version}':
    raise SystemExit(f'Tag {tag!r} does not match mod_version={version}')
notes = ROOT / f'release/{version}.md'
if not notes.is_file() or not notes.read_text().strip():
    raise SystemExit(f'Missing release notes: {notes.relative_to(ROOT)}')
print(f'OK EnhancedTimer {version}; tag and release notes checked')
