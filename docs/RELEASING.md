# Releases

Repository: https://github.com/playmonkeei/EnhancedTimer. Releases are published only under **playmonkeei**.

## GitHub

Java 25 is required. Before a release, update `mod_version` in `gradle.properties`, add `release/<version>.md`, update the changelog and document validation against the actual JAR hashes. Keep historical test reports unchanged.

```sh
./gradlew build releaseBundle
python3 tools/check_release.py
python3 tools/verify_artifacts.py
python3 tools/verify_docs.py
python3 -m unittest discover -s tools/tests
actionlint
```

Commit and push to `main`. The GitHub workflow builds all three Minecraft families, runs the unit tests, validates the packaged archives, compiles against Minecraft 26.1.1/26.1.2, and checks the separate Spigot API. After these checks pass, push a matching tag, for example `v1.0.0`. The tag workflow repeats the checks and publishes four JARs, the documentation ZIP and `SHA256SUMS`. An existing release is never overwritten. Local builds remain useful for gameplay tests; GitHub Actions produces the published files.

The game-test fixtures require a graphical Minecraft client, official server downloads and explicit EULA acceptance. They are separate from CI. See [TESTING.md](TESTING.md), [MANUAL_TESTS.md](MANUAL_TESTS.md) and the release-specific validation report.

## Modrinth

Use one Enhanced Timer project for all loaders, with separate versions for the mod families and Bukkit/Spigot/Paper/Purpur/Folia plugin. This follows [Modrinth's recommendation](https://support.modrinth.com/en/articles/8824810-multiple-loaders-on-one-project) to keep loaders together. The project should be owned by **playmonkeei**, use the MIT license, link this repository and issue tracker, and explain which file belongs in `mods/` or `plugins/`. A project must complete Modrinth's normal submission/review process before it is publicly discoverable.

In GitHub repository Settings → Secrets and variables → Actions, configure:

| Setting | Type | Value |
| --- | --- | --- |
| `MODRINTH_PROJECT_ID` | Repository variable | Enhanced Timer project ID or slug |
| `MODRINTH_TOKEN` | Repository secret | A playmonkeei Modrinth personal access token with user/project read and version-create access |

Never put the token in source files, logs, issues or chat. The publishing script checks the token's account and the project's upload permission before uploading anything.

Run **Publish GitHub release to Modrinth** in the Actions tab, select the existing GitHub tag and leave `dry_run` enabled first. This downloads the already published GitHub JARs, verifies their SHA-256 hashes and previews all four versions. Then run the workflow again with `dry_run` disabled to upload those exact files. A token is only needed for the upload. Identical already-published versions are skipped; different existing files or metadata abort the run.

For a local preview without credentials:

```sh
python3 tools/publish_modrinth.py --version 1.0.0 --artifact-dir build/releases \
  --mod-project YOUR_PROJECT --plugin-project YOUR_PROJECT --dry-run
```

The mod versions use `1.0.0+mc26.1`, `1.0.0+mc26.2`, and `1.0.0+mc26.3`; the plugin uses `1.0.0+server`. No duplicate build is performed for Modrinth. A failed upload is not retried automatically; rerunning verifies existing versions first.

## Other distribution platforms

Modrinth and GitHub are the initial distribution targets. CurseForge can be added for the client/server mod, and Hangar for the Paper-compatible plugin. Additional marketplaces should receive the same verified release JARs. They are not configured by the initial workflows.
