import java.awt.Image
import java.awt.image.BufferedImage
import java.util.Locale
import javax.imageio.ImageIO
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Macht aus einem Bild eine Resource-Pack-Schriftart, mit der sich das Bild als Text anzeigen lässt
 * (z.B. in der Tab-Liste).
 *
 * Minecraft legt Schrift-Glyphen auf 256×256 Pixel großen Texturen ab; größere Glyphen werden nicht
 * dargestellt (Ersatzzeichen). Deshalb wird das Bild in nebeneinanderliegende Kacheln von höchstens
 * 256×256 Pixeln zerlegt (zu hohe Bilder werden vorher verkleinert). Erzeugt werden:
 *
 * - [packOutput]: `assets/<namespace>/textures/font/<fontName>.png` und `assets/<namespace>/font/<fontName>.json`
 * - [resourcesOutput]: `<fontName>.properties` mit der Zeichenfolge (`zeichen`) und Höhe (`hoehe`) für das Plugin
 *
 * Die Zeichenfolge enthält zwischen den Kacheln ein Leerzeichen mit Breite -1, das den automatischen
 * 1-Pixel-Abstand zwischen Glyphen aufhebt, damit keine Fugen entstehen.
 */
@CacheableTask
abstract class FontImageTask : DefaultTask() {

    /** Quellbild (PNG, Transparenz erlaubt). */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val image: RegularFileProperty

    /** Namespace im Resource Pack, z.B. `tablistimage`. */
    @get:Input
    abstract val namespace: Property<String>

    /** Name von Schriftart und Textur, z.B. `bild`. */
    @get:Input
    abstract val fontName: Property<String>

    /** Anzeigehöhe im Spiel in GUI-Pixeln (eine Textzeile ist 9 Pixel hoch). */
    @get:Input
    abstract val displayHeight: Property<Int>

    /** Pixel oberhalb der Grundlinie; 7 = Bild beginnt oben in der Textzeile. */
    @get:Input
    abstract val ascent: Property<Int>

    @get:OutputDirectory
    abstract val packOutput: DirectoryProperty

    @get:OutputDirectory
    abstract val resourcesOutput: DirectoryProperty

    init {
        ascent.convention(7)
    }

    @TaskAction
    fun generate() {
        val height = displayHeight.get()
        if (height <= 0 || ascent.get() > height) {
            throw GradleException("displayHeight muss > 0 und >= ascent sein (displayHeight=$height, ascent=${ascent.get()})")
        }
        val sourceFile = image.get().asFile
        var img = ImageIO.read(sourceFile)?.let(::toArgb)
            ?: throw GradleException("Bild konnte nicht gelesen werden: $sourceFile")
        if (img.height > MAX_SIZE) {
            img = scale(img, MAX_SIZE.toDouble() / img.height)
        }

        val tiles = ceilDiv(img.width, MAX_SIZE)
        val tileWidth = ceilDiv(img.width, tiles)
        val sheet = BufferedImage(tileWidth * tiles, img.height, BufferedImage.TYPE_INT_ARGB)
        sheet.createGraphics().apply { drawImage(img, 0, 0, null); dispose() }
        // Minecraft kürzt Glyphen um durchsichtige Spalten am rechten Rand. Ein fast unsichtbarer Pixel
        // (Alpha 1/255) in der letzten Spalte jeder Kachel (außer der letzten) erhält die volle Breite.
        for (tile in 0 until tiles - 1) {
            val x = (tile + 1) * tileWidth - 1
            if ((0 until sheet.height).none { y -> sheet.getRGB(x, y) ushr 24 != 0 }) {
                sheet.setRGB(x, sheet.height - 1, 0x01000000)
            }
        }

        val ns = namespace.get()
        val name = fontName.get()
        val pack = packOutput.get().asFile.apply { deleteRecursively(); mkdirs() }
        val textureFile = pack.resolve("assets/$ns/textures/font/$name.png").apply { parentFile.mkdirs() }
        ImageIO.write(sheet, "png", textureFile)

        val glyphs = (0 until tiles).map { FIRST_GLYPH + it }
        val chars = glyphs.joinToString("") { escape(it) }
        pack.resolve("assets/$ns/font/$name.json").apply { parentFile.mkdirs() }.writeText(
            """
            |{
            |  "providers": [
            |    {
            |      "type": "bitmap",
            |      "file": "$ns:font/$name.png",
            |      "height": $height,
            |      "ascent": ${ascent.get()},
            |      "chars": ["$chars"]
            |    },
            |    {
            |      "type": "space",
            |      "advances": { "${escape(NEGATIVE_SPACE)}": -1 }
            |    }
            |  ]
            |}
            |""".trimMargin()
        )

        val sequence = glyphs.joinToString(escape(NEGATIVE_SPACE)) { escape(it) }
        val resources = resourcesOutput.get().asFile.apply { deleteRecursively(); mkdirs() }
        resources.resolve("$name.properties").writeText(
            """
            |# Generiert von FontImageTask aus ${sourceFile.name} – nicht von Hand bearbeiten
            |schriftart=$ns:$name
            |zeichen=$sequence
            |hoehe=$height
            |""".trimMargin()
        )
        logger.lifecycle("Schrift-Bild $ns:$name: ${img.width}x${img.height} px in $tiles Kachel(n) à ${tileWidth}x${img.height} px")
    }

    private companion object {
        const val MAX_SIZE = 256
        const val FIRST_GLYPH = 0xE000
        const val NEGATIVE_SPACE = 0xF000

        fun ceilDiv(a: Int, b: Int) = (a + b - 1) / b

        /** `\uXXXX`-Escape, gültig in JSON und Java-Properties. */
        fun escape(codePoint: Int) = "\\u" + "%04X".format(Locale.ROOT, codePoint)

        fun toArgb(source: BufferedImage): BufferedImage {
            if (source.type == BufferedImage.TYPE_INT_ARGB) return source
            return BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB).apply {
                createGraphics().apply { drawImage(source, 0, 0, null); dispose() }
            }
        }

        fun scale(source: BufferedImage, factor: Double): BufferedImage {
            val width = maxOf(1, Math.round(source.width * factor).toInt())
            val height = maxOf(1, Math.round(source.height * factor).toInt())
            val scaled = source.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING)
            return BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
                createGraphics().apply { drawImage(scaled, 0, 0, null); dispose() }
            }
        }
    }
}
