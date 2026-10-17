# Myths & Legends Translator 0.5.0-alpha — Valoliin / Mioutwo_

**Traducteur V0.4 inchangé.** Cette version ajoute une couche de diagnostics sans modifier les mécanismes d'apparition.

## Installation / compilation
`./build-java21.sh clean build` ; placer le JAR sans `-sources` dans mods, aux côtés de Cobblemon 1.8.1 et Myths and Legends 1.9.0. Le script choisit Java 21 sans changer le Java du système.

## Diagnostic
- Clic droit sur une clé en Créatif : règles de spawn lues dans les ressources JSON actives, statut biome / heure / météo et critères supplémentaires non évalués.
- Les liens du chat peuvent rechercher un biome proche puis téléporter en surface, ou régler heure et météo. **Réservé au Créatif avec permission niveau 2 (commandes autorisées).**
- Survie : bref avertissement uniquement si les conditions de base vérifiables sont défavorables. Les conditions avancées et les probabilités peuvent encore bloquer l'apparition.
- Le rapport montre les premières 8 variantes par objet (leurs biomes peuvent être très nombreux).

### LIMITATIONS IMPORTANTES
- Les indicateurs ne reproduisent PAS l'évaluation complète de Cobblemon. Une coche indique uniquement la correspondance de ces critères ; aucune garantie de spawn.
- `timeRange` gère actuellement `day` / `night` ; les autres valeurs ne sont pas évaluées.
- Les tags de biome sont évalués à partir des tags chargés. Le TP cherche dans le monde courant à 6400 blocs max ; il peut échouer et ne garantit pas toutes les exigences environnementales du spawn.
- Les messages sont émis au clic avant la routine de Myths : ils ne permettent pas d'affirmer que le spawn a réussi.
- Les boutons de temps et météo modifient réellement le monde, et exigent permissions.
- Compilation Gradle et test en jeu non réalisés dans cet environnement ; en cas d'erreur, transmettre la sortie de build.

Auteur : Valoliin (Minecraft Mioutwo_). Aucune affiliation au CROF.
