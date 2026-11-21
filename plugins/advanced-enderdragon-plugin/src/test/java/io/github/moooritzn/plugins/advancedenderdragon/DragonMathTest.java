package io.github.moooritzn.plugins.advancedenderdragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DragonMathTest {

    @Test
    void fullDragonStaysFullWhenScaled() {
        assertEquals(800, DragonMath.scaledHealth(200, 200, 800), 1e-9);
    }

    @Test
    void injuredDragonKeepsPercentage() {
        assertEquals(400, DragonMath.scaledHealth(100, 200, 800), 1e-9);
        // zurück auf Vanilla
        assertEquals(50, DragonMath.scaledHealth(200, 800, 200), 1e-9);
    }

    @Test
    void scaledHealthIsClamped() {
        assertEquals(800, DragonMath.scaledHealth(250, 200, 800), 1e-9);
        assertEquals(0, DragonMath.scaledHealth(-5, 200, 800), 1e-9);
        assertEquals(800, DragonMath.scaledHealth(10, 0, 800), 1e-9);
    }

    @Test
    void percentThreshold() {
        assertFalse(DragonMath.atOrBelowPercent(401, 800, 50));
        assertTrue(DragonMath.atOrBelowPercent(400, 800, 50));
        assertTrue(DragonMath.atOrBelowPercent(0, 800, 50));
        assertFalse(DragonMath.atOrBelowPercent(0, 0, 50));
    }
}
