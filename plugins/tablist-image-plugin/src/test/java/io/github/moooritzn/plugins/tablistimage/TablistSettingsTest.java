package io.github.moooritzn.plugins.tablistimage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TablistSettingsTest {

    private static TablistSettings withSha1(String sha1) {
        return new TablistSettings(" https://example.org/pack.zip ", sha1, false, "", "<bild>", "", "", 3);
    }

    @Test
    void emptySha1MeansComputeHash() {
        TablistSettings settings = withSha1("  ");
        assertFalse(settings.hasSha1());
        assertEquals("", settings.packSha1());
    }

    @Test
    void sha1IsNormalisedToLowerCase() {
        TablistSettings settings = withSha1("ABCDEF0123456789ABCDEF0123456789ABCDEF01");
        assertTrue(settings.hasSha1());
        assertEquals("abcdef0123456789abcdef0123456789abcdef01", settings.packSha1());
    }

    @Test
    void urlIsTrimmed() {
        assertEquals("https://example.org/pack.zip", withSha1("").packUrl());
    }

    @Test
    void invalidSha1IsRejected() {
        assertThrows(IllegalArgumentException.class, () -> withSha1("123"));
        assertThrows(IllegalArgumentException.class, () -> withSha1("z".repeat(40)));
    }

    @Test
    void negativeBlankLinesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new TablistSettings("https://example.org/p.zip", "", false, "", "", "", "", -1));
    }
}
