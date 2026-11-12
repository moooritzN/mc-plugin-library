package io.github.moooritzn.plugins.dimensions;

import static io.github.moooritzn.plugins.dimensions.DimensionsPlugin.environmentOf;

import org.bukkit.Location;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/** Hält Spieler beim Betreten des Servers und beim Respawn aus deaktivierten Dimensionen heraus. */
final class PlayerListener implements Listener {

    private final DimensionsPlugin plugin;

    PlayerListener(DimensionsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Spieler, die sich zuletzt in einer inzwischen deaktivierten Dimension ausgeloggt haben. */
    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        plugin.moveOutIfDisabled(event.getPlayer());
    }

    /**
     * Ein Respawn-Punkt in einer deaktivierten Dimension (z.B. ein Seelenanker im Nether) wird auf den
     * Spawn der Hauptwelt umgeleitet.
     */
    @EventHandler(priority = EventPriority.HIGH)
    void onRespawn(PlayerRespawnEvent event) {
        Environment environment = environmentOf(event.getRespawnLocation());
        if (environment == null || plugin.settings().isEnabled(environment)) {
            return;
        }
        Location fallback = plugin.mainSpawn();
        if (fallback != null) {
            event.setRespawnLocation(fallback);
            plugin.notifyMovedOut(event.getPlayer(), environment);
        }
    }

    @EventHandler
    void onQuit(PlayerQuitEvent event) {
        plugin.forgetPlayer(event.getPlayer());
    }
}
