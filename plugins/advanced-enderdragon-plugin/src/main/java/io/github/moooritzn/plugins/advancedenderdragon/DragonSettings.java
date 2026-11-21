package io.github.moooritzn.plugins.advancedenderdragon;

/**
 * Einstellungen aus der config.yml.
 *
 * @param healthMultiplier         Faktor für die maximalen Lebenspunkte
 * @param breathDamageMultiplier   Faktor für den Schaden des Drachenatems
 * @param fightRadius              Radius um (0, 0), in dem Spieler als Kampfteilnehmer gelten
 * @param lightningEnabled         ob Blitzwellen aktiv sind
 * @param lightningIntervalSeconds Sekunden zwischen zwei Blitzwellen
 * @param lightningTargets         anvisierte Spieler pro Welle
 * @param lightningWarningTicks    Vorwarnzeit in Ticks
 * @param lightningDamage          Schaden pro Einschlag
 * @param lightningRadius          Wirkungsradius eines Einschlags
 * @param lightningOnFireball      ob Feuerball-Einschläge zusätzlich einen Blitz auslösen
 * @param crystalRestorePercent    Lebensstand in Prozent, bei dem die Kristalle zurückkehren (0 = nie)
 * @param crystalsRestoredMessage  Nachricht beim Wiederherstellen (MiniMessage, {@code <anzahl>})
 */
record DragonSettings(
        double healthMultiplier,
        double breathDamageMultiplier,
        double fightRadius,
        boolean lightningEnabled,
        int lightningIntervalSeconds,
        int lightningTargets,
        int lightningWarningTicks,
        double lightningDamage,
        double lightningRadius,
        boolean lightningOnFireball,
        double crystalRestorePercent,
        String crystalsRestoredMessage) {

    DragonSettings {
        require(healthMultiplier > 0, "Leben-Multiplikator muss größer als 0 sein");
        require(breathDamageMultiplier >= 0, "Drachenatem-Schaden-Multiplikator darf nicht negativ sein");
        require(fightRadius > 0, "Kampf-Radius muss größer als 0 sein");
        require(lightningIntervalSeconds >= 1, "Blitze.Intervall-Sekunden muss mindestens 1 sein");
        require(lightningTargets >= 1, "Blitze.Ziele-pro-Welle muss mindestens 1 sein");
        require(lightningWarningTicks >= 0, "Blitze.Vorwarnung-Ticks darf nicht negativ sein");
        require(lightningDamage >= 0, "Blitze.Schaden darf nicht negativ sein");
        require(lightningRadius > 0, "Blitze.Radius muss größer als 0 sein");
        require(crystalRestorePercent >= 0 && crystalRestorePercent < 100,
                "Kristalle.Wiederherstellen-bei-Prozent muss zwischen 0 und 99 liegen");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /** Ob die Kristall-Wiederherstellung aktiv ist. */
    boolean crystalRestoreEnabled() {
        return crystalRestorePercent > 0;
    }
}
