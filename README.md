# ZUNO

**ZUNO** is an independently maintained Android music player focused on a personal, customizable listening experience.

The app combines online music discovery with local audio playback, lyrics, player customization, and optional third-party integrations.

<p>
  <a href="https://github.com/mm2ultimatehub-rgb/ZUNO/releases/latest"><strong>Download latest release</strong></a>
  ·
  <a href="https://mm2ultimatehub-rgb.github.io/ZUNO-Icon-Store/"><strong>ZUNO Icon Store</strong></a>
  ·
  <a href="https://github.com/mm2ultimatehub-rgb/ZUNO/issues"><strong>Report an issue</strong></a>
</p>

[![Latest release](https://img.shields.io/github/v/release/mm2ultimatehub-rgb/ZUNO?style=flat-square&label=release)](https://github.com/mm2ultimatehub-rgb/ZUNO/releases/latest)
[![Build](https://img.shields.io/github/actions/workflow/status/mm2ultimatehub-rgb/ZUNO/zuno-preview.yml?branch=main&style=flat-square&label=signed%20APK)](https://github.com/mm2ultimatehub-rgb/ZUNO/actions/workflows/zuno-preview.yml)

---

## What ZUNO offers

- Online music discovery and playback.
- Local music library and offline audio playback.
- Background playback and Android media controls.
- Synchronized lyrics, with translation and romanization options where supported.
- Player appearance, themes, gestures, and interface customization.
- Audio controls such as equalizer, playback speed, pitch, and crossfade where supported by the device and audio pipeline.
- Optional integrations such as Last.fm, ListenBrainz, Discord Rich Presence, and playlist import.
- Backup and restore tools.
- Android Auto support.
- A community icon catalog connected to the [ZUNO Icon Store](https://mm2ultimatehub-rgb.github.io/ZUNO-Icon-Store/).

Availability of online services and provider-based features may vary by region and by the external service.

## App information

| Item | Value |
| --- | --- |
| App name | ZUNO |
| Android application ID | `com.zuno.music` |
| Minimum Android version | Android 8.0 (API 26) |
| Main technologies | Kotlin, Jetpack Compose, Gradle |
| Downloads | [GitHub Releases](https://github.com/mm2ultimatehub-rgb/ZUNO/releases) |

Install updates from the official ZUNO GitHub Releases page. To update an existing installation, the new APK must be signed with the same release signing key as the installed app.

## ZUNO Icon Store sync

ZUNO can synchronize public icons from the [ZUNO Icon Store](https://mm2ultimatehub-rgb.github.io/ZUNO-Icon-Store/).

The repository includes an automated workflow that checks the public icon catalog. When at least **20 new public icons** are available during the Azerbaijan calendar day, the workflow can synchronize the catalog, increment the app version, and start a signed APK build. The daily threshold and build status are managed by GitHub Actions.

## Build from source

1. Clone the repository with its submodules:

   ```bash
   git clone --recurse-submodules https://github.com/mm2ultimatehub-rgb/ZUNO.git
   cd ZUNO
   ```

2. Build with the Gradle wrapper:

   ```bash
   ./gradlew assembleGmsMobileArm64Release
   ```

A locally generated release may require signing configuration. The signed distribution workflow is maintained in [GitHub Actions](https://github.com/mm2ultimatehub-rgb/ZUNO/actions/workflows/zuno-preview.yml). Signing keys and API credentials must be stored as private repository secrets or local, untracked configuration; never commit them to the repository.

## ZUNO-maintained modules

- [ZUNO Core](https://github.com/mm2ultimatehub-rgb/ZUNO-Core)
- [ZUNO Lyrics](https://github.com/mm2ultimatehub-rgb/ZUNO-Lyrics)
- [ZUNO IconPack](https://github.com/mm2ultimatehub-rgb/ZUNO-IconPack)
- [ZUNO Obfuscator](https://github.com/mm2ultimatehub-rgb/ZUNO-Obfuscator)

## Upstream and third-party credits

ZUNO is an independent fork derived from [ArchiveTune](https://github.com/rukamori/ArchiveTune). The upstream project and its contributors retain credit and copyright for their original work. ZUNO is maintained separately and is not affiliated with or endorsed by the ArchiveTune maintainers.

This repository also uses third-party open-source libraries and components. Their respective authors, notices, and license terms remain applicable. Please consult the dependency and license information included with the source before redistributing a build.

## Support

- **Bug reports:** [GitHub Issues](https://github.com/mm2ultimatehub-rgb/ZUNO/issues)
- **Source code:** [mm2ultimatehub-rgb/ZUNO](https://github.com/mm2ultimatehub-rgb/ZUNO)
- **Icon community:** [ZUNO Icon Store](https://mm2ultimatehub-rgb.github.io/ZUNO-Icon-Store/)

When reporting a problem, include your ZUNO version, Android version, device model, and steps to reproduce it. Do not post passwords, signing keys, API tokens, or private account data.
