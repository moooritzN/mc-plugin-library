package io.github.moooritzn.plugins.tablistimage;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.moooritzn.mclib.text.Text;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

/** Befehl {@code /tablistimage reload} (Berechtigung {@value TablistImagePlugin#ADMIN_PERMISSION}). */
final class TablistImageCommand {

    private TablistImageCommand() {
    }

    static LiteralCommandNode<CommandSourceStack> create(TablistImagePlugin plugin) {
        return Commands.literal("tablistimage")
                .requires(source -> source.getSender().hasPermission(TablistImagePlugin.ADMIN_PERMISSION))
                .then(Commands.literal("reload")
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            if (plugin.reload()) {
                                sender.sendMessage(Text.mm("<green>Konfiguration von TablistImage neu geladen. "
                                        + "<gray>Ein geändertes Pack wird allen Spielern erneut geschickt."));
                            } else {
                                sender.sendMessage(Text.mm("<red>Die config.yml enthält Fehler (siehe Server-Log). "
                                        + "Die bisherige Konfiguration bleibt aktiv."));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }
}
