<div align="center">

<img src="assets/translator-icon.png" alt="Myths & Legends Translator icon" width="192" height="192">

# Myths & Legends Translator

**An unofficial API compatibility bridge for Myths and Legends and Cobblemon.**

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)
![Loader](https://img.shields.io/badge/Loader-Fabric-DDBB7B)
![Java](https://img.shields.io/badge/Java-21-5372A1)
![Status](https://img.shields.io/badge/Status-Beta-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

[Français](README.fr.md) · [Technical compatibility notes](docs/COMPATIBILITY.md)

</div>

## Overview

**Myths & Legends Translator** is an independent Minecraft Fabric mod that restores selected legendary and mythical Pokémon encounters when using **Myths and Legends 1.9.0** with **Cobblemon 1.8.1** on **Minecraft 1.21.1**.

Myths and Legends 1.9.0 was built for an older Cobblemon spawning API. Some methods changed or disappeared in Cobblemon 1.8.1, causing a `NoSuchMethodError` when a key item attempts a forced spawn. The translator adapts those incompatible Java calls at runtime.

**The purpose is compatibility, not replacement.** Myths and Legends remains responsible for its key items and encounter mechanics; Cobblemon remains responsible for Pokémon spawning. The translator changes only the relevant API interactions and provides optional in-game diagnostics.

> [!IMPORTANT]
> This is an **unofficial community project**. It does **not** include or replace Cobblemon, Myths and Legends, or their spawn datapacks. Download those projects separately. This release is an early beta, not a guarantee that every encounter works.

## Features

- **Legacy API translation:** adapts the spawning calls that changed between the supported versions of Cobblemon.
- **Original encounter behavior:** avoids hardcoded key-to-Pokémon mappings, custom spawn rates, or a replacement spawning engine.
- **Datapack-aware diagnostics:** reads active `data/cobblemon/spawn_pool_world/*.json` resources to find rules referencing the key item in use.
- **Creative Mode report:** shows the Pokémon and spawn-entry IDs, biome checks, basic time/weather checks, and any extra conditions that are not evaluated.
- **Clickable test tools:** chat actions for biome search/teleportation and for changing the time or weather, with access restrictions.
- **Survival Mode hint:** provides a brief, spoiler-light warning when the basic conditions checked by the diagnostic do not appear to match.

## Requirements and tested versions

| Component | Version / notes |
| --- | --- |
| Minecraft: Java Edition | **1.21.1** |
| Mod loader | **Fabric** |
| Cobblemon | **1.8.1** (the only Cobblemon version verified for this release) |
| Myths and Legends | **1.9.0 Fabric** |
| Fabric API | Required by the diagnostic/event integration |
| Java | **21** recommended for the game and required for compilation |
| Myths and Legends spawn datapack | Required for its key-item encounter rules, or use your own compatible datapack |

Other Minecraft versions, mod loaders, and combinations of the two mods are **not tested**. Even if the Fabric metadata permits a newer Cobblemon version, that does not establish compatibility.

### Original project downloads

- [Cobblemon on Modrinth](https://modrinth.com/mod/cobblemon)
- [Myths and Legends on Modrinth](https://modrinth.com/mod/cobblemon-myths-and-legends-sidemod)
- [Official Myths and Legends datapack on GitHub](https://github.com/D0ctorLeon/mythsandlegends-datapack)
- [Official datapack downloads on CurseForge](https://www.curseforge.com/minecraft/data-packs/mythsandlegends-datapack/files)

The original datapack has its **own license and redistribution restrictions**. This repository does not bundle it.

## Installation (players)

1. Install Minecraft **1.21.1** with Fabric Loader.
2. Install Fabric API, **Cobblemon 1.8.1**, and **Myths and Legends 1.9.0 (Fabric)**.
3. Download the translator's **regular release JAR** and place it in the same instance's `mods/` directory. **Do not use** a `-dev.jar` or `-sources.jar` file.
4. Install the Myths and Legends spawn datapack in your world's `datapacks/` directory (or an equivalent compatible custom datapack). Keep the datapack as a separate download.
5. Start the game, enter your world, and check `/datapack list enabled` if your key items have no matching spawn entries.

Back up an existing world before installing or updating any Minecraft mod. For multiplayer, keep the relevant mod and datapack versions consistent between the server and clients.

## How it works

```text
Myths and Legends 1.9.0
  └─ Uses a key item and runs its original force-spawn routine
       └─ Calls a Cobblemon 1.7-era spawning API
            └─ Myths & Legends Translator
                 ├─ Patches specific bytecode call sites with a Mixin plugin
                 ├─ Converts legacy SpawnBucket usage to String bucket IDs
                 ├─ Calls the corresponding Cobblemon 1.8.1 API
                 └─ Returns the result to the original routine
                      └─ Cobblemon 1.8.1 handles the encounter
```

The bridge works through **Mixin + ASM transformations** and small **reflection-based Java adapters**. It targets `ForceSpawningUtils` rather than rewriting Myths and Legends or shipping modified third-party JARs. Calls that are still compatible are left in place.

Examples of API differences handled in this release include:

| Legacy API usage | Translation for Cobblemon 1.8.1 |
| --- | --- |
| `BestSpawnerConfig.getBuckets()` | Obtain configured world-bucket IDs from `getWorldBuckets()` |
| `PlayerSpawner.chooseBucket(...) → SpawnBucket` | Interpret the new `String` return type |
| `PlayerSpawner.getMatchingSpawns(SpawnBucket, ...)` | Supply a `String` bucket ID |
| `SpawnDetail.getBucket() → SpawnBucket` | Interpret the new `String` return type |
| `SpawnDetail.choose(..., SpawnBucket, ...)` | Supply a `String` bucket ID |
| `SpawnBucket.getName()` | Use the `String` bucket ID directly |

**This is not a generic Cobblemon compatibility framework:** only the call sites and signatures needed by the supported Myths and Legends version are adapted. Implementation details are in [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Creative Mode diagnostics

When a player uses a supported Myths and Legends key item in **Creative Mode**, the mod displays a chat report assembled from the **currently loaded spawn JSON resources**. For each displayed entry, it shows:

- The Pokémon and spawn-entry identifier.
- Biomes (including tags) and whether the current location appears to match.
- Supported `day` / `night` and `isThundering` checks.
- Other datapack conditions as **informational text**, without claiming to evaluate them.

Up to **eight spawn variants** are displayed per use. The report is intended to help debug encounters, **not** to predict an exact spawn result.

### Interactive test commands

Chat buttons run commands in the `/mythstranslator` namespace:

| Command | Effect |
| --- | --- |
| `/mythstranslator biome minecraft:flower_forest` | Search for the specified biome and teleport there if found |
| `/mythstranslator day` | Set daytime |
| `/mythstranslator night` | Set nighttime |
| `/mythstranslator clear` | Clear weather |
| `/mythstranslator thunder` | Trigger a thunderstorm |

**Requirements:** the player must be in **Creative Mode** and have **command permission level 2 or higher**. The biome search runs in the current dimension, with a maximum radius of **6,400 blocks**. It can fail or lead to a location that does not meet all of Cobblemon's placement rules.

> [!WARNING]
> Time and weather commands actually modify the world and can affect **other players** on a server. These commands only prepare the environment; they **do not force a successful spawn**. After using them, click the key item again to test the original encounter logic.

### Survival Mode hints

In Survival Mode, the mod does not display the full checklist or the clickable test tools. When no parsed spawn entry appears compatible with the **basic biome, time, and thunder checks**, it displays a short warning. Other conditions may still block a spawn, and no warning is **not** a promise of success.

## Real-world validation and limitations

The developer has manually tested **approximately ten legendary Pokémon encounters** in-game on the supported versions. One confirmed case is **Ho-Oh summoned with the Clear Bell in a Flower Forest biome**. Not every key item, datapack variant, server configuration, or environmental condition has been tested.

Known diagnostic limitations:

- The diagnostic does **not** run Cobblemon's full internal spawn-condition evaluator or account for every probability and positioning rule.
- Time evaluation only explicitly understands `day` and `night`. Other `timeRange` values are not evaluated reliably and may appear permissive.
- Extra fields such as block requirements, special conditions, and other datapack constraints are displayed but not fully evaluated.
- The diagnostic only reads rules that use `condition.key_item` in the loaded `spawn_pool_world` JSON format.
- The on-screen report is capped at eight variants. Biome teleport buttons are not always available for large lists.
- A green check means **only that a basic check passed**, not that a spawn is guaranteed.
- The chat text is currently mainly in **French**; localization is planned.

Please report anything that differs from these documented behaviors.

## Build from source (developers)

**Tooling:** Java 21, Fabric Loom **1.11.8**, Gradle **8.14.3**, Yarn mappings for Minecraft 1.21.1. VS Code works well with the *Extension Pack for Java*.

```bash
# Ubuntu / Zorin OS
sudo apt install openjdk-21-jdk

# From the repository root
chmod +x build-java21.sh
./build-java21.sh clean build
```

The `build-java21.sh` launcher sets `JAVA_HOME` to `/usr/lib/jvm/java-21-openjdk-amd64` **only for that build process**. It does not change your system's default Java. It uses `./gradlew` if present, otherwise `/opt/gradle-8.14.3/bin/gradle`; it deliberately does **not** use the outdated Ubuntu Gradle 4.4.1 package.

The release artifact is the **remapped JAR** in `build/libs/`, without `-dev` or `-sources` in its filename. Set the version in `gradle.properties` (for example, `mod_version=0.5.0-beta.1`) and rebuild; **renaming the JAR does not change its embedded mod version**.

For Windows or another Linux JDK location, adapt the Java 21 launcher or use the Gradle wrapper with Java 21 configured for your development environment.

### Source layout

```text
src/main/java/io/github/valoliin/mythslegendstranslator/
├── MythsLegendsTranslator.java                 # Mod entrypoint and diagnostic registration
├── compatibility/
│   ├── LegacyApiBridge.java                   # Runtime adapters into the modern API
│   ├── CompatibilityStatus.java               # Known legacy signatures
│   ├── SpawnBucketTranslator.java             # Bucket-ID utility / extension point
│   └── SpawnResultTranslator.java             # Result-handling extension point
├── diagnostic/
│   └── SpawnDiagnostics.java                   # Chat report, checks, and test commands
└── mixin/
    ├── ForceSpawningUtilsMixin.java           # Target marker
    └── LegacyTranslationPlugin.java           # Mixin/ASM call-site translation

src/main/resources/
├── fabric.mod.json
├── mythslegends_translator.mixins.json
└── assets/mythslegends_translator/icon.png

build.gradle
build-java21.sh
gradle.properties
settings.gradle
docs/COMPATIBILITY.md
```

`SpawnBucketTranslator` and `SpawnResultTranslator` currently provide small extension points; the active call-site translation happens in **`LegacyTranslationPlugin` + `LegacyApiBridge`**.

### Troubleshooting

- **`No Pokemon matching ... conditions could be found for spawning`:** check the key, biome, time, weather, loaded datapack, and additional conditions. This message alone does not prove an API failure.
- **`NoSuchMethodError` or `Mixin` errors:** attach your latest log and list exact mod versions; an untested combination or competing mixin may be involved.
- **No Creative report:** ensure the item is a recognized `mythsandlegends:` item with a matching `condition.key_item` in an active spawn JSON.
- **Gradle 4.4.1 error:** run `./build-java21.sh`, not the old system `gradle` executable.
- **`getTopYInclusive()` compilation error:** in `SpawnDiagnostics.java`, use `world.getTopY()` with the Yarn API for 1.21.1 (this was corrected during V0.5 testing).

## Reporting issues and contributing

Open an issue in this repository with:

1. Minecraft, Fabric Loader, Fabric API, Cobblemon, Myths and Legends, and translator versions.
2. The key item used, expected Pokémon, biome, time, weather, and whether a custom datapack is involved.
3. Expected behavior vs. actual behavior, including whether the Pokémon appeared or the world crashed.
4. Relevant `logs/latest.log` or `crash-reports/*.txt` content (remove personal information before sharing).

Small, focused pull requests are welcome, particularly for additional spawn checks, translations, and tests. Please do not submit third-party mod JARs, copied assets, or the original datapack to this repository.

## Credits, licensing and disclaimer

- **Translator developer:** [Valoliin](https://github.com/Valoliin) — Minecraft username **Mioutwo_**.
- **Myths and Legends:** [D0ctorLeon](https://modrinth.com/mod/cobblemon-myths-and-legends-sidemod).
- **Cobblemon:** [The Cobblemon team](https://modrinth.com/mod/cobblemon).
- **Original datapack:** [Myths and Legends datapack](https://github.com/D0ctorLeon/mythsandlegends-datapack).

The independently authored translator source is available under the **MIT License** (see [LICENSE](LICENSE)). This license does **not** apply to Cobblemon, Myths and Legends, Pokémon intellectual property, or third-party datapacks.

Myths & Legends Translator is **unofficial** and is not affiliated with, sponsored by, or endorsed by D0ctorLeon, the Cobblemon team, Nintendo, Game Freak, or The Pokémon Company.
