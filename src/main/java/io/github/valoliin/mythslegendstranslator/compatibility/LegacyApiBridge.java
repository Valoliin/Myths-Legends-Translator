package io.github.valoliin.mythslegendstranslator.compatibility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adaptateur runtime, volontairement sans référence Java directe aux classes internes Cobblemon.
 * Les signatures de bytecode de Myths sont converties en Object/String par le plugin Mixin.
 * Les appels sont transmis aux méthodes existantes de Cobblemon 1.8.1.
 */
public final class LegacyApiBridge {
    private LegacyApiBridge() {}

    /** BestSpawnerConfig.getBuckets() (ancien List<SpawnBucket>).
     * Les pools de spawns par joueur sont maintenant exposés via BasicSpawner.getBuckets().
     * Pour l'ancien choix de pool forcé, utiliser la liste des world buckets configurés.
     */
    public static List<String> configuredBuckets(Object config) {
        Object result = call(config, "getWorldBuckets");
        if (!(result instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Cobblemon 1.8.1: getWorldBuckets() ne renvoie pas une Map");
        }
        List<String> buckets = new ArrayList<>();
        for (Object key : map.keySet()) if (key instanceof String s) buckets.add(s);
        return buckets;
    }

    /** PlayerSpawner.chooseBucket(SpawnCause,List) : maintenant String. */
    public static String chooseBucket(Object spawner, Object cause, List<?> influences) {
        return (String) call(spawner, "chooseBucket", cause, influences);
    }

    /** PlayerSpawner.getMatchingSpawns(SpawnBucket, SpawnablePosition) : maintenant String. */
    @SuppressWarnings("unchecked")
    public static List<?> matchingSpawns(Object spawner, String bucket, Object position) {
        return (List<?>) call(spawner, "getMatchingSpawns", bucket, position);
    }

    /** SpawnDetail.getBucket() : maintenant String. */
    public static String detailBucket(Object detail) {
        return (String) call(detail, "getBucket");
    }

    /** SpawnDetail.choose(position, SpawnBucket, selectionData) : maintenant String. */
    public static Object chooseSpawn(Object detail, Object position, String bucket, Object selectionData) {
        return call(detail, "choose", position, bucket, selectionData);
    }

    /** Ancien SpawnBucket.getName() -> identité de l'ID String. */
    public static String bucketName(String bucket) { return bucket; }

    private static Object call(Object target, String name, Object... args) {
        if (target == null) throw new IllegalArgumentException("Objet Cobblemon null pour " + name);
        for (Class<?> clazz = target.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
                Class<?>[] types = method.getParameterTypes();
                boolean matches = true;
                for (int i = 0; i < types.length; i++) {
                    if (args[i] != null && !types[i].isInstance(args[i])) { matches = false; break; }
                }
                if (!matches) continue;
                try {
                    method.setAccessible(true);
                    return method.invoke(target, args);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException ex) throw ex;
                    if (cause instanceof Error error) throw error;
                    throw new IllegalStateException("Échec Cobblemon " + name, cause);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Impossible d'appeler Cobblemon " + name, e);
                }
            }
        }
        throw new IllegalStateException("Méthode Cobblemon absente: " + target.getClass().getName() + "." + name + "/" + args.length);
    }
}
