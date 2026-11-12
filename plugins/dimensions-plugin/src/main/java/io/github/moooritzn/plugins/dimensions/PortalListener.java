package io.github.moooritzn.plugins.dimensions;

import static io.github.moooritzn.plugins.dimensions.DimensionsPlugin.environmentOf;

import io.papermc.paper.event.entity.EntityPortalReadyEvent;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.PortalCreateEvent;
import org.jspecify.annotations.Nullable;

/**
 * Verhindert Portalreisen in deaktivierte Dimensionen und das Entzünden von Netherportalen,
 * solange der Nether deaktiviert ist.
 *
 * <p>Geprüft wird an mehreren Stellen, weil Paper je nach Portal und Entity unterschiedliche Events
 * auslöst. Auch Enderperlen führen Spieler nicht in eine deaktivierte Dimension. Teleports per Befehl
 * oder Plugin ({@link TeleportCause#COMMAND}, {@link TeleportCause#PLUGIN}) werden nie blockiert.
 */
final class PortalListener implements Listener {

    private final DimensionsPlugin plugin;

    PortalListener(DimensionsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Netherportale: wird ausgelöst, sobald eine Entity (auch ein Spieler) lange genug im Portal stand.
     * Abbrechen setzt die Wartezeit zurück, so wird der Teleport nicht in jedem Tick erneut versucht.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    void onPortalReady(EntityPortalReadyEvent event) {
        World targetWorld = event.getTargetWorld();
        PortalKind kind = PortalKind.of(event.getPortalType());
        if (targetWorld == null || kind == null) {
            return; // Ohne Zielwelt findet ohnehin kein Teleport statt
        }
        Environment target = blockedTarget(kind, environmentOf(event.getEntity().getLocation()),
                targetWorld.getEnvironment());
        if (target != null) {
            event.setCancelled(true);
            if (event.getEntity() instanceof Player player) {
                plugin.notifyPortalBlocked(player, target);
            }
        }
    }

    /** Portalreise eines Spielers (Netherportal, Endportal und, je nach Version, End-Gateway). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    void onPlayerPortal(PlayerPortalEvent event) {
        handlePlayerTeleport(event);
    }

    /**
     * Sonstige Teleports eines Spielers. {@link PlayerPortalEvent} hat eine eigene Handler-Liste und
     * landet nicht hier; dieser Handler fängt Portal-Teleports ab, die nur als normales
     * {@link PlayerTeleportEvent} gemeldet werden (z.B. End-Gateways), sowie Dimensionswechsel ohne Portal,
     * etwa durch eine Enderperle, die in einer deaktivierten Dimension landet.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    void onPlayerTeleport(PlayerTeleportEvent event) {
        handlePlayerTeleport(event);
    }

    private void handlePlayerTeleport(PlayerTeleportEvent event) {
        TeleportCause cause = event.getCause();
        if (cause == TeleportCause.COMMAND || cause == TeleportCause.PLUGIN) {
            return; // Befehle und Plugins – Admins dürfen weiterhin hinein
        }
        Environment from = environmentOf(event.getFrom());
        if (from == null) {
            return;
        }
        Environment resolved = environmentOf(event.getTo());
        PortalKind kind = PortalKind.of(cause);
        // Portale: Die Portalart hilft, falls die Zielwelt noch nicht feststeht. Alle anderen Ursachen
        // (z.B. ENDER_PEARL – Enderperlen teleportieren auch über Dimensionsgrenzen) werden anhand der
        // tatsächlichen Zielwelt geprüft.
        Environment target = kind != null ? kind.target(from, resolved) : resolved;
        if (target != null && plugin.settings().blocksEntry(from, target)) {
            event.setCancelled(true);
            plugin.notifyPortalBlocked(event.getPlayer(), target);
        }
    }

    /** Portalreise anderer Entities (Items, Mobs, Loren, ...). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    void onEntityPortal(EntityPortalEvent event) {
        Environment from = environmentOf(event.getFrom());
        Environment resolved = environmentOf(event.getTo());
        PortalKind kind = PortalKind.of(event.getPortalType());
        if (from == null) {
            return;
        }
        // Plugin-Portale (PortalType.CUSTOM) nur anhand der tatsächlichen Zielwelt prüfen
        Environment target = kind != null ? kind.target(from, resolved) : resolved;
        if (target != null && plugin.settings().blocksEntry(from, target)) {
            event.setCancelled(true);
        }
    }

    /** Neue Netherportale (Rahmen entzündet) sind bei deaktiviertem Nether nicht erlaubt. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason() != PortalCreateEvent.CreateReason.FIRE || plugin.settings().netherEnabled()) {
            return;
        }
        event.setCancelled(true);
        if (event.getEntity() instanceof Player player) {
            plugin.notifyPortalCreateBlocked(player);
        }
    }

    /**
     * Zieldimension, falls der Portalwechsel blockiert werden muss.
     *
     * @param from           Dimension, in der sich die Entity befindet, oder {@code null}, wenn unbekannt
     * @param resolvedTarget Typ der bereits bekannten Zielwelt, oder {@code null}
     * @return die deaktivierte Zieldimension, oder {@code null}, wenn der Wechsel erlaubt ist
     */
    private @Nullable Environment blockedTarget(PortalKind kind, @Nullable Environment from,
                                                @Nullable Environment resolvedTarget) {
        if (from == null) {
            return null;
        }
        Environment target = kind.target(from, resolvedTarget);
        return plugin.settings().blocksEntry(from, target) ? target : null;
    }
}
