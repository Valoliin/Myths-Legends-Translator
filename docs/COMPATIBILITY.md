# Spécification de compatibilité (analyse de bytecode)

Source : MythsAndLegends-fabric-1.9.0.jar et Cobblemon-fabric-1.8.1+1.21.1.jar fournis par l'utilisateur.

## Méthodes rencontrées dans `ForceSpawningUtils.forceSpawnInternal`

1. `BestSpawnerConfig.getBuckets(): List` — **supprimée**.
2. `PlayerSpawner.chooseBucket(SpawnCause, List): SpawnBucket` — nouveau type de retour **String**.
3. `PlayerSpawner.getMatchingSpawns(SpawnBucket, SpawnablePosition): List` — nouveau premier argument **String**.
4. `SpawnDetail.getBucket(): SpawnBucket` — nouveau type de retour **String**.
5. `SpawnDetail.choose(SpawnablePosition, SpawnBucket, SpawnSelectionData): SpawnAction` — deuxième argument désormais **String**.

Ces méthodes ont une résolution Java par descripteur ; on ne peut pas simplement ajouter un `catch(NoSuchMethodError)` autour de tous les appels ni changer une méthode Java isolée.

## Stratégie cible

- Conserver le code original de sélection, validation et consommation dans Myths and Legends.
- Traiter les anciens appels en utilisant des injections/redirections ciblées dans le bytecode (Mixins) ou des méthodes de compatibilité ajoutées aux classes cibles.
- Éviter les copies de classes Cobblemon ; pour le type supprimé `SpawnBucket`, modéliser le contrat minimal requis sans créer de collisions.
- Garantir les correspondances de catégories et des noms exacts (normalisation uniquement si l'API le permet).
- Faire passer les tests de démarrage, clés invalides, clés valides, consommation et cooldown, biomes et météo.

## État d'exécution

**Cette version ne réalise pas encore les cinq adaptations.** Le Mixin temporaire à HEAD neutralise l'ancien chemin qui crashe. `SpawnBucketTranslator` et `SpawnResultTranslator` sont des primitives de futur pont et non un patch branché. Ne pas publier en indiquant « compatible » ou « corrige le spawn ».
