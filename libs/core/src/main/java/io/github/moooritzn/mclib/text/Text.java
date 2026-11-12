package io.github.moooritzn.mclib.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Hilfsmethoden für Texte im MiniMessage-Format.
 *
 * <pre>{@code
 * player.sendMessage(Text.mm("<green>Hallo <name>!", Placeholder.unparsed("name", player.getName())));
 * }</pre>
 */
public final class Text {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Text() {
    }

    /** Parst einen MiniMessage-String zu einer {@link Component}. */
    public static Component mm(String input, TagResolver... resolvers) {
        return MINI_MESSAGE.deserialize(input, resolvers);
    }

    /** Entfernt alle MiniMessage-Tags aus dem String. */
    public static String strip(String input) {
        return MINI_MESSAGE.stripTags(input);
    }
}
