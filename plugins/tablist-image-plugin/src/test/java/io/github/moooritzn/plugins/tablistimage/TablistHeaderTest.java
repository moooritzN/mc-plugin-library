package io.github.moooritzn.plugins.tablistimage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class TablistHeaderTest {

    private static final String GLYPHS = "";
    /** 60 Pixel hoch = 7 Textzeilen (à 9 Pixel). */
    private static final Banner BANNER = new Banner(Key.key("tablistimage", "bild"), GLYPHS, 60);

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static List<TextComponent> flatten(Component component) {
        List<TextComponent> result = new ArrayList<>();
        collect(component, result);
        return result;
    }

    private static void collect(Component component, List<TextComponent> result) {
        if (component instanceof TextComponent text) {
            result.add(text);
        }
        component.children().forEach(child -> collect(child, result));
    }

    private static TextComponent glyphs(Component component) {
        return flatten(component).stream()
                .filter(text -> text.content().equals(GLYPHS))
                .findFirst()
                .orElse(null);
    }

    @Test
    void imageUsesPackFontWithoutShadow() {
        TextComponent glyphs = glyphs(TablistHeader.image(BANNER, 0));
        assertNotNull(glyphs);
        assertEquals(BANNER.font(), glyphs.font());
        assertEquals(ShadowColor.none(), glyphs.shadowColor());
    }

    @Test
    void blankLinesDoNotUsePackFont() {
        for (TextComponent text : flatten(TablistHeader.image(BANNER, 2))) {
            if (text.content().contains("\n")) {
                assertNull(text.font());
            }
        }
    }

    @Test
    void bannerCoversSevenLines() {
        assertEquals(7, BANNER.lines());
        assertEquals(1, new Banner(BANNER.font(), GLYPHS, 9).lines());
        assertEquals(2, new Banner(BANNER.font(), GLYPHS, 10).lines());
    }

    @Test
    void autoWithImageOnlyFillsHeaderToImageHeight() {
        // 6 Leerzeilen -> Kopfzeile hat 7 Zeilen (63 px) für ein 60 px hohes Bild
        assertEquals(GLYPHS + "\n".repeat(6), plain(TablistHeader.render("<bild>", BANNER, TablistHeader.AUTO)));
    }

    @Test
    void autoWithTextAfterImageStartsTextBelowImage() {
        // 7 Leerzeilen -> Text beginnt bei 63 px, also unter dem 60 px hohen Bild
        Component header = TablistHeader.render("<bild><gold>Willkommen!", BANNER, TablistHeader.AUTO);
        assertEquals(GLYPHS + "\n".repeat(7) + "Willkommen!", plain(header));
        assertNotNull(glyphs(header));
    }

    @Test
    void explicitBlankLinesOverrideAuto() {
        assertEquals(GLYPHS + "\n\n", plain(TablistHeader.render("<bild>", BANNER, 2)));
    }

    @Test
    void emptyTextRendersEmptyComponent() {
        assertEquals(Component.empty(), TablistHeader.render("", BANNER, TablistHeader.AUTO));
    }

    @Test
    void textWithoutImageTagHasNoGlyphs() {
        Component header = TablistHeader.render("<gold>Mein Server", BANNER, TablistHeader.AUTO);
        assertEquals("Mein Server", plain(header));
        assertNull(glyphs(header));
    }
}
