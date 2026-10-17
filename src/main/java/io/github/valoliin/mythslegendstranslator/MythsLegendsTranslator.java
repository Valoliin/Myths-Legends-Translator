package io.github.valoliin.mythslegendstranslator;

import io.github.valoliin.mythslegendstranslator.diagnostic.SpawnDiagnostics;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MythsLegendsTranslator implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("mythslegends_translator");
    @Override public void onInitialize() {
        SpawnDiagnostics.register();
        LOG.info("Myths Legends Translator 0.5: pont API V0.4 conserve, diagnostic et actions de test actives");
    }
}
