package io.github.moooritzn.plugins.example;

import io.github.moooritzn.mclib.text.Text;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

final class JoinListener implements Listener {

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(Text.mm(
                "<gray>Willkommen, <gold><name></gold>!",
                Placeholder.unparsed("name", event.getPlayer().getName())));
    }
}
