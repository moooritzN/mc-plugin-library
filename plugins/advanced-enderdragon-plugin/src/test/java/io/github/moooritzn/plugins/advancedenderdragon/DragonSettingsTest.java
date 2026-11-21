package io.github.moooritzn.plugins.advancedenderdragon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DragonSettingsTest {

    private static DragonSettings settings(double healthMultiplier, int interval, double crystalPercent) {
        return new DragonSettings(healthMultiplier, 2.0, 150, true, interval, 3, 30, 10.0, 3.0, true,
                crystalPercent, "");
    }

    @Test
    void defaultsAreValid() {
        DragonSettings settings = settings(4.0, 12, 50);
        assertTrue(settings.crystalRestoreEnabled());
    }

    @Test
    void zeroPercentDisablesCrystalRestore() {
        assertFalse(settings(4.0, 12, 0).crystalRestoreEnabled());
    }

    @Test
    void invalidValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> settings(0, 12, 50));
        assertThrows(IllegalArgumentException.class, () -> settings(4.0, 0, 50));
        assertThrows(IllegalArgumentException.class, () -> settings(4.0, 12, 100));
        assertThrows(IllegalArgumentException.class, () -> settings(4.0, 12, -1));
    }
}
