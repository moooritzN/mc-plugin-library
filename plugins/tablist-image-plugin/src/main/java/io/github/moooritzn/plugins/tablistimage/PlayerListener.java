package io.github.moooritzn.plugins.tablistimage;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

/** Verschickt das Pack beim Join und setzt die Tab-Liste passend zum Pack-Status des Clients. */
final class PlayerListener implements Listener {

    private final TablistImagePlugin plugin;

    PlayerListener(TablistImagePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        plugin.updateTablist(event.getPlayer());
        plugin.sendPack(event.getPlayer());
    }

    @EventHandler
    void onQuit(PlayerQuitEvent event) {
        plugin.forgetPlayer(event.getPlayer());
    }

    @EventHandler
    void onPackStatus(PlayerResourcePackStatusEvent event) {
        if (!plugin.isOwnPack(event.getID())) {
            return; // anderes Pack, z.B. aus der server.properties
        }
        switch (event.getStatus()) {
            case SUCCESSFULLY_LOADED -> plugin.setPackLoaded(event.getPlayer(), true);
            case DECLINED, DISCARDED -> plugin.setPackLoaded(event.getPlayer(), false);
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD -> {
                plugin.getSLF4JLogger().warn("Resource Pack konnte bei {} nicht geladen werden ({})",
                        event.getPlayer().getName(), event.getStatus());
                plugin.setPackLoaded(event.getPlayer(), false);
            }
            default -> {
                // ACCEPTED, DOWNLOADED: Zwischenschritte, Pack ist noch nicht aktiv
            }
        }
    }
}
