# Enhanced Timer

Display name: **Enhanced Timer**. Code identifier, mod ID, resource namespace and data directory: `enhancedtimer`. Planned repository: `playmonkeei/EnhancedTimer`. This preparation does not create or publish a remote repository or marketplace project.

The previous working name was BetterTimer. Production code, packages, artifact names, commands and release metadata now use the new identity. Historical test reports keep their original names, version numbers and hashes. The working directory may still be named `mc-better-timer`; it does not affect the build or product identity.

| Before | Now |
| --- | --- |
| `dev.bettertimer` | `dev.enhancedtimer` |
| `bettertimer:sync` | `enhancedtimer:sync` |
| `/bt`, `/bettertimer` | `/et`, `/enhancedtimer` |
| `/bts`, `/bettertimer-server` | `/ets`, `/enhancedtimer-server` |
| `bettertimer.view`, `bettertimer.control` | `enhancedtimer.view`, `enhancedtimer.control` |
| `config/bettertimer/` | `config/enhancedtimer/` |
| `plugins/BetterTimer/` | `plugins/EnhancedTimer/` |
| `<world>/bettertimer/` | `<world>/enhancedtimer/` |
| `bettertimer-<family>-1.0.0.jar` | `enhancedtimer-<family>-1.0.0.jar` |

The default menu key remains **B**, avoiding Minecraft's inventory binding. The generic `/timer` command is left available for other mods. Client and server must both use the renamed build to share timers. The wire channel and magic value changed together. If moving a local development installation, back up the old data first and copy it into the new directories while Minecraft/server processes are stopped. Automatic migration from the unpublished working-name installation is not implemented.

## Artwork and screenshots

The original stopwatch icon, README banner and status badges are authored SVGs under `assets/brand/`. The icon's PNG export is bundled in the mod JAR. Branding uses mint (`#8FE9C9`) on dark navy (`#101E2B`); the in-game HUD keeps its simple gold default.

The five PNGs under `assets/screenshots/` are unchanged Minecraft F2 captures from a real isolated Minecraft Java 26.3 Fabric client running Enhanced Timer. They show the gold default, main menu, color palette/rainbow, freely positioned editor and actual countdown expiry. The editor example uses 2× text size and keyboard positioning; mouse drag is covered by the historical manual baseline, not newly claimed from this capture session. The scene uses vanilla graphics, a local demo world, daytime and staged Survival inventory. No rendered or generated mock HUD is composited into the screenshots.

English screenshots are shared between the English and German player READMEs. Source filenames and capture hashes are in the local `.cache/enhanced-readme/shots.json`; raw captures and observational probe traces are kept in the ignored isolated client directory.

The README uses GitHub-compatible Markdown/HTML, inline SVG artwork and local image paths. It has no external image service dependency or fake CI/download counters. The version badge identifies the 1.0.0 release.
