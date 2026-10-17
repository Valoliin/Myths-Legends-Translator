<div align="center">

<img src="assets/translator-icon.png" alt="Icône Myths & Legends Translator" width="192" height="192">

# Myths & Legends Translator

**Un traducteur d'API non officiel entre Myths and Legends et Cobblemon.**

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)
![Fabric](https://img.shields.io/badge/Loader-Fabric-DDBB7B)
![Java](https://img.shields.io/badge/Java-21-5372A1)
![Statut](https://img.shields.io/badge/Statut-Bêta-orange)
![Licence](https://img.shields.io/badge/Licence-MIT-blue)

[English](README.md) · [Documentation technique](docs/COMPATIBILITY.md)

</div>

## Présentation

**Myths & Legends Translator** est un mod Fabric indépendant qui corrige des incompatibilités d'API entre **Myths and Legends 1.9.0** et **Cobblemon 1.8.1**, pour **Minecraft 1.21.1**.

Myths and Legends 1.9.0 utilise des méthodes de génération des Pokémon datant d'une ancienne version de Cobblemon. Certaines ont été supprimées ou modifiées : lors de l'utilisation d'une clé légendaire, Minecraft peut notamment planter avec `NoSuchMethodError` et `BestSpawnerConfig.getBuckets()`.

L'idée est celle d'un **traducteur**, pas d'une réécriture : Myths and Legends continue de gérer les clés et la logique de rencontre ; Cobblemon continue de gérer l'apparition des Pokémon. Notre mod ne fait qu'adapter les appels Java incompatibles et leurs paramètres/résultats.

> [!IMPORTANT]
> Projet **non officiel** et en **bêta**. Il ne fournit ni les deux mods originaux ni le datapack d'apparition. Ils doivent être téléchargés séparément. Tous les Pokémon et toutes les conditions n'ont pas été testés.

## Fonctionnalités

- **Pont d'API :** adaptation des appels de spawn incompatibles avec Cobblemon 1.8.1.
- **Respect des mécaniques originales :** aucune liste fixe « clé → Pokémon » ni nouveau moteur de spawn.
- **Diagnostic en Créatif :** affichage dans le chat des variantes d'apparition déclarées, des Pokémon concernés, des biomes et des vérifications de base de l'heure et de la météo.
- **Conditions supplémentaires :** affichées lorsqu'elles existent dans les JSON, même si le diagnostic ne sait pas encore les évaluer.
- **Outils cliquables :** rechercher un biome et s'y téléporter, mettre le jour/la nuit ou modifier la météo.
- **Indice en Survie :** avertissement bref et non détaillé lorsque les conditions de base paraissent incorrectes.

## Versions et dépendances

| Élément | Version / précision |
| --- | --- |
| Minecraft Java | **1.21.1** |
| Mod loader | **Fabric** |
| Cobblemon | **1.8.1**, seule version actuellement validée |
| Myths and Legends | **1.9.0 Fabric** |
| Fabric API | Nécessaire pour les événements et commandes du diagnostic |
| Java | **21** |
| Datapack Myths and Legends | Nécessaire pour les règles de spawn des objets-clés ; datapack personnalisé compatible possible |

Les autres versions et autres loaders (Forge, NeoForge) ne sont pas garantis. Le fait qu'une version plus récente passe un contrôle de dépendances Fabric n'implique pas qu'elle fonctionne.

### Liens vers les projets originaux

- [Cobblemon — Modrinth](https://modrinth.com/mod/cobblemon)
- [Myths and Legends — Modrinth](https://modrinth.com/mod/cobblemon-myths-and-legends-sidemod)
- [Datapack officiel — GitHub](https://github.com/D0ctorLeon/mythsandlegends-datapack)
- [Datapack officiel — CurseForge](https://www.curseforge.com/minecraft/data-packs/mythsandlegends-datapack/files)

Le datapack officiel possède sa **propre licence et ses propres restrictions de redistribution**. Il n'est pas intégré à notre projet.

## Installation pour les joueurs

1. Installer **Minecraft 1.21.1** avec **Fabric Loader**.
2. Ajouter **Fabric API**, **Cobblemon 1.8.1** et **Myths and Legends 1.9.0 (Fabric)**.
3. Ajouter dans `mods/` le fichier **JAR normal** de Myths & Legends Translator, **pas** le fichier `-dev.jar` ni `-sources.jar`.
4. Ajouter séparément le datapack officiel dans le dossier `datapacks/` du monde, ou utiliser un datapack compatible.
5. Démarrer le monde. En cas de problème avec les apparitions, vérifier `/datapack list enabled`.

Pensez à sauvegarder votre monde avant toute modification de mods. Sur un serveur, utilisez des versions cohérentes entre serveur et clients.

## Comment fonctionne le traducteur ?

```text
Myths and Legends 1.9.0
   └─ Le joueur utilise une clé légendaire
       └─ Appel à l'ancienne API de spawn de Cobblemon
           └─ Myths & Legends Translator
               ├─ Redirection ciblée via Mixin et ASM
               ├─ Conversion de SpawnBucket en identifiant String
               ├─ Appel de l'API Cobblemon 1.8.1
               └─ Retour des résultats à Myths and Legends
                   └─ Le fonctionnement original continue
```

Le système utilise un **plugin Mixin** pour réécrire certaines instructions dans `ForceSpawningUtils` et une classe Java `LegacyApiBridge` pour appeler les méthodes disponibles via réflexion. Il ne modifie pas les fichiers JAR originaux et ne remplace pas la logique de sélection des rencontres.

| Ancien appel Cobblemon | Traduction actuelle |
| --- | --- |
| `BestSpawnerConfig.getBuckets()` | Utilisation des identifiants de `getWorldBuckets()` |
| `PlayerSpawner.chooseBucket(...)` | Type de retour `String` au lieu de `SpawnBucket` |
| `PlayerSpawner.getMatchingSpawns(SpawnBucket, ...)` | Identifiant `String` |
| `SpawnDetail.getBucket()` | Type de retour `String` |
| `SpawnDetail.choose(..., SpawnBucket, ...)` | Identifiant `String` |
| `SpawnBucket.getName()` | Identifiant directement utilisé |

Ce n'est **pas** un traducteur universel de toutes les versions de Cobblemon. Voir [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) pour le détail technique.

## Diagnostic Créatif

Lorsqu'un joueur utilise un objet-clé de Myths and Legends en **Créatif**, le mod lit les règles JSON d'apparition actives (`data/cobblemon/spawn_pool_world/*.json`), cherche les entrées liées à cet objet par `condition.key_item`, et affiche jusqu'à **huit variantes** dans le chat.

Il affiche le Pokémon, l'identifiant de l'entrée, les biomes, les contrôles simples de jour/nuit/orage et les critères supplémentaires non évalués.

**Une coche verte ne garantit pas un spawn.** Certaines exigences sont gérées uniquement par Cobblemon ; le diagnostic ne reproduit pas l'intégralité de son évaluateur.

### Boutons et commandes

| Commande | Action |
| --- | --- |
| `/mythstranslator biome minecraft:flower_forest` | Chercher ce biome et essayer d'y téléporter le joueur |
| `/mythstranslator day` | Mettre le jour |
| `/mythstranslator night` | Mettre la nuit |
| `/mythstranslator clear` | Mettre le beau temps |
| `/mythstranslator thunder` | Déclencher un orage |

Ces actions nécessitent **le mode Créatif et les permissions de commande de niveau 2 minimum**. La recherche se fait dans la dimension actuelle sur un rayon maximal de **6 400 blocs**, sans garantir un terrain ou des conditions de spawn parfaitement adaptés.

> [!WARNING]
> Modifier l'heure et la météo **modifie réellement le monde**, y compris pour les autres joueurs. Les boutons ne déclenchent pas directement les rencontres : il faut réutiliser la clé pour vérifier le fonctionnement original.

## Message en Survie

En Survie, le joueur ne reçoit pas de diagnostic détaillé. Lorsque toutes les entrées examinées semblent incompatibles avec les contrôles élémentaires de biome, heure et orage, il voit seulement un avertissement court. Les autres critères ne sont pas révélés.

## État des tests et limites

Le développeur a validé manuellement **environ dix rencontres légendaires** sur la combinaison de versions indiquée, dont **Ho-Oh avec la Clear Bell dans une forêt fleurie**. Il reste des Pokémon, des objets et des configurations non testés.

Limites connues :

- Le diagnostic n'évalue pas tous les critères internes de Cobblemon et ne tient pas compte de toutes les probabilités.
- Les périodes `day` et `night` sont reconnues ; d'autres formats de `timeRange` sont actuellement mal ou non évalués et peuvent apparaître validés à tort.
- Les restrictions supplémentaires sont affichées en texte mais ne sont pas entièrement vérifiées.
- Seules les ressources JSON chargées de `spawn_pool_world` utilisant `condition.key_item` sont exploitées par le diagnostic.
- Huit variantes maximum sont affichées ; les boutons de TP peuvent manquer pour certaines listes de biomes.
- Le diagnostic et les messages sont actuellement principalement **en français**.

## Compilation et développement

Outils : **Java 21**, **Gradle 8.14.3**, **Fabric Loom 1.11.8**, Yarn pour Minecraft 1.21.1. Projet compatible avec VS Code et l'extension Java.

Sous Zorin OS ou Ubuntu :

```bash
sudo apt install openjdk-21-jdk
chmod +x build-java21.sh
./build-java21.sh clean build
```

Le script configure **Java 21 uniquement pour la compilation**, sans changer le Java par défaut du système. Il utilise `./gradlew` s'il existe, sinon `/opt/gradle-8.14.3/bin/gradle`. Il évite Gradle 4.4.1, trop ancien.

Le JAR distribuable se trouve dans `build/libs/` : utiliser celui **sans** suffixe `-dev` ou `-sources`. La version doit être modifiée dans `gradle.properties` via `mod_version`, puis recompilée. **Renommer le fichier JAR ne change pas sa version interne.**

### Structure du code

```text
src/main/java/io/github/valoliin/mythslegendstranslator/
├── MythsLegendsTranslator.java
├── compatibility/
│   ├── LegacyApiBridge.java
│   ├── CompatibilityStatus.java
│   ├── SpawnBucketTranslator.java
│   └── SpawnResultTranslator.java
├── diagnostic/
│   └── SpawnDiagnostics.java
└── mixin/
    ├── ForceSpawningUtilsMixin.java
    └── LegacyTranslationPlugin.java

src/main/resources/
├── fabric.mod.json
├── mythslegends_translator.mixins.json
└── assets/mythslegends_translator/icon.png
```

Le code de traduction actif se trouve surtout dans **`LegacyTranslationPlugin`** et **`LegacyApiBridge`**. Les classes `SpawnBucketTranslator` et `SpawnResultTranslator` sont encore des points d'extension.

### En cas d'erreur

- **`No Pokemon matching ...` :** vérifier le datapack actif et les véritables conditions de biome, heure, météo et autres restrictions.
- **`NoSuchMethodError` ou erreur de Mixin :** relever le crash report et les versions exactes des mods.
- **Pas de diagnostic Créatif :** vérifier que l'objet possède une entrée avec `condition.key_item` dans les JSON actifs.
- **Gradle 4.4.1 :** utiliser `./build-java21.sh`.
- **Erreur `getTopYInclusive()` :** remplacer par `world.getTopY()` dans `SpawnDiagnostics.java` pour les mappings Yarn de 1.21.1 ; correction validée pendant les tests V0.5.

## Signaler un bug / participer

Pour un rapport de bug, fournir les versions de Minecraft, Fabric, Fabric API, Cobblemon, Myths and Legends et Translator ; l'objet utilisé ; le Pokémon ciblé ; le biome, l'heure et la météo ; la présence d'un datapack personnalisé ; les étapes pour reproduire le problème ; enfin `logs/latest.log` ou le crash report (en masquant les informations personnelles).

Les contributions à la traduction, aux diagnostics, aux traductions linguistiques et aux tests sont les bienvenues. Merci de ne pas joindre les JAR des mods tiers ni redistribuer le datapack original dans ce dépôt.

## Crédits, licence et indépendance

- **Développeur du traducteur :** [Valoliin](https://github.com/Valoliin) — pseudo Minecraft **Mioutwo_**.
- **Myths and Legends :** [D0ctorLeon](https://modrinth.com/mod/cobblemon-myths-and-legends-sidemod).
- **Cobblemon :** [équipe Cobblemon](https://modrinth.com/mod/cobblemon).
- **Datapack officiel :** [Myths and Legends datapack](https://github.com/D0ctorLeon/mythsandlegends-datapack).

Le code original de ce projet est proposé sous licence **MIT** (voir [LICENSE](LICENSE)). Cette licence ne s'étend pas aux mods tiers, au datapack ni aux propriétés intellectuelles Pokémon.

**Projet indépendant et non officiel**, sans affiliation ni approbation de D0ctorLeon, des développeurs de Cobblemon, de Nintendo, de Game Freak ou de The Pokémon Company.
