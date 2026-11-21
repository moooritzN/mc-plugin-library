package io.github.moooritzn.plugins.tablistimage;

import io.github.moooritzn.mclib.text.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

/**
 * Baut die Kopf- und Fußzeile der Tab-Liste.
 *
 * <p>Die Grafik ({@link Banner}) ist eine Zeichenfolge in einer Schriftart aus dem Resource Pack. Ohne
 * geladenes Pack würde der Client stattdessen Ersatzzeichen anzeigen.
 */
final class TablistHeader {

    /** MiniMessage-Tag, das in der Config durch die Grafik ersetzt wird. */
    static final String IMAGE_TAG = "bild";

    /** Wert für {@code blankLines}: Leerzeilen automatisch aus der Bildhöhe berechnen. */
    static final int AUTO = -1;

    private TablistHeader() {
    }

    /**
     * Die Grafik als Text-Komponente, gefolgt von {@code blankLines} Zeilenumbrüchen. Weiß und ohne Schatten,
     * damit die Farben des Bildes unverändert bleiben und kein dunkler Schatten dahinter liegt.
     */
    static Component image(Banner banner, int blankLines) {
        Component glyphs = Component.text(banner.glyphs())
                .font(banner.font())
                .color(NamedTextColor.WHITE)
                .shadowColor(ShadowColor.none());
        if (blankLines <= 0) {
            return glyphs;
        }
        return Component.text().append(glyphs).append(Component.text("\n".repeat(blankLines))).build();
    }

    /**
     * Parst einen MiniMessage-Text; {@code <bild>} wird durch die Grafik ersetzt.
     *
     * @param blankLines Leerzeilen nach der Grafik, oder {@link #AUTO}: so viele, dass nachfolgender Text
     *                   unter dem Bild beginnt – bzw. eine weniger, wenn nach {@code <bild>} nichts mehr folgt
     *                   (dann reicht es, dass die Kopfzeile so hoch wie das Bild ist)
     */
    static Component render(String miniMessage, Banner banner, int blankLines) {
        if (miniMessage.isEmpty()) {
            return Component.empty();
        }
        int lines = blankLines;
        if (lines == AUTO) {
            boolean imageIsLast = miniMessage.stripTrailing().endsWith("<" + IMAGE_TAG + ">");
            lines = imageIsLast ? banner.lines() - 1 : banner.lines();
        }
        return Text.mm(miniMessage, Placeholder.component(IMAGE_TAG, image(banner, lines)));
    }
}
