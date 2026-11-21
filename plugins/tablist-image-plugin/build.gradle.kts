plugins {
    id("mclib.plugin-conventions")
}

dependencies {
    implementation(project(":mclib-core"))
}

// Banner aus src/banner/bild.png in Kacheln zerlegen (siehe build-logic/.../FontImageTask.kt):
// Textur + Font-JSON landen im Resource Pack, die Zeichenfolge als bild.properties im Plugin-JAR.
val bannerFont = tasks.register<FontImageTask>("bannerFont") {
    image = layout.projectDirectory.file("src/banner/bild.png")
    namespace = "tablistimage"
    fontName = "bild"
    // Anzeigehöhe in der Tab-Liste in GUI-Pixeln (eine Textzeile = 9 Pixel)
    displayHeight = 60
    packOutput = layout.buildDirectory.dir("generated/banner/pack")
    resourcesOutput = layout.buildDirectory.dir("generated/banner/resources")
}

tasks.named<Zip>("resourcePack") {
    from(bannerFont.flatMap { it.packOutput })
}

sourceSets.main {
    resources.srcDir(bannerFont.flatMap { it.resourcesOutput })
}
