package io.github.moooritzn.plugins.dimensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.bukkit.PortalType;
import org.bukkit.World.Environment;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.junit.jupiter.api.Test;

class PortalKindTest {

    @Test
    void mapsPortalTeleportCauses() {
        assertEquals(PortalKind.NETHER, PortalKind.of(TeleportCause.NETHER_PORTAL));
        assertEquals(PortalKind.END, PortalKind.of(TeleportCause.END_PORTAL));
        assertEquals(PortalKind.END_GATEWAY, PortalKind.of(TeleportCause.END_GATEWAY));
    }

    @Test
    void commandAndPluginTeleportsAreNoPortals() {
        assertNull(PortalKind.of(TeleportCause.COMMAND));
        assertNull(PortalKind.of(TeleportCause.PLUGIN));
        assertNull(PortalKind.of(TeleportCause.ENDER_PEARL));
    }

    @Test
    void mapsPortalTypes() {
        assertEquals(PortalKind.NETHER, PortalKind.of(PortalType.NETHER));
        assertEquals(PortalKind.END, PortalKind.of(PortalType.ENDER));
        assertEquals(PortalKind.END_GATEWAY, PortalKind.of(PortalType.END_GATEWAY));
        assertNull(PortalKind.of(PortalType.CUSTOM));
    }

    @Test
    void resolvedTargetWins() {
        assertEquals(Environment.THE_END, PortalKind.NETHER.target(Environment.NORMAL, Environment.THE_END));
    }

    @Test
    void netherPortalFallback() {
        assertEquals(Environment.NETHER, PortalKind.NETHER.target(Environment.NORMAL, null));
        assertEquals(Environment.NORMAL, PortalKind.NETHER.target(Environment.NETHER, null));
        assertEquals(Environment.NETHER, PortalKind.NETHER.target(Environment.THE_END, null));
    }

    @Test
    void endPortalFallback() {
        assertEquals(Environment.THE_END, PortalKind.END.target(Environment.NORMAL, null));
        assertEquals(Environment.THE_END, PortalKind.END.target(Environment.NETHER, null));
        assertEquals(Environment.NORMAL, PortalKind.END.target(Environment.THE_END, null));
    }

    @Test
    void endGatewayStaysInDimension() {
        assertEquals(Environment.THE_END, PortalKind.END_GATEWAY.target(Environment.THE_END, null));
    }
}
