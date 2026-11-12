package io.github.moooritzn.plugins.dimensions;

import org.bukkit.World.Environment;

/**
 * Welche Dimensionen laut Konfiguration aktiv sind.
 *
 * <p>Die Oberwelt ({@link Environment#NORMAL}) und benutzerdefinierte Welten ({@link Environment#CUSTOM})
 * sind immer aktiv – abschalten lassen sich nur Nether und End. Maßgeblich ist der Welttyp, nicht der
 * Weltname: Zusätzliche Nether- oder End-Welten (z.B. von Multiworld-Plugins) zählen mit.
 *
 * @param netherEnabled ob der Nether betreten werden darf ({@code Nether} in der config.yml)
 * @param endEnabled    ob das End betreten werden darf ({@code End} in der config.yml)
 */
record DimensionSettings(boolean netherEnabled, boolean endEnabled) {

    /** Ob Welten dieses Typs aktiv sind. */
    boolean isEnabled(Environment environment) {
        return switch (environment) {
            case NETHER -> netherEnabled;
            case THE_END -> endEnabled;
            default -> true;
        };
    }

    /**
     * Ob ein Wechsel von {@code from} nach {@code target} verhindert werden muss.
     *
     * <p>Das ist nur der Fall, wenn die Zieldimension deaktiviert ist und tatsächlich gewechselt wird.
     * Wer sich bereits in der Dimension befindet (z.B. ein Admin per {@code /tp}), kann sich dort
     * weiter bewegen, etwa durch End-Gateways.
     */
    boolean blocksEntry(Environment from, Environment target) {
        return from != target && !isEnabled(target);
    }

    /** Anzeigename einer Dimension für Spielernachrichten. */
    static String displayName(Environment environment) {
        return switch (environment) {
            case NORMAL -> "Oberwelt";
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> environment.name();
        };
    }
}
