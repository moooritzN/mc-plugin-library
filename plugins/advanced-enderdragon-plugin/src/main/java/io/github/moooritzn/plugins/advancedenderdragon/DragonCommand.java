package io.github.moooritzn.plugins.advancedenderdragon;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.moooritzn.mclib.text.Text;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.World;
import org.bukkit.boss.DragonBattle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderDragon;

/**
 * Befehl {@code /enderdrache} (zeigt den Zustand aller Drachenkämpfe) und {@code /enderdrache reload}.
 * Beide erfordern {@value AdvancedEnderDragonPlugin#ADMIN_PERMISSION}.
 */
final class DragonCommand {

    private DragonCommand() {
    }

    static LiteralCommandNode<CommandSourceStack> create(AdvancedEnderDragonPlugin plugin) {
        return Commands.literal("enderdrache")
                .requires(source -> source.getSender().hasPermission(AdvancedEnderDragonPlugin.ADMIN_PERMISSION))
                .executes(context -> {
                    sendStatus(plugin, context.getSource().getSender());
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("reload")
                        .executes(context -> {
                            CommandSender sender = context.getSource().getSender();
                            if (plugin.reload()) {
                                sender.sendMessage(Text.mm("<green>Konfiguration von AdvancedEnderDragon neu geladen."));
                            } else {
                                sender.sendMessage(Text.mm("<red>Die config.yml enthält Fehler (siehe Server-Log). "
                                        + "Die bisherige Konfiguration bleibt aktiv."));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }

    private static void sendStatus(AdvancedEnderDragonPlugin plugin, CommandSender sender) {
        boolean anyFight = false;
        for (World world : plugin.getServer().getWorlds()) {
            DragonBattle battle = world.getEnvironment() == World.Environment.THE_END
                    ? world.getEnderDragonBattle() : null;
            EnderDragon dragon = battle == null ? null : battle.getEnderDragon();
            if (dragon == null || !dragon.isValid()) {
                continue;
            }
            anyFight = true;
            double max = AdvancedEnderDragonPlugin.maxHealth(dragon);
            sender.sendMessage(Text.mm("<dark_purple><welt>: <light_purple><leben> / <max> Leben <gray>(<prozent> %)",
                    Placeholder.unparsed("welt", world.getName()),
                    Placeholder.unparsed("leben", Long.toString(Math.round(dragon.getHealth()))),
                    Placeholder.unparsed("max", Long.toString(Math.round(max))),
                    Placeholder.unparsed("prozent", Long.toString(Math.round(dragon.getHealth() / max * 100)))));
        }
        if (!anyFight) {
            sender.sendMessage(Text.mm("<gray>Zurzeit läuft kein Drachenkampf."));
        }
    }
}
