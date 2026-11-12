package io.github.moooritzn.mclib.plugin;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Optionale Basisklasse für Plugins mit häufig benötigten Hilfsmethoden.
 */
public abstract class BasePlugin extends JavaPlugin {

    /** Registriert mehrere Listener auf einmal. */
    protected final void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }
}
