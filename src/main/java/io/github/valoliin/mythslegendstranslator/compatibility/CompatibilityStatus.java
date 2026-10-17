package io.github.valoliin.mythslegendstranslator.compatibility;

/** Inventaire des incompatibilités repérées dans les JAR fournis. */
public final class CompatibilityStatus {
    private CompatibilityStatus() {}
    public static final String[] LEGACY_SIGNATURES = {
        "BestSpawnerConfig.getBuckets(): java.util.List",
        "PlayerSpawner.chooseBucket(SpawnCause, List): SpawnBucket -> String",
        "PlayerSpawner.getMatchingSpawns(SpawnBucket, SpawnablePosition): List -> argument String",
        "SpawnDetail.getBucket(): SpawnBucket -> String",
        "SpawnDetail.choose(SpawnablePosition, SpawnBucket, SpawnSelectionData): SpawnAction -> argument String"
    };
}
