package io.github.moooritzn.plugins.dimensions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.World.Environment;
import org.junit.jupiter.api.Test;

class DimensionSettingsTest {

    @Test
    void overworldAndCustomWorldsAreAlwaysEnabled() {
        DimensionSettings settings = new DimensionSettings(false, false);
        assertTrue(settings.isEnabled(Environment.NORMAL));
        assertTrue(settings.isEnabled(Environment.CUSTOM));
        assertFalse(settings.isEnabled(Environment.NETHER));
        assertFalse(settings.isEnabled(Environment.THE_END));
    }

    @Test
    void togglesAreIndependent() {
        DimensionSettings netherOnly = new DimensionSettings(true, false);
        assertTrue(netherOnly.isEnabled(Environment.NETHER));
        assertFalse(netherOnly.isEnabled(Environment.THE_END));

        DimensionSettings endOnly = new DimensionSettings(false, true);
        assertFalse(endOnly.isEnabled(Environment.NETHER));
        assertTrue(endOnly.isEnabled(Environment.THE_END));
    }

    @Test
    void blocksEnteringDisabledDimension() {
        DimensionSettings settings = new DimensionSettings(false, true);
        assertTrue(settings.blocksEntry(Environment.NORMAL, Environment.NETHER));
        assertFalse(settings.blocksEntry(Environment.NORMAL, Environment.THE_END));
    }

    @Test
    void neverBlocksLeavingOrMovingWithinDisabledDimension() {
        DimensionSettings settings = new DimensionSettings(false, false);
        assertFalse(settings.blocksEntry(Environment.NETHER, Environment.NORMAL));
        assertFalse(settings.blocksEntry(Environment.THE_END, Environment.NORMAL));
        // z.B. End-Gateway, während ein Admin per /tp im deaktivierten End ist
        assertFalse(settings.blocksEntry(Environment.THE_END, Environment.THE_END));
    }

    @Test
    void allEnabledBlocksNothing() {
        DimensionSettings settings = new DimensionSettings(true, true);
        for (Environment from : Environment.values()) {
            for (Environment to : Environment.values()) {
                assertFalse(settings.blocksEntry(from, to), from + " -> " + to);
            }
        }
    }
}
