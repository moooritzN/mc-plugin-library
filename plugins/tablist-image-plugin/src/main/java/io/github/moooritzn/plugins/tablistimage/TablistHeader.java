package io.github.moooritzn.plugins.tablistimage;

import io.github.moooritzn.mclib.text.Text;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

/**
 * Baut die Kopf- und Fußzeile der Tab-Liste.
 *
 * <p>Die Grafik ist ein einzelnes Zeichen ({@value #GLYPH_DESCRIPTION}) in der Schriftart
 * {@code tablistimage:bild}, die das Resource Pack mitbringt (siehe {@code src/resourcepack/}).
 * Ohne geladenes Pack würde der Client stattdessen ein Ersatzzeichen anzeigen.
 */
final class TablistHeader {

    /** Schriftart aus dem Resource Pack ({@code assets/tablistimage/font/bild.json}). */
    static final Key FONT = Key.key("tablistimage", "bild");

    /** Zeichen, dem die Grafik in der Schriftart zugeordnet ist (Private-Use-Bereich). */
    static final String GLYPH = "";

    private static final String GLYPH_DESCRIPTION = "U+E000";

    /** MiniMessage-Tag, das in der Config durch die Grafik ersetzt wird. */
    static final String IMAGE_TAG = "bild";

    private TablistHeader() {
    }

    /**
     * Die Grafik als Text-Komponente, gefolgt von {@code blankLines} Zeilenumbrüchen, damit nachfolgender
     * Text unterhalb der Grafik beginnt. Weiß und ohne Schatten, damit die Farben des Bildes unverändert
     * bleiben und kein dunkler Schatten dahinter liegt.
     */
    static Component image(int blankLines) {
        Component glyph = Component.text(GLYPH)
                .font(FONT)
                .color(NamedTextColor.WHITE)
                .shadowColor(ShadowColor.none());
        if (blankLines <= 0) {
            return glyph;
        }
        return Component.text().append(glyph).append(Component.text("\n".repeat(blankLines))).build();
    }

    /** Parst einen MiniMessage-Text; {@code <bild>} wird durch die Grafik ersetzt. */
    static Component render(String miniMessage, int blankLines) {
        if (miniMessage.isEmpty()) {
            return Component.empty();
        }
        return Text.mm(miniMessage, Placeholder.component(IMAGE_TAG, image(blankLines)));
    }
}
