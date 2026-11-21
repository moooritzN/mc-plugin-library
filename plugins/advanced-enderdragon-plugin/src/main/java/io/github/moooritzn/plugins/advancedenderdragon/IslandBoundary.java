package io.github.moooritzn.plugins.advancedenderdragon;

import io.github.moooritzn.mclib.text.Text;
import io.github.moooritzn.mclib.util.Cooldowns;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;

/**
 * Hält Spieler während des Drachenkampfes auf der Hauptinsel: Ab dem Warn-Radius gibt es eine Warnung,
 * ab dem Teleport-Radius geht es zurück zur Obsidian-Plattform. Abstände zählen nur horizontal (x/z).
 *
 * <p>Geprüft wird bei jeder Bewegung über eine Blockgrenze und zusätzlich einmal pro Sekunde
 * ({@link #check}), damit auch Spieler erfasst werden, die z.B. per Enderperle hinausgelangt sind und
 * stillstehen. Ausgenommen sind nur Spieler im Kreativ-/Zuschauermodus – bewusst keine Bypass-Berechtigung,
 * da Admins mit {@code *}-Rechten sonst unbemerkt ausgenommen wären.
 */
final class IslandBoundary implements Listener {

    private static final Duration ACTION_BAR_INTERVAL = Duration.ofSeconds(1);
    private static final Title.Times TITLE_TIMES =
            Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(500));

    private final AdvancedEnderDragonPlugin plugin;
    private final EndPlatform platform;
    private final Cooldowns<UUID> actionBarCooldown = new Cooldowns<>();
    /** Spieler, die sich gerade in der Warnzone befinden (Titel nur beim Betreten zeigen). */
    private final Set<UUID> warned = new HashSet<>();
    /** Spieler, deren Rückteleport gerade läuft (verhindert doppelte Teleports). */
    private final Set<UUID> teleporting = new HashSet<>();

    IslandBoundary(AdvancedEnderDragonPlugin plugin, EndPlatform platform) {
        this.plugin = plugin;
        this.platform = platform;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()
                && from.getWorld() == to.getWorld()) {
            return; // nur bei Wechsel der Blockspalte prüfen – die Höhe spielt keine Rolle
        }
        check(event.getPlayer(), to);
    }

    /** Ankunftsposition merken, wenn jemand per End-Portal ins End kommt (= Obsidian-Plattform). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onPortal(PlayerPortalEvent event) {
        learnArrival(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onTeleport(PlayerTeleportEvent event) {
        learnArrival(event);
    }

    private void learnArrival(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (event.getCause() == TeleportCause.END_PORTAL && to.getWorld() != null
                && to.getWorld().getEnvironment() == World.Environment.THE_END) {
            platform.learn(to);
        }
    }

    @EventHandler
    void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        warned.remove(id);
        teleporting.remove(id);
        actionBarCooldown.reset(id);
    }

    /** Prüft die Position eines Spielers und warnt bzw. teleportiert ihn bei Bedarf. */
    void check(Player player, Location location) {
        UUID id = player.getUniqueId();
        BoundarySettings settings = plugin.boundary();
        World world = location.getWorld();
        if (!settings.enabled() || world == null || !plugin.isFightActive(world)
                || !LightningStrikes.isVulnerable(player)) {
            warned.remove(id);
            return;
        }
        switch (settings.zoneAt(location.getX(), location.getZ())) {
            case ISLAND -> warned.remove(id);
            case WARNING -> warn(player, settings, location);
            case TELEPORT -> {
                warned.remove(id);
                teleportBack(player, world, settings);
            }
        }
    }

    /** Beschreibt für {@code /enderdrache}, ob und warum die Grenze für den Spieler gerade (nicht) gilt. */
    String describe(Player player) {
        BoundarySettings settings = plugin.boundary();
        World world = player.getWorld();
        if (!settings.enabled()) {
            return "<red>aus</red> <gray>(Insel-Grenze.Aktiv: false)";
        }
        if (world.getEnvironment() != World.Environment.THE_END) {
            return "<gray>gilt nicht – du bist nicht im End";
        }
        if (!plugin.isFightActive(world)) {
            return "<gray>gilt nicht – kein lebender Drache in dieser Welt erkannt";
        }
        if (!LightningStrikes.isVulnerable(player)) {
            return "<gray>gilt nicht für dich – Spielmodus " + player.getGameMode().name();
        }
        Location location = player.getLocation();
        long distance = Math.round(BoundarySettings.horizontalDistance(location.getX(), location.getZ()));
        return "<green>aktiv</green> <gray>– dein Abstand: " + distance + " Blöcke (Warnung ab "
                + Math.round(settings.warnRadius()) + ", Teleport ab " + Math.round(settings.teleportRadius()) + ")";
    }

    private void warn(Player player, BoundarySettings settings, Location location) {
        if (warned.add(player.getUniqueId())
                && !(settings.warnTitle().isBlank() && settings.warnSubtitle().isBlank())) {
            player.showTitle(Title.title(mm(settings.warnTitle()), mm(settings.warnSubtitle()), TITLE_TIMES));
        }
        if (!settings.warnActionBar().isBlank()
                && actionBarCooldown.tryStart(player.getUniqueId(), ACTION_BAR_INTERVAL)) {
            double distance = BoundarySettings.horizontalDistance(location.getX(), location.getZ());
            long blocks = Math.max(1, (long) Math.ceil(settings.teleportRadius() - distance));
            player.sendActionBar(Text.mm(settings.warnActionBar(),
                    Placeholder.unparsed("bloecke", Long.toString(blocks))));
        }
    }

    private void teleportBack(Player player, World world, BoundarySettings settings) {
        if (!teleporting.add(player.getUniqueId())) {
            return;
        }
        Location target = platform.target(world);
        player.leaveVehicle();
        player.setGliding(false);
        player.setVelocity(new Vector());
        player.setFallDistance(0);
        // Ursache PLUGIN: wird vom Dimensions-Plugin bewusst nicht blockiert
        player.teleportAsync(target, TeleportCause.PLUGIN).whenComplete((success, error) -> {
            teleporting.remove(player.getUniqueId());
            if (Boolean.TRUE.equals(success) && !settings.teleportedMessage().isBlank()) {
                player.sendMessage(Text.mm(settings.teleportedMessage()));
            } else if (error != null || !Boolean.TRUE.equals(success)) {
                plugin.getSLF4JLogger().warn("{} konnte nicht zur End-Plattform zurückteleportiert werden",
                        player.getName());
            }
        });
    }

    private static Component mm(String miniMessage) {
        return miniMessage.isBlank() ? Component.empty() : Text.mm(miniMessage);
    }
}
