#!/usr/bin/env python3
"""Check local README/documentation links and required branding assets without network access."""
from pathlib import Path
import re
from urllib.parse import unquote
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
documents = [ROOT / 'README.md', ROOT / 'README.de.md', ROOT / 'CHANGELOG.md',
             ROOT / 'docs/TECHNICAL.md', ROOT / 'docs/RELEASING.md', ROOT / 'docs/BRANDING.md']
for path in documents:
    text = path.read_text()
    links = re.findall(r'!?\[[^\]]*\]\(([^)]+)\)', text)
    links += re.findall(r'(?:src|href)="([^"]+)"', text)
    for link in links:
        if link.startswith(('https://', 'http://', 'mailto:')):
            continue
        filename, _, fragment = link.partition('#')
        target = (path.parent / unquote(filename)).resolve() if filename else path
        assert target.is_file(), f'{path.relative_to(ROOT)}: missing {link}'
        if fragment and not filename:
            assert f'id="{fragment}"' in text, f'{path.name}: missing anchor {fragment}'
    print(f'OK {path.relative_to(ROOT)} local links')
for path in (ROOT / 'docs/assets/brand').glob('*.svg'):
    ET.parse(path)
assert (ROOT / 'client/src/main/resources/assets/enhancedtimer/icon.png').read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
print('OK SVG branding, PNG icon and referenced screenshots')
