package io.github.moooritzn.plugins.deathcounter;

import io.github.moooritzn.mclib.plugin.BasePlugin;
import io.github.moooritzn.mclib.text.Text;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Zeigt die Anzahl der Tode jedes Spielers in der Tab-Spielerliste an.
 *
 * <p>Die Tode zählt der Server selbst über ein Scoreboard-Objective mit dem Kriterium
 * {@code deathCount} auf dem Haupt-Scoreboard. Dadurch bleiben die Werte über Neustarts
 * hinweg erhalten (sie werden mit den Scoreboard-Daten der Welt gespeichert).
 *
 * <p>Das Objective wird bewusst nicht zwischengespeichert, sondern bei Bedarf per Name
 * nachgeschlagen: Es kann zur Laufzeit z.B. mit {@code /scoreboard objectives remove}
 * entfernt werden, wodurch eine gespeicherte Referenz ungültig würde.
 */
public final class DeathcounterPlugin extends BasePlugin {

    /** Interner Name des Objectives (gleichzeitig nutzbar mit {@code /scoreboard}). */
    static final String OBJECTIVE_NAME = "deathcounter";

    @Override
    public void onEnable() {
        Objective objective = setUpObjective(mainScoreboard());

        // Spieler, die schon vor dem Aktivieren online waren (z.B. nach /reload)
        for (Player player : getServer().getOnlinePlayers()) {
            initScore(objective, player);
        }
        registerListeners(new JoinListener(this));

        getSLF4JLogger().info("Deathcounter aktiviert");
    }

    private Scoreboard mainScoreboard() {
        return getServer().getScoreboardManager().getMainScoreboard();
    }

    private static boolean hasDeathCountCriterion(Objective objective) {
        return Criteria.DEATH_COUNT.getName().equals(objective.getTrackedCriteria().getName());
    }

    /**
     * Holt das Objective vom Scoreboard oder legt es neu an und zeigt es in der Spielerliste an.
     * Ein bereits vorhandenes Objective (z.B. nach Neustart) wird mitsamt seinen Werten weiterverwendet.
     */
    private Objective setUpObjective(Scoreboard scoreboard) {
        Objective existing = scoreboard.getObjective(OBJECTIVE_NAME);
        if (existing != null && !hasDeathCountCriterion(existing)) {
            getSLF4JLogger().warn("Objective '{}' hat das falsche Kriterium '{}' und wird neu angelegt",
                    OBJECTIVE_NAME, existing.getTrackedCriteria().getName());
            existing.unregister();
            existing = null;
        }

        Objective result = existing != null
                ? existing
                : scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DEATH_COUNT,
                        Text.mm("<red>Tode"), RenderType.INTEGER);
        result.setRenderType(RenderType.INTEGER);
        result.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        return result;
    }

    /**
     * Liefert das aktuelle Objective. Wurde es zur Laufzeit entfernt oder durch eines mit
     * falschem Kriterium ersetzt, wird es neu angelegt und wieder in der Spielerliste angezeigt.
     */
    private Objective currentObjective() {
        Scoreboard scoreboard = mainScoreboard();
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective != null && hasDeathCountCriterion(objective)) {
            return objective;
        }
        if (objective == null) {
            getSLF4JLogger().warn("Objective '{}' wurde entfernt und wird neu angelegt", OBJECTIVE_NAME);
        }
        return setUpObjective(scoreboard);
    }

    /** Setzt den Wert eines Spielers auf 0, falls er noch keinen hat – sonst bliebe die Anzeige leer. */
    void initScore(Player player) {
        initScore(currentObjective(), player);
    }

    private static void initScore(Objective objective, Player player) {
        Score score = objective.getScore(player);
        if (!score.isScoreSet()) {
            score.setScore(0);
        }
    }
}
