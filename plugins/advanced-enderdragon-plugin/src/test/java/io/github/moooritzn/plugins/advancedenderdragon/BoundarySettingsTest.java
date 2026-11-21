package io.github.moooritzn.plugins.advancedenderdragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.moooritzn.plugins.advancedenderdragon.BoundarySettings.Zone;
import org.junit.jupiter.api.Test;

class BoundarySettingsTest {

    private static final BoundarySettings SETTINGS = new BoundarySettings(true, 120, 150, "", "", "", "");

    @Test
    void zonesByHorizontalDistance() {
        assertEquals(Zone.ISLAND, SETTINGS.zoneAt(0, 0));
        // Obsidian-Plattform (x=100) liegt noch auf der Insel
        assertEquals(Zone.ISLAND, SETTINGS.zoneAt(100.5, 0.5));
        assertEquals(Zone.WARNING, SETTINGS.zoneAt(120, 0));
        assertEquals(Zone.WARNING, SETTINGS.zoneAt(-100, -100)); // ~141 Blöcke
        assertEquals(Zone.TELEPORT, SETTINGS.zoneAt(0, -150));
        assertEquals(Zone.TELEPORT, SETTINGS.zoneAt(500, 500));
    }

    @Test
    void distanceIgnoresHeight() {
        // Es gibt bewusst keinen y-Parameter: 3-4-5-Dreieck in x/z
        assertEquals(5.0, BoundarySettings.horizontalDistance(3, 4), 1e-9);
        assertEquals(100.0, BoundarySettings.horizontalDistance(-60, 80), 1e-9);
    }

    @Test
    void teleportRadiusMustBeBeyondWarnRadius() {
        assertThrows(IllegalArgumentException.class, () -> new BoundarySettings(true, 150, 150, "", "", "", ""));
        assertThrows(IllegalArgumentException.class, () -> new BoundarySettings(true, 0, 150, "", "", "", ""));
    }
}
