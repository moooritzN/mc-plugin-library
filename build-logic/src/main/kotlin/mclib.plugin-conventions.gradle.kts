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

tasks.runServer {
    minecraftVersion(minecraftVersion)
}
