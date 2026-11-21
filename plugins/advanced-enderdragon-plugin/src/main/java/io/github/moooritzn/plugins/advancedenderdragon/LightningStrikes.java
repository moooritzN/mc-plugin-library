package io.github.moooritzn.plugins.advancedenderdragon;

import io.papermc.paper.event.entity.EntityKnockbackEvent;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jspecify.annotations.Nullable;

/**
 * Blitzeinschläge des Drachen.
 *
 * <p>Es wird nur der Blitz-Effekt erzeugt ({@link World#strikeLightningEffect}); den Schaden berechnet das Plugin
 * selbst. So zünden Blitze nichts an, treffen weder Kristalle noch den Drachen und verursachen keinen Rückstoß:
 * Die {@link DamageSource} hat bewusst weder verursachende Entity noch Ort, und den verbleibenden kleinen
 * Standard-Rückstoß unterdrückt {@link #onKnockback}.
 */
final class LightningStrikes implements Listener {

    /** Abstand zwischen zwei Partikel-Ringen während der Vorwarnung. */
    private static final int WARNING_INTERVAL_TICKS = 5;
    private static final int RING_POINTS = 16;

    private final AdvancedEnderDragonPlugin plugin;
    private final DamageSource damageSource = DamageSource.builder(DamageType.LIGHTNING_BOLT).build();
    /** Spieler, der gerade vom Blitz getroffen wird (für {@link #onKnockback}). */
    private @Nullable UUID striking;

    LightningStrikes(AdvancedEnderDragonPlugin plugin) {
        this.plugin = plugin;
    }

    /** Ob ein Spieler von Drachen-Angriffen getroffen werden kann (nicht im Kreativ-/Zuschauermodus). */
    static boolean isVulnerable(Player player) {
        GameMode mode = player.getGameMode();
        return !player.isDead() && (mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE);
    }

    /**
     * Kündigt einen Blitz an {@code target} mit knisternden Partikeln an und lässt ihn nach der Vorwarnzeit
     * einschlagen. Der Zielort steht ab der Ankündigung fest – wer wegläuft, entkommt.
     */
    void strikeWithWarning(Location target) {
        DragonSettings settings = plugin.settings();
        Location spot = target.clone();
        int warningTicks = settings.lightningWarningTicks();
        spot.getWorld().playSound(spot, Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 2.0f);
        for (int tick = 0; tick < warningTicks; tick += WARNING_INTERVAL_TICKS) {
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> warningRing(spot, settings.lightningRadius()), tick);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> strike(spot), warningTicks);
    }

    private static void warningRing(Location center, double radius) {
        World world = center.getWorld();
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = 2 * Math.PI * i / RING_POINTS;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius);
            world.spawnParticle(Particle.ELECTRIC_SPARK, point, 2, 0.05, 0.1, 0.05, 0);
        }
        world.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 0.5, 0), 6, 0.3, 0.5, 0.3, 0);
    }

    /** Lässt sofort einen Blitz einschlagen und verletzt alle angreifbaren Spieler im Radius. */
    void strike(Location target) {
        if (!target.isWorldLoaded()) {
            return;
        }
        DragonSettings settings = plugin.settings();
        World world = target.getWorld();
        world.strikeLightningEffect(target);
        if (settings.lightningDamage() <= 0) {
            return;
        }
        for (Player player : world.getNearbyPlayers(target, settings.lightningRadius(),
                LightningStrikes::isVulnerable)) {
            striking = player.getUniqueId();
            try {
                player.damage(settings.lightningDamage(), damageSource);
            } finally {
                striking = null;
            }
        }
    }

    /**
     * Minecraft gibt auch bei Schaden ohne Angreifer einen kleinen Rückstoß (Hüpfer). Für Blitz-Treffer dieses
     * Plugins wird er unterdrückt – das Event feuert synchron innerhalb von {@code damage(...)}.
     */
    @EventHandler(ignoreCancelled = true)
    void onKnockback(EntityKnockbackEvent event) {
        if (event.getCause() == EntityKnockbackEvent.Cause.DAMAGE
                && event.getEntity().getUniqueId().equals(striking)) {
            event.setCancelled(true);
        }
    }
}
