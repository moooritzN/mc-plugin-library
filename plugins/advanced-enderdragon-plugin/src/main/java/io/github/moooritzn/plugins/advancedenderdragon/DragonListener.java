package io.github.moooritzn.plugins.advancedenderdragon;

import com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent;
import org.bukkit.Location;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/** Verstärkt den Drachenatem und lässt beim Einschlag eines Drachen-Feuerballs einen Blitz einschlagen. */
final class DragonListener implements Listener {

    private final AdvancedEnderDragonPlugin plugin;
    private final LightningStrikes lightning;

    DragonListener(AdvancedEnderDragonPlugin plugin, LightningStrikes lightning) {
        this.plugin = plugin;
        this.lightning = lightning;
    }

    /** Drachenatem (lila Wolken) macht mehr Schaden. Rückstoß verursacht Drachenatem ohnehin nicht. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    void onBreathDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof EnderDragon
                || event.getDamageSource().getDamageType() != DamageType.DRAGON_BREATH) {
            return;
        }
        event.setDamage(event.getDamage() * plugin.settings().breathDamageMultiplier());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onFireballHit(EnderDragonFireballHitEvent event) {
        if (!plugin.settings().lightningOnFireball()) {
            return;
        }
        AreaEffectCloud cloud = event.getAreaEffectCloud();
        Location impact = cloud != null ? cloud.getLocation() : event.getEntity().getLocation();
        lightning.strike(impact);
    }
}
