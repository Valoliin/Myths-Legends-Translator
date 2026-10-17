package io.github.valoliin.mythslegendstranslator.compatibility;

/**
 * Point d'extension de conversion de retour. Ne pas simuler un SpawnAction réussi :
 * Myths and Legends ne doit pas consommer les clés sans apparition effective.
 */
public final class SpawnResultTranslator {
    private SpawnResultTranslator() {}
    public static boolean isSafeToReportSuccess(boolean spawnActionExecuted) {
        return spawnActionExecuted;
    }
}
