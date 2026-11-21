package io.github.moooritzn.plugins.tablistimage;

import io.github.moooritzn.mclib.plugin.BasePlugin;
import io.github.moooritzn.mclib.text.Text;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * Zeigt eine Grafik oben in der Tab-Spielerliste an.
 *
 * <p>Die Grafik steckt in einem Resource Pack (Schriftart mit einem Bild-Zeichen), das jeder Spieler beim
 * Betreten erhält. Erst wenn der Client meldet, dass das Pack geladen ist, wird die Kopfzeile mit der
 * Grafik gesetzt; vorher bzw. bei Ablehnung gilt {@code Tab-Liste.Header-ohne-Pack}.
 */
public final class TablistImagePlugin extends BasePlugin {

    /** Berechtigung für {@code /tablistimage reload}. */
    static final String ADMIN_PERMISSION = "tablistimage.admin";

    /** Spieler, deren Client das Pack erfolgreich geladen hat. */
    private final Set<UUID> packLoaded = ConcurrentHashMap.newKeySet();

    /** Die Grafik als Zeichenfolge (aus der beim Build generierten bild.properties). */
    private @Nullable Banner banner;
    private @Nullable TablistSettings settings;
    /** Aktuelles Pack, {@code null} solange es nicht bereit ist (Hash wird noch berechnet oder Fehler). */
    private @Nullable ResourcePackInfo pack;
    /** Zählt Reloads, damit verspätete Ergebnisse eines älteren Reloads verworfen werden. */
    private int generation;

    @Override
    public void onEnable() {
        try (InputStream in = getResource(Banner.RESOURCE)) {
            if (in == null) {
                throw new IOException(Banner.RESOURCE + " fehlt im Plugin-JAR");
            }
            banner = Banner.load(in);
        } catch (IOException e) {
            getSLF4JLogger().error("Grafik-Daten konnten nicht geladen werden – TablistImage wird deaktiviert", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        if (!reload()) {
            getSLF4JLogger().error("Ungültige config.yml – TablistImage wird deaktiviert");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        registerListeners(new PlayerListener(this));
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(TablistImageCommand.create(this),
                        "Lädt die Konfiguration von TablistImage neu", List.of()));
    }

    @Override
    public void onDisable() {
        // z.B. bei /reload: Grafik und Pack wieder entfernen, damit nichts Verwaistes zurückbleibt
        ResourcePackInfo current = pack;
        for (Player player : getServer().getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            if (current != null) {
                player.removeResourcePacks(current.id());
            }
        }
        packLoaded.clear();
    }

    /**
     * Lädt die config.yml neu, aktualisiert die Tab-Liste aller Spieler und bereitet das Pack vor
     * (bei leerem SHA1 asynchron, da es dafür heruntergeladen wird).
     *
     * @return {@code false}, wenn die Konfiguration ungültig ist (die bisherige bleibt dann aktiv)
     */
    boolean reload() {
        reloadConfig();
        try {
            settings = readSettings(getConfig());
        } catch (IllegalArgumentException e) {
            getSLF4JLogger().error("Fehler in der config.yml: {}", e.getMessage());
            return false;
        }
        getServer().getOnlinePlayers().forEach(this::updateTablist);
        preparePack();
        return true;
    }

    private static TablistSettings readSettings(FileConfiguration config) {
        // Fehlende Werte kommen aus der config.yml im JAR (Defaults von JavaPlugin#reloadConfig)
        return new TablistSettings(
                config.getString("Resource-Pack.URL", ""),
                config.getString("Resource-Pack.SHA1", ""),
                config.getBoolean("Resource-Pack.Pflicht"),
                config.getString("Resource-Pack.Hinweis", ""),
                config.getString("Tab-Liste.Header", ""),
                config.getString("Tab-Liste.Header-ohne-Pack", ""),
                config.getString("Tab-Liste.Footer", ""),
                readBlankLines(config));
    }

    private static int readBlankLines(FileConfiguration config) {
        String path = "Bild.Leerzeilen";
        if (config.isInt(path)) {
            return config.getInt(path);
        }
        String value = config.getString(path, "auto").strip();
        if (value.equalsIgnoreCase("auto")) {
            return TablistHeader.AUTO;
        }
        throw new IllegalArgumentException(path + " muss 'auto' oder eine Zahl sein: " + value);
    }

    private TablistSettings settings() {
        if (settings == null) {
            throw new IllegalStateException("Konfiguration noch nicht geladen");
        }
        return settings;
    }

    private void preparePack() {
        TablistSettings current = settings();
        int gen = ++generation;
        URI uri;
        try {
            uri = new URI(current.packUrl());
        } catch (URISyntaxException e) {
            getSLF4JLogger().error("Resource-Pack.URL ist keine gültige URL: {}", current.packUrl());
            return;
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            getSLF4JLogger().error("Resource-Pack.URL muss mit http:// oder https:// beginnen: {}", uri);
            return;
        }
        // Gleiche URL -> gleiche ID, so ersetzt ein neues Pack gleicher Herkunft das alte beim Client
        UUID id = UUID.nameUUIDFromBytes(("tablistimage:" + uri).getBytes(StandardCharsets.UTF_8));

        if (current.hasSha1()) {
            applyPack(ResourcePackInfo.resourcePackInfo(id, uri, current.packSha1()));
            return;
        }
        getSLF4JLogger().info("Lade Resource Pack von {}, um den SHA-1-Hash zu berechnen ...", uri);
        ResourcePackInfo.resourcePackInfo().id(id).uri(uri)
                .computeHashAndBuild(task -> getServer().getScheduler().runTaskAsynchronously(this, task))
                .whenComplete((info, error) -> {
                    if (!isEnabled()) {
                        return;
                    }
                    getServer().getScheduler().runTask(this, () -> {
                        if (gen != generation) {
                            return; // von einem neueren Reload überholt
                        }
                        if (error != null) {
                            getSLF4JLogger().error("Resource Pack konnte nicht von {} geladen werden: {}. "
                                    + "Die Grafik wird nicht angezeigt, bis /tablistimage reload erfolgreich ist.",
                                    uri, rootMessage(error));
                            return;
                        }
                        getSLF4JLogger().info("Resource Pack bereit (SHA-1 {})", info.hash());
                        applyPack(info);
                    });
                });
    }

    private static String rootMessage(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + (root.getMessage() == null ? "" : ": " + root.getMessage());
    }

    /** Übernimmt ein (neues) Pack und schickt es allen Spielern; unverändertes Pack wird nicht erneut gesendet. */
    private void applyPack(ResourcePackInfo next) {
        ResourcePackInfo previous = pack;
        if (previous != null && previous.id().equals(next.id()) && previous.hash().equals(next.hash())
                && previous.uri().equals(next.uri())) {
            return;
        }
        pack = next;
        packLoaded.clear();
        for (Player player : getServer().getOnlinePlayers()) {
            if (previous != null && !previous.id().equals(next.id())) {
                player.removeResourcePacks(previous.id());
            }
            updateTablist(player);
            sendPack(player);
        }
    }

    /** Schickt dem Spieler das Pack, sofern es bereit ist. */
    void sendPack(Player player) {
        ResourcePackInfo current = pack;
        if (current == null) {
            return;
        }
        TablistSettings config = settings();
        ResourcePackRequest.Builder request = ResourcePackRequest.resourcePackRequest()
                .packs(current)
                .required(config.packRequired())
                .replace(false); // andere Server-Packs (z.B. aus server.properties) behalten
        if (!config.packPrompt().isEmpty()) {
            request.prompt(Text.mm(config.packPrompt()));
        }
        player.sendResourcePacks(request.build());
    }

    /** Ob die ID zum aktuellen Pack dieses Plugins gehört. */
    boolean isOwnPack(UUID packId) {
        ResourcePackInfo current = pack;
        return current != null && current.id().equals(packId);
    }

    /** Merkt sich, ob der Client des Spielers das Pack geladen hat, und aktualisiert seine Tab-Liste. */
    void setPackLoaded(Player player, boolean loaded) {
        if (loaded) {
            packLoaded.add(player.getUniqueId());
        } else {
            packLoaded.remove(player.getUniqueId());
        }
        updateTablist(player);
    }

    /** Vergisst den Pack-Status eines Spielers (beim Verlassen des Servers). */
    void forgetPlayer(Player player) {
        packLoaded.remove(player.getUniqueId());
    }

    /** Setzt Kopf- und Fußzeile – mit Grafik nur, wenn der Client das Pack geladen hat. */
    void updateTablist(Player player) {
        TablistSettings config = settings();
        Banner image = banner;
        if (image == null) {
            throw new IllegalStateException("Grafik-Daten noch nicht geladen");
        }
        String header = packLoaded.contains(player.getUniqueId()) ? config.header() : config.headerWithoutPack();
        player.sendPlayerListHeaderAndFooter(
                TablistHeader.render(header, image, config.blankLines()),
                TablistHeader.render(config.footer(), image, config.blankLines()));
    }
}
