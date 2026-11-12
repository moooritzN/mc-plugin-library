package io.github.moooritzn.plugins.dimensions;

import io.github.moooritzn.mclib.plugin.BasePlugin;
import io.github.moooritzn.mclib.text.Text;
import io.github.moooritzn.mclib.util.Cooldowns;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jspecify.annotations.Nullable;

/**
 * Erlaubt es, den Nether und/oder das End über die config.yml oder per Befehl
 * ({@code /dimensions activate|deactivate <nether|end>}) zu deaktivieren.
 *
 * <p>Deaktivierte Dimensionen können nicht mehr über Portale betreten werden, und Spieler, die sich
 * darin befinden, werden in eine aktive Welt gebracht. Teleports per Befehl oder durch andere Plugins
 * bleiben erlaubt, damit Admins die Dimensionen weiterhin besuchen können.
 */
public final class DimensionsPlugin extends BasePlugin {

    /** Berechtigung für {@code /dimensions} (in der plugin.yml standardmäßig nur für OPs). */
    static final String ADMIN_PERMISSION = "dimensions.admin";

    /** Mindestabstand zwischen zwei Hinweisen an denselben Spieler (z.B. solange er im Portal steht). */
    private static final Duration MESSAGE_COOLDOWN = Duration.ofSeconds(3);

    private final Cooldowns<UUID> messageCooldowns = new Cooldowns<>();
    private DimensionSettings settings = new DimensionSettings(true, true);
    private Messages messages = new Messages("", "", "");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        registerListeners(new PortalListener(this), new PlayerListener(this));
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(DimensionsCommand.create(this),
                        "Nether und End aktivieren/deaktivieren, Status anzeigen oder Konfiguration neu laden",
                        List.of()));

        // Spieler, die schon vor dem Aktivieren online waren (z.B. nach /reload)
        moveAllOutOfDisabledDimensions();

        getSLF4JLogger().info("Dimensions aktiviert (Nether: {}, End: {})",
                stateName(settings.netherEnabled()), stateName(settings.endEnabled()));
    }

    private static String stateName(boolean enabled) {
        return enabled ? "aktiviert" : "deaktiviert";
    }

    DimensionSettings settings() {
        return settings;
    }

    /**
     * Lädt die config.yml neu, übernimmt die Einstellungen sofort und bringt Spieler aus nun
     * deaktivierten Dimensionen heraus.
     *
     * @return Anzahl der Spieler, die aus einer deaktivierten Dimension gebracht werden
     */
    int reload() {
        reloadConfig();
        loadSettings();
        getSLF4JLogger().info("Konfiguration neu geladen (Nether: {}, End: {})",
                stateName(settings.netherEnabled()), stateName(settings.endEnabled()));
        return moveAllOutOfDisabledDimensions();
    }

    /**
     * Aktiviert oder deaktiviert eine Dimension zur Laufzeit (per Befehl), speichert die Änderung in der
     * config.yml und bringt Spieler aus einer nun deaktivierten Dimension heraus.
     *
     * @param dimension {@link Environment#NETHER} oder {@link Environment#THE_END}
     * @return Anzahl der Spieler, die aus der Dimension gebracht werden
     */
    int setDimensionEnabled(Environment dimension, boolean enabled) {
        // Erst neu einlesen, damit saveConfig() keine noch nicht geladenen Änderungen an der Datei überschreibt
        reloadConfig();
        loadSettings();
        settings = settings.with(dimension, enabled);
        getConfig().set(DimensionSettings.configKey(dimension), enabled);
        saveConfig();
        getSLF4JLogger().info("{} per Befehl {}", DimensionSettings.displayName(dimension), stateName(enabled));
        return moveAllOutOfDisabledDimensions();
    }

    private void loadSettings() {
        FileConfiguration config = getConfig();
        // Fehlende Werte kommen aus der config.yml im JAR (Defaults von JavaPlugin#reloadConfig)
        settings = new DimensionSettings(
                readToggle(config, DimensionSettings.configKey(Environment.NETHER)),
                readToggle(config, DimensionSettings.configKey(Environment.THE_END)));
        messages = new Messages(
                readMessage(config, "Nachrichten.Portal-gesperrt"),
                readMessage(config, "Nachrichten.Portal-entzuenden-gesperrt"),
                readMessage(config, "Nachrichten.Herausteleportiert"));
    }

    private boolean readToggle(FileConfiguration config, String key) {
        if (!config.isBoolean(key)) {
            getSLF4JLogger().warn("'{}' in der config.yml ist kein gültiger Wahrheitswert (true/false), "
                    + "die Dimension bleibt aktiviert", key);
            return true;
        }
        return config.getBoolean(key);
    }

    private static String readMessage(FileConfiguration config, String path) {
        return Objects.requireNonNullElse(config.getString(path), "");
    }

    /** Hinweis in der Actionbar, dass ein Portal in eine deaktivierte Dimension führt. */
    void notifyPortalBlocked(Player player, Environment target) {
        sendThrottled(player, messages.portalBlocked(), target);
    }

    /** Hinweis in der Actionbar, dass keine Netherportale entzündet werden können. */
    void notifyPortalCreateBlocked(Player player) {
        sendThrottled(player, messages.portalCreateBlocked(), Environment.NETHER);
    }

    /** Sendet einen Actionbar-Hinweis höchstens einmal pro {@link #MESSAGE_COOLDOWN}. */
    private void sendThrottled(Player player, String message, Environment dimension) {
        if (message.isBlank() || !messageCooldowns.tryStart(player.getUniqueId(), MESSAGE_COOLDOWN)) {
            return;
        }
        player.sendActionBar(Text.mm(message, dimensionPlaceholder(dimension)));
    }

    /** Hinweis im Chat, dass der Spieler aus einer deaktivierten Dimension gebracht wurde. */
    void notifyMovedOut(Player player, Environment from) {
        if (!messages.movedOut().isBlank()) {
            player.sendMessage(Text.mm(messages.movedOut(), dimensionPlaceholder(from)));
        }
    }

    private static TagResolver dimensionPlaceholder(Environment dimension) {
        return Placeholder.unparsed("dimension", DimensionSettings.displayName(dimension));
    }

    /** Vergisst den Nachrichten-Cooldown eines Spielers (beim Verlassen des Servers). */
    void forgetPlayer(Player player) {
        messageCooldowns.reset(player.getUniqueId());
    }

    private int moveAllOutOfDisabledDimensions() {
        int moved = 0;
        for (Player player : getServer().getOnlinePlayers()) {
            if (moveOutIfDisabled(player)) {
                moved++;
            }
        }
        return moved;
    }

    /**
     * Bringt den Spieler in eine aktive Welt, falls er sich in einer deaktivierten Dimension befindet.
     * Ziel ist sein Respawn-Punkt, sofern dieser in einer aktiven Welt liegt, sonst {@link #mainSpawn()}.
     *
     * @return {@code true}, wenn ein Teleport gestartet wurde
     */
    boolean moveOutIfDisabled(Player player) {
        Environment current = player.getWorld().getEnvironment();
        if (settings.isEnabled(current)) {
            return false;
        }
        Location target = safeLocation(player);
        if (target == null) {
            getSLF4JLogger().warn("Keine aktive Welt gefunden, um {} aus der Dimension {} zu bringen",
                    player.getName(), DimensionSettings.displayName(current));
            return false;
        }
        // Spieler mit Passagieren lassen sich laut Paper-Javadoc nicht über Dimensionsgrenzen teleportieren
        // (teleportAsync liefert dann false) – die Passagiere bleiben deshalb zurück
        if (!player.getPassengers().isEmpty()) {
            player.eject();
        }
        // Ursache PLUGIN: wird vom PortalListener bewusst nicht blockiert
        player.teleportAsync(target, TeleportCause.PLUGIN).thenAccept(success -> {
            if (success) {
                notifyMovedOut(player, current);
            } else {
                getSLF4JLogger().warn("{} konnte nicht aus der Dimension {} teleportiert werden",
                        player.getName(), DimensionSettings.displayName(current));
            }
        });
        return true;
    }

    /** Respawn-Punkt des Spielers, falls er in einer aktiven Welt liegt, sonst {@link #mainSpawn()}. */
    private @Nullable Location safeLocation(Player player) {
        // Erst ohne Chunk-Laden prüfen, damit Respawn-Punkte in deaktivierten Welten gar nicht erst geladen werden
        Location potential = player.getRespawnLocation(false);
        if (potential != null && isEnabled(potential)) {
            Location validated = player.getRespawnLocation(true);
            if (validated != null && isEnabled(validated)) {
                return validated;
            }
        }
        return mainSpawn();
    }

    /**
     * Spawn der Hauptwelt (die erste geladene Welt, normalerweise die Oberwelt). Ist diese selbst
     * deaktiviert, wird die erste aktive Welt verwendet.
     *
     * @return {@code null}, wenn keine aktive Welt geladen ist
     */
    @Nullable Location mainSpawn() {
        for (World world : getServer().getWorlds()) {
            if (settings.isEnabled(world.getEnvironment())) {
                return world.getSpawnLocation();
            }
        }
        return null;
    }

    /** Ob die Welt des Ortes geladen und aktiv ist. */
    boolean isEnabled(Location location) {
        Environment environment = environmentOf(location);
        return environment != null && settings.isEnabled(environment);
    }

    /** Welttyp eines Ortes, oder {@code null}, wenn keine (geladene) Welt gesetzt ist. */
    static @Nullable Environment environmentOf(@Nullable Location location) {
        if (location == null || !location.isWorldLoaded()) {
            return null;
        }
        return location.getWorld().getEnvironment();
    }
}
