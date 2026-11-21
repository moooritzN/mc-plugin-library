package io.github.moooritzn.plugins.tablistimage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class TablistHeaderTest {

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

    private static TextComponent glyph(Component component) {
        return flatten(component).stream()
                .filter(text -> text.content().equals(TablistHeader.GLYPH))
                .findFirst()
                .orElse(null);
    }

    @Test
    void imageUsesPackFontWithoutShadow() {
        TextComponent glyph = glyph(TablistHeader.image(0));
        assertNotNull(glyph);
        assertEquals(TablistHeader.FONT, glyph.font());
        assertEquals(ShadowColor.none(), glyph.shadowColor());
    }

    @Test
    void imageIsFollowedByBlankLines() {
        assertEquals(TablistHeader.GLYPH + "\n\n\n", plain(TablistHeader.image(3)));
        assertEquals(TablistHeader.GLYPH, plain(TablistHeader.image(0)));
    }

    @Test
    void blankLinesDoNotUsePackFont() {
        for (TextComponent text : flatten(TablistHeader.image(2))) {
            if (text.content().contains("\n")) {
                assertEquals(null, text.font());
            }
        }
    }

    @Test
    void renderReplacesImageTagAndKeepsSurroundingText() {
        Component header = TablistHeader.render("<bild><gold>Willkommen!", 2);
        assertEquals(TablistHeader.GLYPH + "\n\nWillkommen!", plain(header));
        assertNotNull(glyph(header));
    }

    @Test
    void emptyTextRendersEmptyComponent() {
        assertEquals(Component.empty(), TablistHeader.render("", 3));
    }

    @Test
    void textWithoutImageTagHasNoGlyph() {
        Component header = TablistHeader.render("<gold>Mein Server", 3);
        assertEquals("Mein Server", plain(header));
        assertTrue(glyph(header) == null);
    }
}
