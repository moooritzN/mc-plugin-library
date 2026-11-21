package io.github.moooritzn.plugins.advancedenderdragon;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jspecify.annotations.Nullable;

/**
 * Die zehn Obsidian-Säulen des Ends, auf denen die End-Kristalle stehen.
 *
 * <p>Die Säulen stehen im Kreis mit Radius 42 um (0, 0), ihre Höhe hängt vom Welt-Seed ab. Die Kristallposition
 * wird deshalb in der Welt gesucht: Oben auf jeder Säule liegt ein Grundgestein-Block, auf dem der Kristall steht.
 * Damit die Suche auch bei kleinen Rundungsabweichungen trifft, wird ein kleiner Bereich um den berechneten
 * Mittelpunkt abgesucht.
 */
final class EndPillars {

    /** Anzahl der Säulen. */
    static final int COUNT = 10;
    /** Abstand der Säulen-Mittelpunkte von (0, 0). */
    static final double RADIUS = 42.0;
    /** Suchbereich um den berechneten Mittelpunkt (in Blöcken, je Richtung). */
    private static final int SEARCH = 2;
    /** So viele Blöcke unter der höchsten Stelle wird nach dem Grundgestein gesucht. */
    private static final int DEPTH = 6;

    private EndPillars() {
    }

    /** Ein Block-Spalten-Mittelpunkt. */
    record Column(int x, int z) {
    }

    /** Berechnete Mittelpunkte der zehn Säulen (wie bei der Vanilla-Generierung). */
    static List<Column> centers() {
        List<Column> centers = new ArrayList<>(COUNT);
        for (int i = 0; i < COUNT; i++) {
            double angle = 2.0 * (-Math.PI + Math.PI / COUNT * i);
            centers.add(new Column(
                    (int) Math.floor(RADIUS * Math.cos(angle)),
                    (int) Math.floor(RADIUS * Math.sin(angle))));
        }
        return centers;
    }

    /**
     * Position für einen Kristall auf der Säule bei {@code center}: mittig über dem obersten Grundgestein-Block.
     *
     * @return {@code null}, wenn auf dieser Säule kein Grundgestein gefunden wurde (z.B. Säule zerstört)
     */
    static @Nullable Location crystalSpot(World world, Column center) {
        Location best = null;
        for (int dx = -SEARCH; dx <= SEARCH; dx++) {
            for (int dz = -SEARCH; dz <= SEARCH; dz++) {
                int x = center.x() + dx;
                int z = center.z() + dz;
                int top = world.getHighestBlockYAt(x, z);
                for (int y = top; y >= Math.max(world.getMinHeight(), top - DEPTH); y--) {
                    if (world.getBlockAt(x, y, z).getType() == Material.BEDROCK) {
                        if (best == null || y + 1 > best.getY()) {
                            best = new Location(world, x + 0.5, y + 1, z + 0.5);
                        }
                        break;
                    }
                }
            }
        }
        return best;
    }
}
