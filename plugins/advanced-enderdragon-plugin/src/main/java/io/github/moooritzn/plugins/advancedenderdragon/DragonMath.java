package io.github.moooritzn.plugins.advancedenderdragon;

/** Bukkit-unabhängige Berechnungen rund um den Drachen. */
final class DragonMath {

    private DragonMath() {
    }

    /**
     * Neue Lebenspunkte beim Ändern der maximalen Lebenspunkte, sodass der prozentuale Lebensstand erhalten
     * bleibt (ein voller Drache bleibt voll, ein halb verletzter bleibt halb verletzt).
     */
    static double scaledHealth(double health, double oldMax, double newMax) {
        if (oldMax <= 0) {
            return newMax;
        }
        double scaled = health / oldMax * newMax;
        return Math.max(0, Math.min(newMax, scaled));
    }

    /** Ob der Lebensstand auf oder unter {@code percent} Prozent gefallen ist. */
    static boolean atOrBelowPercent(double health, double maxHealth, double percent) {
        return maxHealth > 0 && health / maxHealth * 100 <= percent;
    }

    /** Ob zwei Werte praktisch gleich sind (für Attributwerte als double). */
    static boolean nearlyEqual(double a, double b) {
        return Math.abs(a - b) < 1e-6;
    }
}
