package io.github.moooritzn.plugins.dimensions;

/**
 * Spielernachrichten aus der config.yml (Abschnitt {@code Nachrichten}) im MiniMessage-Format.
 * Leere Nachrichten werden nicht angezeigt.
 *
 * @param portalBlocked       Actionbar-Text, wenn ein Portal in eine deaktivierte Dimension benutzt wird
 * @param portalCreateBlocked Actionbar-Text, wenn ein Netherportal nicht entzündet werden darf
 * @param movedOut            Chat-Text, wenn ein Spieler aus einer deaktivierten Dimension gebracht wurde
 */
record Messages(String portalBlocked, String portalCreateBlocked, String movedOut) {
}
