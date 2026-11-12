package io.github.moooritzn.plugins.example;

import io.github.moooritzn.mclib.text.Text;
import io.github.moooritzn.mclib.util.Cooldowns;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.time.Duration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Entity;

final class HelloCommand implements BasicCommand {

    private final Cooldowns<Object> cooldowns = new Cooldowns<>();

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        Object key = source.getExecutor() instanceof Entity entity ? entity.getUniqueId() : "console";
        if (!cooldowns.tryStart(key, Duration.ofSeconds(5))) {
            long seconds = cooldowns.remaining(key).toSeconds() + 1;
            source.getSender().sendMessage(Text.mm("<red>Bitte warte noch <s>s.",
                    Placeholder.unparsed("s", String.valueOf(seconds))));
            return;
        }
        source.getSender().sendMessage(Text.mm("<green>Hallo von mclib!"));
    }
}
