package io.github.moooritzn.plugins.advancedenderdragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class EndPillarsTest {

    @Test
    void tenDistinctPillarsOnCircleAroundOrigin() {
        List<EndPillars.Column> centers = EndPillars.centers();
        assertEquals(EndPillars.COUNT, centers.size());
        assertEquals(EndPillars.COUNT, new HashSet<>(centers).size());
        for (EndPillars.Column column : centers) {
            double distance = Math.hypot(column.x(), column.z());
            assertTrue(distance > EndPillars.RADIUS - 2 && distance < EndPillars.RADIUS + 2,
                    column + " liegt nicht auf dem Säulenkreis: " + distance);
        }
    }

    @Test
    void pillarsAreEvenlySpaced() {
        List<EndPillars.Column> centers = EndPillars.centers();
        for (int i = 0; i < centers.size(); i++) {
            EndPillars.Column a = centers.get(i);
            EndPillars.Column b = centers.get((i + 1) % centers.size());
            double gap = Math.hypot(a.x() - b.x(), a.z() - b.z());
            // Sehne bei 36° auf Radius 42 ≈ 25,96 Blöcke
            assertEquals(25.96, gap, 2.0);
        }
    }
}
