package io.github.moooritzn.plugins.dimensions;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.moooritzn.mclib.text.Text;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.World.Environment;
import org.bukkit.command.CommandSender;

/**
 * Befehl {@code /dimensions} mit den Unterbefehlen:
 * <ul>
 *   <li>{@code /dimensions} – zeigt den Status an</li>
 *   <li>{@code /dimensions activate <nether|end>} – aktiviert eine Dimension</li>
 *   <li>{@code /dimensions deactivate <nether|end>} – deaktiviert eine Dimension</li>
 *   <li>{@code /dimensions reload} – lädt die config.yml neu</li>
 * </ul>
 * Alle erfordern die Berechtigung {@value DimensionsPlugin#ADMIN_PERMISSION}. Änderungen per
 * activate/deactivate werden in der config.yml gespeichert und gelten damit auch nach einem Neustart.
 */
final class DimensionsCommand {

    /** Umschaltbare Dimensionen; im Befehl heißen sie wie in der config.yml, nur kleingeschrieben. */
    private static final List<Environment> DIMENSIONS = List.of(Environment.NETHER, Environment.THE_END);

    private DimensionsCommand() {
    }

    static LiteralCommandNode<CommandSourceStack> create(DimensionsPlugin plugin) {
        return Commands.literal("dimensions")
                .requires(source -> source.getSender().hasPermission(DimensionsPlugin.ADMIN_PERMISSION))
                .executes(context -> {
                    sendStatus(plugin, context.getSource().getSender());
                    return Command.SINGLE_SUCCESS;
                })
                .then(toggle(plugin, "activate", true))
                .then(toggle(plugin, "deactivate", false))
                .then(Commands.literal("reload")
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            int moved = plugin.reload();
                            sender.sendMessage(Text.mm("<green>Konfiguration von Dimensions neu geladen."));
                            sendStatus(plugin, sender);
                            sendMoved(sender, moved);
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    /** {@code activate}/{@code deactivate} mit je einem Unterbefehl pro Dimension (mit Tab-Vervollständigung). */
    private static LiteralArgumentBuilder<CommandSourceStack> toggle(DimensionsPlugin plugin, String name,
                                                                     boolean enabled) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(name);
        DIMENSIONS.forEach(dimension -> node.then(Commands.literal(
                        DimensionSettings.configKey(dimension).toLowerCase(Locale.ROOT))
                .executes(context -> {
                    CommandSender sender = context.getSource().getSender();
                    String displayName = DimensionSettings.displayName(dimension);
                    if (plugin.settings().isEnabled(dimension) == enabled) {
                        sender.sendMessage(Text.mm("<yellow>Die Dimension <gold><dimension></gold> ist bereits "
                                        + state(enabled) + "<yellow>.",
                                Placeholder.unparsed("dimension", displayName)));
                        return Command.SINGLE_SUCCESS;
                    }
                    int moved = plugin.setDimensionEnabled(dimension, enabled);
                    sender.sendMessage(Text.mm("<green>Die Dimension <gold><dimension></gold> wurde "
                                    + state(enabled) + "<green>.",
                            Placeholder.unparsed("dimension", displayName)));
                    sendMoved(sender, moved);
                    return Command.SINGLE_SUCCESS;
                })));
        return node;
    }

    private static void sendStatus(DimensionsPlugin plugin, CommandSender sender) {
        DimensionSettings settings = plugin.settings();
        sender.sendMessage(Text.mm("<gray>Nether: <nether><gray>, End: <end>",
                Placeholder.parsed("nether", state(settings.netherEnabled())),
                Placeholder.parsed("end", state(settings.endEnabled()))));
    }

    private static void sendMoved(CommandSender sender, int moved) {
        if (moved > 0) {
            sender.sendMessage(Text.mm(
                    "<gray><count> Spieler werden aus deaktivierten Dimensionen gebracht.",
                    Placeholder.unparsed("count", Integer.toString(moved))));
        }
    }

    private static String state(boolean enabled) {
        return enabled ? "<green>aktiviert</green>" : "<red>deaktiviert</red>";
    }
}
