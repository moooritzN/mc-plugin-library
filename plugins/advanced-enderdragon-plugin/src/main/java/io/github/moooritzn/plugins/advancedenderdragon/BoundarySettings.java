package io.github.moooritzn.plugins.advancedenderdragon;

/**
 * Einstellungen der Insel-Grenze aus der config.yml ({@code Insel-Grenze}).
 *
 * <p>Alle Abstände sind horizontal (nur x und z) von der Mitte des Ends (0, 0) gemessen – die Höhe zählt nicht.
 *
 * @param enabled           ob die Grenze während des Drachenkampfes gilt
 * @param warnRadius        ab diesem Abstand wird gewarnt
 * @param teleportRadius    ab diesem Abstand wird zur Plattform teleportiert
 * @param warnTitle         Titel beim Betreten der Warnzone (MiniMessage)
 * @param warnSubtitle      Untertitel beim Betreten der Warnzone (MiniMessage)
 * @param warnActionBar     Actionbar-Hinweis in der Warnzone (MiniMessage, {@code <bloecke>} = Blöcke bis zur Teleportation)
 * @param teleportedMessage Chat-Nachricht nach dem Zurückteleportieren (MiniMessage)
 */
record BoundarySettings(
        boolean enabled,
        double warnRadius,
        double teleportRadius,
        String warnTitle,
        String warnSubtitle,
        String warnActionBar,
        String teleportedMessage) {

    BoundarySettings {
        if (warnRadius <= 0) {
            throw new IllegalArgumentException("Insel-Grenze.Warn-Radius muss größer als 0 sein");
        }
        if (teleportRadius <= warnRadius) {
            throw new IllegalArgumentException("Insel-Grenze.Teleport-Radius muss größer als der Warn-Radius sein");
        }
    }

    /** Horizontaler Abstand von der Mitte des Ends (0, 0); die Höhe (y) zählt nicht. */
    static double horizontalDistance(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    /** Zone eines Spielers anhand seines horizontalen Abstands. */
    Zone zoneAt(double x, double z) {
        double distance = horizontalDistance(x, z);
        if (distance >= teleportRadius) {
            return Zone.TELEPORT;
        }
        return distance >= warnRadius ? Zone.WARNING : Zone.ISLAND;
    }

    /** Bereiche rund um die Hauptinsel. */
    enum Zone {
        /** Innerhalb des Warn-Radius – alles in Ordnung. */
        ISLAND,
        /** Zwischen Warn- und Teleport-Radius – Warnung. */
        WARNING,
        /** Ab dem Teleport-Radius – zurück zur Plattform. */
        TELEPORT
    }
}
