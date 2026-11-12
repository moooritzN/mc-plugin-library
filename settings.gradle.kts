rootProject.name = "mc-plugin-library"

pluginManagement {
    includeBuild("build-logic")
}

plugins {
    // Lädt fehlende JDKs (Toolchains) automatisch herunter
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

// Library-Module: libs/<name>  ->  Projekt ":mclib-<name>"
file("libs").listFiles()
    ?.filter { File(it, "build.gradle.kts").isFile }
    ?.sortedBy { it.name }
    ?.forEach { dir ->
        include("mclib-${dir.name}")
        project(":mclib-${dir.name}").projectDir = dir
    }

// Plugins: plugins/<name>  ->  Projekt ":<name>"
file("plugins").listFiles()
    ?.filter { File(it, "build.gradle.kts").isFile }
    ?.sortedBy { it.name }
    ?.forEach { dir ->
        include(dir.name)
        project(":${dir.name}").projectDir = dir
    }
