package io.github.moooritzn.plugins.advancedenderdragon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * Läuft einmal pro Sekunde und kümmert sich in jeder End-Welt mit laufendem Drachenkampf um
 * Lebenspunkte, Kristall-Wiederherstellung, Blitzwellen und die Insel-Grenze.
 */
final class DragonFightTask implements Runnable {

    /** Ausführungsintervall in Ticks. */
    static final long PERIOD_TICKS = 20;

    private final AdvancedEnderDragonPlugin plugin;
    private final LightningStrikes lightning;
    private final IslandBoundary islandBoundary;
    /** Sekunden bis zur nächsten Blitzwelle, je Welt. */
    private final Map<UUID, Integer> lightningCountdown = new HashMap<>();

    DragonFightTask(AdvancedEnderDragonPlugin plugin, LightningStrikes lightning, IslandBoundary islandBoundary) {
        this.plugin = plugin;
        this.lightning = lightning;
        this.islandBoundary = islandBoundary;
    }

    @Override
    public void run() {
        for (World world : plugin.getServer().getWorlds()) {
            if (world.getEnvironment() != World.Environment.THE_END) {
                continue;
            }
            EnderDragon dragon = findDragon(world);
            boolean active = dragon != null && dragon.getPhase() != EnderDragon.Phase.DYING;
            plugin.setFightActive(world, active);
            if (!active) {
                lightningCountdown.remove(world.getUID());
                continue;
            }
            // Zuerst die Insel-Grenze, damit sie auch greift, falls ein späterer Schritt fehlschlägt.
            // Auch stillstehende Spieler prüfen (z.B. per Enderperle über die Grenze gelangt).
            for (Player player : world.getPlayers()) {
                islandBoundary.check(player, player.getLocation());
            }
            plugin.applyHealth(dragon);
            plugin.restoreCrystalsIfDue(dragon);
            tickLightning(world);
        }
    }

    /**
     * Der lebende Drache dieser End-Welt: bevorzugt aus dem Drachenkampf, sonst (z.B. falls der Kampf-Status
     * den Drachen nicht kennt) direkt unter den geladenen Entities gesucht.
     */
    static @Nullable EnderDragon findDragon(World world) {
        DragonBattle battle = world.getEnderDragonBattle();
        EnderDragon dragon = battle == null ? null : battle.getEnderDragon();
        if (dragon != null && dragon.isValid()) {
            return dragon;
        }
        for (EnderDragon candidate : world.getEntitiesByClass(EnderDragon.class)) {
            if (candidate.isValid()) {
                return candidate;
            }
        }
        return null;
    }

    private void tickLightning(World world) {
        DragonSettings settings = plugin.settings();
        if (!settings.lightningEnabled()) {
            lightningCountdown.remove(world.getUID());
            return;
        }
        int remaining = lightningCountdown.getOrDefault(world.getUID(), settings.lightningIntervalSeconds()) - 1;
        if (remaining > 0) {
            lightningCountdown.put(world.getUID(), remaining);
            return;
        }
        lightningCountdown.put(world.getUID(), settings.lightningIntervalSeconds());

        List<Player> targets = new ArrayList<>(fighters(world, settings.fightRadius()));
        Collections.shuffle(targets);
        for (Player target : targets.subList(0, Math.min(settings.lightningTargets(), targets.size()))) {
            lightning.strikeWithWarning(target.getLocation());
        }
    }

    /** Angreifbare Spieler im Kampfgebiet um (0, 0). */
    static List<Player> fighters(World world, double radius) {
        Location center = new Location(world, 0, 0, 0);
        List<Player> result = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            Location location = player.getLocation();
            double dx = location.getX() - center.getX();
            double dz = location.getZ() - center.getZ();
            if (dx * dx + dz * dz <= radius * radius && LightningStrikes.isVulnerable(player)) {
                result.add(player);
            }
        }
        return result;
    }
}
