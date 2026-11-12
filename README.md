# mc-plugin-library

Bibliothek und Sammlung von Minecraft-Server-Plugins für [Paper](https://papermc.io/).

- **`libs/`** – wiederverwendbare Library-Module (z.B. `mclib-core`), die von Plugins eingebunden werden
- **`plugins/`** – lauffähige Server-Plugins, die die Libraries nutzen

## Voraussetzungen

- Java 25 (wird bei Bedarf automatisch über die Gradle-Toolchain geladen)
- Kein lokales Gradle nötig – der Gradle Wrapper (`./gradlew`) ist enthalten

## Projektstruktur

```
.
├── build-logic/                 # Gradle-Convention-Plugins (gemeinsame Build-Konfiguration)
│   └── src/main/kotlin/
│       ├── mclib.java-conventions.gradle.kts     # Java 25, Paper-API, JUnit
│       ├── mclib.library-conventions.gradle.kts  # java-library + maven-publish
│       └── mclib.plugin-conventions.gradle.kts   # Shadow-JAR, Relocation, run-paper
├── gradle/libs.versions.toml    # zentrale Versionsverwaltung
├── libs/
│   └── core/                    # -> Gradle-Projekt :mclib-core
└── plugins/
    └── deathcounter-plugin/     # -> Gradle-Projekt :deathcounter-plugin
```

Neue Ordner unter `libs/` und `plugins/` werden automatisch in den Build aufgenommen,
sobald sie eine `build.gradle.kts` enthalten (siehe [settings.gradle.kts](settings.gradle.kts)).

## Bauen

```sh
./gradlew build
```

Die fertigen Plugin-JARs liegen danach unter `plugins/<name>/build/libs/<name>-<version>.jar`.
Die Library-Klassen werden per Shadow in das Plugin eingebettet und in das Package des
Plugins relocated (`<plugin-package>.libs.mclib`), damit sich mehrere Plugins nicht in die Quere kommen.

## Releases

Fertige Plugin-JARs gibt es unter **Releases** auf GitHub.

### Release

Wird ein Pull Request in `develop` gemergt, erstellt der [Release-Workflow](.github/workflows/release.yml)
das Release `v<version>` mit allen Plugin-JARs. Die Version kommt aus [gradle.properties](gradle.properties)
(ohne `-SNAPSHOT`, aus `0.2.0-SNAPSHOT` wird `v0.2.0`).

Existiert das Release für diese Version bereits, wird nichts veröffentlicht. Für ein neues Release
also im PR die Version in `gradle.properties` erhöhen.

### Dev-Build

Enthält die Commit-Message eines Pushes `[deploy]` (auf beliebigem Branch), baut der
[Dev-Build-Workflow](.github/workflows/dev-build.yml) die Plugins und ersetzt das Pre-Release `dev`.
Die JARs haben feste Download-Links:

```
https://github.com/moooritzN/mc-plugin-library/releases/download/dev/<plugin-name>-dev.jar
```

Die genaue Version (z.B. `0.2.0-dev.42+abc1234`) steht in der `plugin.yml` des JARs.

## Testserver starten

```sh
./gradlew :deathcounter-plugin:runServer
```

Lädt automatisch einen Paper-Server der passenden Version herunter und startet ihn
mit dem Plugin im Ordner `plugins/deathcounter-plugin/run/`.

## Neue Library anlegen

1. Ordner `libs/<name>/` mit `build.gradle.kts` erstellen:
   ```kotlin
   plugins {
       id("mclib.library-conventions")
   }
   ```
2. Code unter dem Package `io.github.moooritzn.mclib.<name>` ablegen.
3. In Plugins einbinden: `implementation(project(":mclib-<name>"))`

## Neues Plugin anlegen

1. Ordner `plugins/<name>-plugin/` mit `build.gradle.kts` erstellen:
   ```kotlin
   plugins {
       id("mclib.plugin-conventions")
   }

   dependencies {
       implementation(project(":mclib-core"))
   }
   ```
2. `src/main/resources/plugin.yml` anlegen (`${version}` und `${apiVersion}` werden beim Build ersetzt).
3. Code unter dem Package `io.github.moooritzn.plugins.<name>` ablegen
   (Ordnername ohne `-plugin`-Suffix und ohne Bindestriche).

## Versionen aktualisieren

Alle Versionen (Paper-API, Minecraft, Gradle-Plugins, JUnit) stehen in
[gradle/libs.versions.toml](gradle/libs.versions.toml). Die Minecraft-Version für
`api-version` und `runServer` wird aus der Paper-API-Version abgeleitet.
