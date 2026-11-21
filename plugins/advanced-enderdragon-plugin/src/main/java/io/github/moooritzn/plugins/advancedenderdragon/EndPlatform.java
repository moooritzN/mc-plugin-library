package io.github.moooritzn.plugins.advancedenderdragon;

import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

/**
 * Die Obsidian-Plattform, auf der Spieler beim Betreten des Ends ankommen.
 *
 * <p>Die API kennt diese Position nicht direkt. Sie wird deshalb in dieser Reihenfolge bestimmt:
 * <ol>
 *   <li>gelernt: die exakte Ankunftsposition des letzten Spielers, der per End-Portal kam
 *       (dauerhaft in den Weltdaten gespeichert),</li>
 *   <li>gesucht: Obsidianboden an der Vanilla-Plattformstelle (x=100, z=0),</li>
 *   <li>Notfall: Vanilla-Position; fehlt dort der Boden, wird wie in Vanilla eine 5×5-Obsidianplattform gelegt,
 *       damit niemand ins Void teleportiert wird.</li>
 * </ol>
 */
final class EndPlatform {

    /** Vanilla-Ankunftsposition im End (Blockmitte), Blickrichtung Westen zur Insel. */
    private static final double DEFAULT_X = 100.5;
    private static final double DEFAULT_Y = 50;
    private static final double DEFAULT_Z = 0.5;
    private static final float DEFAULT_YAW = 90f;
    /** Suchbereich (y) für den Obsidianboden der Plattform. */
    private static final int SEARCH_TOP = 100;
    private static final int SEARCH_BOTTOM = 20;

    private final NamespacedKey key;

    EndPlatform(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "plattform");
    }

    /** Merkt sich die Ankunftsposition eines Spielers, der per End-Portal ins End kam. */
    void learn(Location arrival) {
        World world = arrival.getWorld();
        if (world == null || world.getEnvironment() != World.Environment.THE_END) {
            return;
        }
        world.getPersistentDataContainer().set(key, PersistentDataType.STRING, String.format(Locale.ROOT,
                "%.3f;%.3f;%.3f;%.1f", arrival.getX(), arrival.getY(), arrival.getZ(), arrival.getYaw()));
    }

    /** Sichere Zielposition auf der Plattform in dieser End-Welt. */
    Location target(World world) {
        Location location = learned(world);
        if (location == null) {
            location = search(world);
        }
        if (location == null) {
            location = new Location(world, DEFAULT_X, DEFAULT_Y, DEFAULT_Z, DEFAULT_YAW, 0);
        }
        ensureFloor(location);
        return location;
    }

    private @Nullable Location learned(World world) {
        String stored = world.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (stored == null) {
            return null;
        }
        String[] parts = stored.split(";");
        if (parts.length != 4) {
            return null;
        }
        try {
            return new Location(world, Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]), Float.parseFloat(parts[3]), 0);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Sucht an der Vanilla-Plattformstelle einen Obsidianboden mit zwei freien Blöcken darüber. */
    private static @Nullable Location search(World world) {
        int x = (int) Math.floor(DEFAULT_X);
        int z = (int) Math.floor(DEFAULT_Z);
        for (int y = Math.min(SEARCH_TOP, world.getMaxHeight() - 3); y >= Math.max(SEARCH_BOTTOM, world.getMinHeight()); y--) {
            if (world.getBlockAt(x, y, z).getType() == Material.OBSIDIAN
                    && world.getBlockAt(x, y + 1, z).isPassable()
                    && world.getBlockAt(x, y + 2, z).isPassable()) {
                return new Location(world, DEFAULT_X, y + 1, DEFAULT_Z, DEFAULT_YAW, 0);
            }
        }
        return null;
    }

    /** Legt wie Vanilla eine 5×5-Obsidianplattform, falls unter dem Ziel kein fester Boden ist. */
    private static void ensureFloor(Location target) {
        Block floor = target.getBlock().getRelative(0, -1, 0);
        if (floor.getType().isSolid()) {
            return;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                floor.getRelative(dx, 0, dz).setType(Material.OBSIDIAN);
            }
        }
    }
}
