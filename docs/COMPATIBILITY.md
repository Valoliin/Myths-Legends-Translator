# Compatibility internals (Minecraft 1.21.1 / Fabric)

This document describes the **active** translation approach in Myths & Legends Translator 0.5.x.

## Supported combination

- Myths and Legends **1.9.0 Fabric** (built for the Cobblemon 1.7-era spawning API).
- Cobblemon **1.8.1 Fabric**.
- Minecraft **1.21.1**, Java **21**, Fabric.

The compatibility change is localized to `com.github.d0ctorleon.mythsandlegends.utils.ForceSpawningUtils` and its force-spawn implementation. This is not a general-purpose compatibility layer for arbitrary mods or versions.

## Confirmed differences

| Myths and Legends expects | Cobblemon 1.8.1 provides | Adapter |
| --- | --- | --- |
| `BestSpawnerConfig.getBuckets(): List` | `BestSpawnerConfig.getWorldBuckets(): Map` | `configuredBuckets` |
| `PlayerSpawner.chooseBucket(...): SpawnBucket` | `PlayerSpawner.chooseBucket(...): String` | `chooseBucket` |
| `PlayerSpawner.getMatchingSpawns(SpawnBucket, position)` | `getMatchingSpawns(String, position)` | `matchingSpawns` |
| `SpawnDetail.getBucket(): SpawnBucket` | `SpawnDetail.getBucket(): String` | `detailBucket` |
| `SpawnDetail.choose(position, SpawnBucket, selection)` | `SpawnDetail.choose(position, String, selection)` | `chooseSpawn` |
| `SpawnBucket.getName(): String` | Bucket ID is already a String | `bucketName` |

Java links methods by exact descriptors. The problem cannot be solved reliably by catching one `NoSuchMethodError` after it occurs.

## Active implementation

1. `ForceSpawningUtilsMixin` declares the target class as a pseudo mixin.
2. `LegacyTranslationPlugin` performs a post-apply ASM pass over `forceSpawnInternal` and related lambda methods. It translates the obsolete `SpawnBucket` type in relevant method descriptors and replaces selected virtual calls with static calls to `LegacyApiBridge`.
3. `LegacyApiBridge` uses reflective dispatch to invoke methods available at runtime in Cobblemon 1.8.1.
4. The normal Myths and Legends routine continues, including its own selection logic and subsequent actions.

The plugin fails fast if too few replacements were made, to avoid silently running against an unexpected target class version. Logging indicates how many substitutions were applied.

`SpawnBucketTranslator` and `SpawnResultTranslator` are helper/extension classes, but **not** a complete bidirectional compatibility API. The actual working translator is the combination of the bytecode plugin and `LegacyApiBridge`.

## Diagnostic implementation

`SpawnDiagnostics` uses `UseItemCallback` to identify `mythsandlegends:` items, and reads live resources from the server resource manager under `cobblemon:spawn_pool_world/*.json`. It looks for conditions with `condition.key_item` equal to the item identifier.

It evaluates only biome IDs/tags, the `day`/`night` subset of `timeRange`, and `isThundering`; other conditions are included as unverified text. It returns `TypedActionResult.pass` so the original item-use logic can continue.

The `/mythstranslator` commands are registered through Fabric's command API and require permission level 2; the actions also verify Creative Mode. World time/weather changes are real server-side changes.

## Validation and known gaps

The developer reports approximately ten encounters tested successfully, including a Clear Bell / Ho-Oh encounter in a Flower Forest. Comprehensive coverage is not available yet. Known gaps include unrecognized time ranges, non-evaluated custom conditions, large biome lists, and untested newer versions of either mod.

**Note for source archives based on the original V0.5 draft:** Minecraft 1.21.1/Yarn uses `ServerWorld.getTopY()` rather than `getTopYInclusive()` in `SpawnDiagnostics.java`. The user has already applied that one-line compilation fix locally.

Never vendor, repackage, or include the original Cobblemon/Myths and Legends JARs or datapack in release artifacts.
