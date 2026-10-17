package io.github.valoliin.mythslegendstranslator.compatibility;

import java.util.Locale;
import java.util.Objects;

/**
 * Conversion de l'identifiant des anciens SpawnBucket en identifiants String 1.8.1.
 * ATTENTION : aucun nom de bucket n'est inventé ; l'identifiant doit provenir du mod.
 * La conversion inverse d'un ID en objet SpawnBucket doit encore être implémentée.
 */
public final class SpawnBucketTranslator {
    private SpawnBucketTranslator() {}

    public static String normalizeId(String legacyBucketName) {
        Objects.requireNonNull(legacyBucketName, "legacyBucketName");
        String name = legacyBucketName.trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty()) throw new IllegalArgumentException("Bucket sans identifiant");
        return name;
    }
}
