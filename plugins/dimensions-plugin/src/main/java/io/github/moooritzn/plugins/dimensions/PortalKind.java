package io.github.moooritzn.plugins.dimensions;

import org.bukkit.PortalType;
import org.bukkit.World.Environment;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jspecify.annotations.Nullable;

/** Die Portalarten, über die eine Dimension betreten werden kann. */
enum PortalKind {
    NETHER,
    END,
    END_GATEWAY;

    /**
     * Portalart zu einer Teleport-Ursache eines Spielers.
     *
     * @return {@code null}, wenn der Teleport nicht durch ein Portal ausgelöst wurde
     *         (z.B. {@link TeleportCause#COMMAND} oder {@link TeleportCause#PLUGIN})
     */
    static @Nullable PortalKind of(TeleportCause cause) {
        return switch (cause) {
            case NETHER_PORTAL -> NETHER;
            case END_PORTAL -> END;
            case END_GATEWAY -> END_GATEWAY;
            default -> null;
        };
    }

    /**
     * Portalart zu einem {@link PortalType} (bei Entity-Events).
     *
     * @return {@code null} für Plugin-Portale ({@link PortalType#CUSTOM})
     */
    static @Nullable PortalKind of(PortalType type) {
        return switch (type) {
            case NETHER -> NETHER;
            case ENDER -> END;
            case END_GATEWAY -> END_GATEWAY;
            default -> null;
        };
    }

    /**
     * Bestimmt die Zieldimension eines Portals.
     *
     * <p>Ist die Zielwelt bereits bekannt, wird deren Typ verwendet (sie kann z.B. von anderen Plugins
     * geändert worden sein). Andernfalls wird das Vanilla-Verhalten angenommen: Nether- und Endportale
     * führen aus ihrer Dimension zurück in die Oberwelt und sonst in ihre Dimension, End-Gateways
     * bleiben in derselben Dimension.
     *
     * @param from           Dimension, in der das Portal steht
     * @param resolvedTarget Typ der tatsächlichen Zielwelt, falls bekannt
     */
    Environment target(Environment from, @Nullable Environment resolvedTarget) {
        if (resolvedTarget != null) {
            return resolvedTarget;
        }
        return switch (this) {
            case NETHER -> from == Environment.NETHER ? Environment.NORMAL : Environment.NETHER;
            case END -> from == Environment.THE_END ? Environment.NORMAL : Environment.THE_END;
            case END_GATEWAY -> from;
        };
    }
}
