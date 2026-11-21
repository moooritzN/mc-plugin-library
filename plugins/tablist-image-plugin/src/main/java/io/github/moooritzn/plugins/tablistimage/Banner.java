package io.github.moooritzn.plugins.tablistimage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import net.kyori.adventure.key.Key;

/**
 * Die Grafik als Schriftzeichen aus dem Resource Pack.
 *
 * <p>Die Werte stammen aus der beim Build generierten {@code bild.properties} (siehe {@code FontImageTask} in
 * build-logic): Das Bild ist dort in mehrere Kacheln zerlegt, die als Zeichenfolge nebeneinander erscheinen.
 *
 * @param font   Schriftart im Resource Pack
 * @param glyphs Zeichenfolge, die das komplette Bild ergibt
 * @param height Anzeigehöhe in GUI-Pixeln
 */
record Banner(Key font, String glyphs, int height) {

    /** Name der generierten Datei im Plugin-JAR. */
    static final String RESOURCE = "bild.properties";

    /** Höhe einer Textzeile in GUI-Pixeln. */
    static final int LINE_HEIGHT = 9;

    Banner {
        if (glyphs.isEmpty() || height <= 0) {
            throw new IllegalArgumentException("Ungültige Grafik: zeichen='" + glyphs + "', hoehe=" + height);
        }
    }

    /** Liest die generierte {@code bild.properties}. */
    static Banner load(InputStream in) throws IOException {
        Properties properties = new Properties();
        properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        String font = properties.getProperty("schriftart");
        String glyphs = properties.getProperty("zeichen");
        String height = properties.getProperty("hoehe");
        if (font == null || glyphs == null || height == null) {
            throw new IOException(RESOURCE + " ist unvollständig (schriftart, zeichen und hoehe erforderlich)");
        }
        try {
            return new Banner(Key.key(font), glyphs, Integer.parseInt(height.strip()));
        } catch (IllegalArgumentException e) {
            throw new IOException(RESOURCE + " enthält ungültige Werte: " + e.getMessage(), e);
        }
    }

    /** Anzahl Textzeilen, die die Grafik überdeckt. */
    int lines() {
        return (height + LINE_HEIGHT - 1) / LINE_HEIGHT;
    }
}
