# CLAUDE.md

Bibliothek + Sammlung von Minecraft-Server-Plugins für **Paper** (Gradle-Multiprojekt, Kotlin DSL).

## Befehle

```sh
./gradlew build                          # alles bauen + testen
./gradlew :mclib-core:test               # Tests eines Moduls
./gradlew :deathcounter-plugin:runServer  # Paper-Testserver mit Plugin starten (lädt Server herunter)
```

Kein lokales Gradle verwenden – immer den Wrapper (`./gradlew`). Java 25 ist Pflicht (Paper-API 26.x verlangt es).

## Aufbau

- `libs/<name>/` → Gradle-Projekt `:mclib-<name>`, Plugin `mclib.library-conventions`, Package `io.github.moooritzn.mclib.<name>`
- `plugins/<name>-plugin/` → Gradle-Projekt `:<name>-plugin`, Plugin `mclib.plugin-conventions`, Package `io.github.moooritzn.plugins.<name>` (ohne `-plugin`-Suffix, ohne Bindestriche)
- Neue Ordner werden von [settings.gradle.kts](settings.gradle.kts) automatisch eingebunden, sobald sie eine `build.gradle.kts` haben – nichts manuell in `settings` eintragen.
- Build-Logik gehört in die Convention-Plugins unter [build-logic/src/main/kotlin/](build-logic/src/main/kotlin/), nicht in einzelne Modul-Builds.
- Resource Packs: `plugins/<name>/src/resourcepack/` (mit `pack.mcmeta`) wird von der Plugin-Convention automatisch zu `build/resourcepack/<name>-resourcepack.zip` gepackt (reproduzierbar, ohne Version im Namen) und von den Release-/Dev-Workflows mit hochgeladen. Bei Minecraft-Updates `max_format` in `pack.mcmeta` anheben.
- Bilder als Schrift-Glyphen (z.B. für Tab-Liste/Chat): Glyphen größer als 256×256 Pixel stellt Minecraft nicht dar (Ersatzzeichen ▯). Dafür den Task `FontImageTask` aus build-logic nutzen – er zerlegt das Bild in Kacheln und erzeugt Textur, Font-JSON und eine Properties-Datei mit der Zeichenfolge (Beispiel: `plugins/tablist-image-plugin/build.gradle.kts`).
- Alle Versionen nur in [gradle/libs.versions.toml](gradle/libs.versions.toml). Die Minecraft-Version (`api-version`, `runServer`) wird aus der `paper`-Version abgeleitet (`26.3.build.200-stable` → `26.3`).

## Wichtige Konventionen

- **Paper-API ist `compileOnly`** – wird vom Server bereitgestellt und darf nie ins JAR.
- **Relocation:** Plugins shaden die Libraries und relocaten `io.github.moooritzn.mclib` → `<plugin-package>.libs.mclib`. Deshalb dürfen Plugin-Packages **nicht** unter `io.github.moooritzn.mclib` liegen, und das Plugin-Package muss zum Ordnernamen passen (siehe oben).
- `plugin.yml` nutzt Platzhalter `${version}` und `${apiVersion}` – nicht hart codieren. Wegen `expand()` müssen echte `$` in `plugin.yml` als `\$` escaped werden.
- Commands über die Brigadier-Lifecycle-API registrieren (`LifecycleEvents.COMMANDS`), nicht über `commands:` in `plugin.yml`.
- Texte mit Adventure/MiniMessage (`Text.mm(...)`), keine Legacy-`§`-Farbcodes oder `ChatColor`.
- Libraries sollen Bukkit-unabhängige Logik testbar halten (z.B. `Clock` injizieren wie in `Cooldowns`). Tests: JUnit 6, liegen in `src/test/java`.
- Code-Kommentare, Javadoc und Nutzertexte auf Deutsch; Commit-Messages auf Englisch.
- Hauptbranch ist `develop`.

## Releases (GitHub Actions)

- **Release** (`release.yml`): bei jedem gemergten PR auf `develop`. Version = `version` aus `gradle.properties` ohne `-SNAPSHOT`. Existiert `v<version>` schon, wird übersprungen → für ein Release die Version im PR erhöhen. Keine Tags manuell pushen.
- **Dev-Build** (`dev-build.yml`): bei `[deploy]` in der Commit-Message (beliebiger Branch). Ersetzt das rollierende Pre-Release `dev`; Assets heißen `<plugin>-dev.jar`. `[deploy]` nur setzen, wenn der User es verlangt.

## Commit Messages
**IMPORTANT**: Do NOT include Claude Code attribution in commit messages. Specifically:
- Do NOT add "🤖 Generated with [Claude Code](https://claude.com/claude-code)"
- Do NOT add "Co-Authored-By: Claude <noreply@anthropic.com>"

Keep commit messages clean and focused on describing the actual changes made.