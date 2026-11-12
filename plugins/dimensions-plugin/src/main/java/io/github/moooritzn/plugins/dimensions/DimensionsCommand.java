package io.github.moooritzn.plugins.dimensions;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.moooritzn.mclib.text.Text;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

/**
 * Befehl {@code /dimensions} (zeigt den Status) und {@code /dimensions reload} (lädt die config.yml neu).
 * Beide erfordern die Berechtigung {@value DimensionsPlugin#ADMIN_PERMISSION}.
 */
final class DimensionsCommand {

    private DimensionsCommand() {
    }

    static LiteralCommandNode<CommandSourceStack> create(DimensionsPlugin plugin) {
        return Commands.literal("dimensions")
                .requires(source -> source.getSender().hasPermission(DimensionsPlugin.ADMIN_PERMISSION))
                .executes(context -> {
                    sendStatus(plugin, context.getSource().getSender());
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("reload")
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            int moved = plugin.reload();
                            sender.sendMessage(Text.mm("<green>Konfiguration von Dimensions neu geladen."));
                            sendStatus(plugin, sender);
                            if (moved > 0) {
                                sender.sendMessage(Text.mm(
                                        "<gray><count> Spieler werden aus deaktivierten Dimensionen gebracht.",
                                        Placeholder.unparsed("count", Integer.toString(moved))));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    private static void sendStatus(DimensionsPlugin plugin, CommandSender sender) {
        DimensionSettings settings = plugin.settings();
        sender.sendMessage(Text.mm("<gray>Nether: <nether><gray>, End: <end>",
                Placeholder.parsed("nether", state(settings.netherEnabled())),
                Placeholder.parsed("end", state(settings.endEnabled()))));
    }

    private static String state(boolean enabled) {
        return enabled ? "<green>aktiviert</green>" : "<red>deaktiviert</red>";
    }
}
