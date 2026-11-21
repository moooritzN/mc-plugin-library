package io.github.moooritzn.plugins.tablistimage;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Einstellungen aus der config.yml.
 *
 * @param packUrl           Download-URL des Resource Packs
 * @param packSha1          SHA-1 des Packs in Kleinbuchstaben, oder leer (wird dann berechnet)
 * @param packRequired      ob Spieler das Pack annehmen müssen
 * @param packPrompt        Hinweistext im Pack-Dialog (MiniMessage)
 * @param header            Kopfzeile mit geladenem Pack (MiniMessage, {@code <bild>} = Grafik)
 * @param headerWithoutPack Kopfzeile ohne Pack (MiniMessage)
 * @param footer            Fußzeile (MiniMessage)
 * @param blankLines        Leerzeilen nach der Grafik, oder {@link TablistHeader#AUTO}
 */
record TablistSettings(
        String packUrl,
        String packSha1,
        boolean packRequired,
        String packPrompt,
        String header,
        String headerWithoutPack,
        String footer,
        int blankLines) {

    private static final Pattern SHA1 = Pattern.compile("[0-9a-f]{40}");

    TablistSettings {
        packUrl = packUrl.strip();
        packSha1 = packSha1.strip().toLowerCase(Locale.ROOT);
        if (!packSha1.isEmpty() && !SHA1.matcher(packSha1).matches()) {
            throw new IllegalArgumentException(
                    "Resource-Pack.SHA1 muss leer sein oder aus 40 Hex-Zeichen bestehen: " + packSha1);
        }
        if (blankLines < 0 && blankLines != TablistHeader.AUTO) {
            throw new IllegalArgumentException("Bild.Leerzeilen muss 'auto' oder eine Zahl >= 0 sein: " + blankLines);
        }
    }

    /** Ob der Hash vorgegeben ist und nicht berechnet werden muss. */
    boolean hasSha1() {
        return !packSha1.isEmpty();
    }
}
