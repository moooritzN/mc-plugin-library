package io.github.moooritzn.plugins.deathcounter;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Sorgt dafür, dass neu beitretende Spieler in der Tab-Liste mit 0 Toden statt leer angezeigt werden. */
final class JoinListener implements Listener {

    private final DeathcounterPlugin plugin;

    JoinListener(DeathcounterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        plugin.initScore(event.getPlayer());
    }
}
