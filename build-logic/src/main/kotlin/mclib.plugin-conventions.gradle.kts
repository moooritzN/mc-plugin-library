// Konvention für lauffähige Server-Plugins unter plugins/
plugins {
    id("mclib.java-conventions")
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
}

val libs = the<VersionCatalogsExtension>().named("libs")
val paperVersion = libs.findVersion("paper").get().requiredVersion
// "26.3.build.200-stable" -> "26.3"
val minecraftVersion = paperVersion.substringBefore(".build")

val libraryPackage = "${project.group}.mclib"
val pluginPackage = "${project.group}.plugins.${project.name.removeSuffix("-plugin").replace("-", "")}"

tasks.processResources {
    val props = mapOf(
        "version" to project.version.toString(),
        "apiVersion" to minecraftVersion,
    )
    inputs.properties(props)
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}

tasks.jar {
    archiveClassifier = "plain"
}

tasks.shadowJar {
    archiveClassifier = ""
    // Library-Klassen in das Plugin-Package verschieben, damit mehrere Plugins
    // unterschiedliche Library-Versionen nutzen können, ohne sich zu stören.
    relocate(libraryPackage, "$pluginPackage.libs.mclib")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

// Optionales Resource Pack: Liegt unter src/resourcepack/ ein Pack (pack.mcmeta + assets/), wird es als
// build/resourcepack/<projekt>-resourcepack.zip gepackt. Der Dateiname enthält bewusst keine Version,
// damit die Download-URL im GitHub-Release stabil bleibt.
val resourcePackDir = layout.projectDirectory.dir("src/resourcepack")
if (resourcePackDir.file("pack.mcmeta").asFile.isFile) {
    val resourcePack = tasks.register<Zip>("resourcePack") {
        group = "build"
        description = "Packt src/resourcepack als Resource-Pack-ZIP."
        from(resourcePackDir)
        archiveFileName = "${project.name}-resourcepack.zip"
        destinationDirectory = layout.buildDirectory.dir("resourcepack")
        // Byte-identische ZIPs bei gleichem Inhalt -> gleicher SHA-1-Hash für Clients
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    tasks.assemble {
        dependsOn(resourcePack)
    }
}

tasks.runServer {
    minecraftVersion(minecraftVersion)
}
