package io.github.moooritzn.plugins.tablistimage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class BannerTest {

    private static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void loadsGeneratedProperties() throws IOException {
        Banner banner = Banner.load(stream("""
                # Generiert von FontImageTask aus bild.png – nicht von Hand bearbeiten
                schriftart=tablistimage:bild
                zeichen=\\uE000\\uF000\\uE001
                hoehe=60
                """));
        assertEquals(Key.key("tablistimage", "bild"), banner.font());
        assertEquals("\uE000\uF000\uE001", banner.glyphs());
        assertEquals(60, banner.height());
    }

    @Test
    void generatedFileInJarIsValid() throws IOException {
        // bild.properties wird vom Build (bannerFont) erzeugt und liegt auf dem Test-Classpath
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(Banner.RESOURCE)) {
            assertNotNull(in, Banner.RESOURCE + " fehlt – wurde bannerFont ausgeführt?");
            Banner banner = Banner.load(in);
            assertEquals(Key.key("tablistimage", "bild"), banner.font());
        }
    }

    @Test
    void rejectsIncompleteOrInvalidFiles() {
        assertThrows(IOException.class, () -> Banner.load(stream("schriftart=tablistimage:bild\nhoehe=60\n")));
        assertThrows(IOException.class,
                () -> Banner.load(stream("schriftart=tablistimage:bild\nzeichen=\\uE000\nhoehe=abc\n")));
        assertThrows(IOException.class,
                () -> Banner.load(stream("schriftart=tablistimage:bild\nzeichen=\\uE000\nhoehe=0\n")));
    }
}
