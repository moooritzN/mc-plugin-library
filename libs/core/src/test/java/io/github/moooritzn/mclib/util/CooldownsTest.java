package io.github.moooritzn.mclib.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CooldownsTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void cooldownExpiresAfterDuration() {
        MutableClock clock = new MutableClock();
        Cooldowns<String> cooldowns = new Cooldowns<>(clock);

        assertTrue(cooldowns.tryStart("a", Duration.ofSeconds(10)));
        assertFalse(cooldowns.tryStart("a", Duration.ofSeconds(10)));
        assertEquals(Duration.ofSeconds(10), cooldowns.remaining("a"));

        clock.advance(Duration.ofSeconds(4));
        assertEquals(Duration.ofSeconds(6), cooldowns.remaining("a"));

        clock.advance(Duration.ofSeconds(6));
        assertFalse(cooldowns.isActive("a"));
        assertTrue(cooldowns.tryStart("a", Duration.ofSeconds(1)));
    }

    @Test
    void resetClearsCooldown() {
        Cooldowns<String> cooldowns = new Cooldowns<>(new MutableClock());
        cooldowns.start("a", Duration.ofMinutes(1));
        cooldowns.reset("a");
        assertFalse(cooldowns.isActive("a"));
    }
}
