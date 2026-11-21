package io.github.moooritzn.plugins.advancedenderdragon;

import io.github.moooritzn.mclib.plugin.BasePlugin;
import io.github.moooritzn.mclib.text.Text;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;

/**
 * Macht den Enderdrachen deutlich stärker:
 * <ul>
 *   <li>mehr Lebenspunkte (Standard: 4-fach),</li>
 *   <li>stärkerer Drachenatem und Blitzwellen auf die Spieler im Kampfgebiet,</li>
 *   <li>einmalige Wiederherstellung der End-Kristalle, wenn der Drache die Hälfte seines Lebens verloren hat,</li>
 *   <li>Spieler dürfen die Hauptinsel nicht verlassen, solange der Drache lebt (Warnung, dann Rückteleport).</li>
 * </ul>
 * Der Rückstoß des Drachen bleibt unverändert.
 */
public final class AdvancedEnderDragonPlugin extends BasePlugin {

    /** Berechtigung für {@code /enderdrache}. */
    static final String ADMIN_PERMISSION = "advancedenderdragon.admin";

    /** Markiert einen Drachen, für den die Kristalle bereits wiederhergestellt wurden (überdauert Neustarts). */
    private NamespacedKey crystalsRestoredKey;
    private @Nullable DragonSettings settings;
    private @Nullable BoundarySettings boundary;
    /** End-Welten, in denen gerade ein Drachenkampf läuft (aktualisiert vom {@link DragonFightTask}). */
    private final Set<UUID> activeFights = new HashSet<>();

    @Override
    public void onEnable() {
        crystalsRestoredKey = new NamespacedKey(this, "kristalle_wiederhergestellt");
        saveDefaultConfig();
        if (!reload()) {
            getSLF4JLogger().error("Ungültige config.yml – AdvancedEnderDragon wird deaktiviert");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        LightningStrikes lightning = new LightningStrikes(this);
        IslandBoundary islandBoundary = new IslandBoundary(this, new EndPlatform(this));
        registerListeners(new DragonListener(this, lightning), lightning, islandBoundary);
        getServer().getScheduler().runTaskTimer(this, new DragonFightTask(this, lightning, islandBoundary),
                DragonFightTask.PERIOD_TICKS, DragonFightTask.PERIOD_TICKS);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(DragonCommand.create(this),
                        "Status des Enderdrachen anzeigen oder Konfiguration neu laden", List.of()));
    }

    /**
     * Lädt die config.yml neu. Neue Werte (z.B. Leben-Multiplikator) wirken spätestens nach einer Sekunde.
     *
     * @return {@code false}, wenn die Konfiguration ungültig ist (die bisherige bleibt dann aktiv)
     */
    boolean reload() {
        reloadConfig();
        try {
            DragonSettings newSettings = readSettings(getConfig());
            BoundarySettings newBoundary = readBoundary(getConfig());
            settings = newSettings;
            boundary = newBoundary;
            return true;
        } catch (IllegalArgumentException e) {
            getSLF4JLogger().error("Fehler in der config.yml: {}", e.getMessage());
            return false;
        }
    }

    private static DragonSettings readSettings(FileConfiguration config) {
        // Fehlende Werte kommen aus der config.yml im JAR (Defaults von JavaPlugin#reloadConfig)
        return new DragonSettings(
                config.getDouble("Leben-Multiplikator"),
                config.getDouble("Drachenatem-Schaden-Multiplikator"),
                config.getDouble("Kampf-Radius"),
                config.getBoolean("Blitze.Aktiv"),
                config.getInt("Blitze.Intervall-Sekunden"),
                config.getInt("Blitze.Ziele-pro-Welle"),
                config.getInt("Blitze.Vorwarnung-Ticks"),
                config.getDouble("Blitze.Schaden"),
                config.getDouble("Blitze.Radius"),
                config.getBoolean("Blitze.Bei-Feuerball-Einschlag"),
                config.getDouble("Kristalle.Wiederherstellen-bei-Prozent"),
                config.getString("Nachrichten.Kristalle-wiederhergestellt", ""));
    }

    private static BoundarySettings readBoundary(FileConfiguration config) {
        return new BoundarySettings(
                config.getBoolean("Insel-Grenze.Aktiv"),
                config.getDouble("Insel-Grenze.Warn-Radius"),
                config.getDouble("Insel-Grenze.Teleport-Radius"),
                config.getString("Insel-Grenze.Nachrichten.Warnung-Titel", ""),
                config.getString("Insel-Grenze.Nachrichten.Warnung-Untertitel", ""),
                config.getString("Insel-Grenze.Nachrichten.Warnung-Actionbar", ""),
                config.getString("Insel-Grenze.Nachrichten.Teleportiert", ""));
    }

    DragonSettings settings() {
        if (settings == null) {
            throw new IllegalStateException("Konfiguration noch nicht geladen");
        }
        return settings;
    }

    BoundarySettings boundary() {
        if (boundary == null) {
            throw new IllegalStateException("Konfiguration noch nicht geladen");
        }
        return boundary;
    }

    /** Ob in dieser Welt gerade ein Drachenkampf läuft (Drache lebt und stirbt nicht gerade). */
    boolean isFightActive(World world) {
        return activeFights.contains(world.getUID());
    }

    void setFightActive(World world, boolean active) {
        if (active) {
            activeFights.add(world.getUID());
        } else {
            activeFights.remove(world.getUID());
        }
    }

    /**
     * Setzt die maximalen Lebenspunkte auf Standardwert × Multiplikator, falls sie abweichen. Der prozentuale
     * Lebensstand bleibt erhalten – ein frisch gespawnter Drache ist also danach wieder voll.
     */
    void applyHealth(EnderDragon dragon) {
        AttributeInstance maxHealth = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        // Standardwert des Drachen (200) – AttributeInstance#getDefaultValue liefert den serverweiten Wert (20)
        AttributeInstance vanilla = EntityType.ENDER_DRAGON.getDefaultAttributes().getAttribute(Attribute.MAX_HEALTH);
        if (vanilla == null) {
            return;
        }
        double target = vanilla.getBaseValue() * settings().healthMultiplier();
        if (DragonMath.nearlyEqual(maxHealth.getBaseValue(), target)) {
            return;
        }
        double oldMax = maxHealth.getValue();
        double health = dragon.getHealth();
        maxHealth.setBaseValue(target);
        dragon.setHealth(DragonMath.scaledHealth(health, oldMax, maxHealth.getValue()));
        getSLF4JLogger().info("Enderdrache in '{}' verstärkt: {} Lebenspunkte (vorher {})",
                dragon.getWorld().getName(), Math.round(maxHealth.getValue()), Math.round(oldMax));
    }

    /** Aktuelle maximale Lebenspunkte des Drachen (inkl. Modifikatoren). */
    static double maxHealth(EnderDragon dragon) {
        AttributeInstance maxHealth = dragon.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? 0 : maxHealth.getValue();
    }

    /**
     * Stellt einmalig pro Drache die End-Kristalle auf allen Säulen ohne Kristall wieder her, sobald sein
     * Lebensstand auf den konfigurierten Prozentwert gefallen ist.
     */
    void restoreCrystalsIfDue(EnderDragon dragon) {
        DragonSettings config = settings();
        if (!config.crystalRestoreEnabled()
                || dragon.getPersistentDataContainer().has(crystalsRestoredKey, PersistentDataType.BOOLEAN)
                || !DragonMath.atOrBelowPercent(dragon.getHealth(), maxHealth(dragon),
                        config.crystalRestorePercent())) {
            return;
        }
        dragon.getPersistentDataContainer().set(crystalsRestoredKey, PersistentDataType.BOOLEAN, true);
        int restored = restoreCrystals(dragon.getWorld());
        getSLF4JLogger().info("Enderdrache in '{}' bei {} % Leben: {} Kristalle wiederhergestellt",
                dragon.getWorld().getName(), Math.round(dragon.getHealth() / maxHealth(dragon) * 100), restored);

        World world = dragon.getWorld();
        world.playSound(dragon.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 20f, 0.8f);
        if (!config.crystalsRestoredMessage().isBlank()) {
            var message = Text.mm(config.crystalsRestoredMessage(),
                    Placeholder.unparsed("anzahl", Integer.toString(restored)));
            world.getPlayers().forEach(player -> player.sendMessage(message));
        }
    }

    /**
     * Setzt auf jede Säule, auf der kein Kristall mehr steht, einen neuen – mit Blitz-Effekt.
     *
     * @return Anzahl der neu gesetzten Kristalle
     */
    private int restoreCrystals(World world) {
        int restored = 0;
        for (EndPillars.Column pillar : EndPillars.centers()) {
            Location spot = EndPillars.crystalSpot(world, pillar);
            if (spot == null) {
                getSLF4JLogger().warn("Keine Säulenspitze bei x={} z={} gefunden – kein Kristall gesetzt",
                        pillar.x(), pillar.z());
                continue;
            }
            if (!world.getNearbyEntitiesByType(EnderCrystal.class, spot, 1.5).isEmpty()) {
                continue; // Kristall steht noch
            }
            world.spawn(spot, EnderCrystal.class, crystal -> crystal.setShowingBottom(false));
            world.strikeLightningEffect(spot);
            restored++;
        }
        return restored;
    }
}
