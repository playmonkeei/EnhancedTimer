import copy
import hashlib
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from publish_modrinth import TARGETS, plan, preflight


class FakeAPI:
    def __init__(self, items, username='playmonkeei', permissions=1, existing=None):
        self.responses = {
            '/user': {'username': username, 'id': 'owner'},
            '/tag/loader': [{'name': name} for name in {n for _, _, ns in TARGETS for n in ns}],
            '/tag/game_version': [{'version': name} for name in {n for _, ns, _ in TARGETS for n in ns}],
        }
        for slug in ('mod', 'plugin'):
            project_id = slug + '-id'
            self.responses['/project/' + slug] = {'id': project_id}
            self.responses[f'/project/{project_id}/members'] = [
                {'user': {'id': 'owner'}, 'accepted': True, 'permissions': permissions}]
            self.responses[f'/project/{project_id}/version'] = existing or []

    def request(self, path):
        return copy.deepcopy(self.responses[path])


class PublishingTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.path = Path(self.directory.name)
        lines = []
        for family, _, _ in TARGETS:
            filename = f'enhancedtimer-{family}-1.0.0.jar'
            content = family.encode()
            (self.path / filename).write_bytes(content)
            lines.append(f'{hashlib.sha256(content).hexdigest()}  {filename}')
        (self.path / 'SHA256SUMS').write_text('\n'.join(lines) + '\n')
        self.items = plan('1.0.0', self.path, 'mod', 'plugin')

    def test_one_mod_project_is_resolved_for_all_three_families(self):
        preflight(FakeAPI(self.items), self.items)
        self.assertEqual(['mod-id'] * 3 + ['plugin-id'], [i['data']['project_id'] for i in self.items])

    def test_wrong_account_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'token must belong'):
            preflight(FakeAPI(self.items, username='another-account'), self.items)

    def test_mod_and_plugin_share_one_project(self):
        items = plan('1.0.0', self.path, 'mod', 'mod')
        preflight(FakeAPI(items), items)
        self.assertEqual(['mod-id'] * 4, [i['data']['project_id'] for i in items])

    def test_edit_permission_does_not_grant_upload_permission(self):
        with self.assertRaisesRegex(ValueError, 'lacks upload_version'):
            preflight(FakeAPI(self.items, permissions=4), self.items)

    def test_changed_release_bytes_are_rejected(self):
        self.items[0]['path'].write_bytes(b'changed')
        with self.assertRaisesRegex(ValueError, 'Checksum mismatch'):
            plan('1.0.0', self.path, 'mod', 'plugin')

    def existing(self):
        item = self.items[0]
        return {**item['data'], 'id': 'version-id', 'status': 'listed',
                'files': [{'hashes': {'sha512': item['sha512']}}]}

    def test_identical_published_version_is_skipped(self):
        preflight(FakeAPI(self.items, existing=[self.existing()]), self.items)
        self.assertEqual('version-id', self.items[0]['existing'])
        self.assertNotIn('existing', self.items[1])

    def test_different_existing_release_is_rejected(self):
        existing = self.existing()
        existing['files'][0]['hashes']['sha512'] = 'different'
        with self.assertRaisesRegex(ValueError, 'Existing version differs'):
            preflight(FakeAPI(self.items, existing=[existing]), self.items)

    def test_duplicate_manifest_entry_is_rejected(self):
        manifest = self.path / 'SHA256SUMS'
        manifest.write_text(manifest.read_text() + manifest.read_text().splitlines()[0] + '\n')
        with self.assertRaisesRegex(ValueError, 'Invalid checksum manifest'):
            plan('1.0.0', self.path, 'mod', 'plugin')


if __name__ == '__main__':
    unittest.main()
