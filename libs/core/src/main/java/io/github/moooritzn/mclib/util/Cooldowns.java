package io.github.moooritzn.mclib.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Einfache, thread-sichere Cooldown-Verwaltung pro Schlüssel (z.B. Spieler-UUID).
 *
 * @param <K> Schlüsseltyp
 */
public final class Cooldowns<K> {

    private final Map<K, Instant> expiries = new ConcurrentHashMap<>();
    private final Clock clock;

    public Cooldowns() {
        this(Clock.systemUTC());
    }

    public Cooldowns(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Setzt (oder überschreibt) einen Cooldown für den Schlüssel. */
    public void start(K key, Duration duration) {
        expiries.put(key, clock.instant().plus(duration));
    }

    /** Prüft, ob für den Schlüssel noch ein Cooldown läuft. */
    public boolean isActive(K key) {
        return !remaining(key).isZero();
    }

    /** Verbleibende Zeit, oder {@link Duration#ZERO}, wenn kein Cooldown aktiv ist. */
    public Duration remaining(K key) {
        Instant expiry = expiries.get(key);
        if (expiry == null) {
            return Duration.ZERO;
        }
        Duration left = Duration.between(clock.instant(), expiry);
        if (left.isNegative() || left.isZero()) {
            expiries.remove(key, expiry);
            return Duration.ZERO;
        }
        return left;
    }

    /**
     * Startet den Cooldown nur, wenn aktuell keiner aktiv ist.
     *
     * @return {@code true}, wenn der Cooldown gestartet wurde (die Aktion also ausgeführt werden darf)
     */
    public boolean tryStart(K key, Duration duration) {
        Instant now = clock.instant();
        Instant newExpiry = now.plus(duration);
        boolean[] started = {false};
        expiries.compute(key, (k, current) -> {
            if (current != null && current.isAfter(now)) {
                return current;
            }
            started[0] = true;
            return newExpiry;
        });
        return started[0];
    }

    /** Entfernt den Cooldown für den Schlüssel. */
    public void reset(K key) {
        expiries.remove(key);
    }
}
