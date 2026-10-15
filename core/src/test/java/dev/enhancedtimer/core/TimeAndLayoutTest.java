package dev.enhancedtimer.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimeAndLayoutTest {
    @Test void durationInputs() {
        for (String value : new String[]{"2h", "120m", "7200", "02:00:00", "120:00", "1h60m"})
            assertEquals(7_200_000, Durations.parse(value), value);
        assertEquals(5_403_000, Durations.parse("1h30m3s"));
    }
    @Test void malformedAndOverflowingDurations() {
        for (String value : new String[]{"", "0", "-1", "2.5h", "1:60", "1:2:3:4", "1hgarbage", "999999999999999h", "366d", "8761h"})
            assertThrows(IllegalArgumentException.class, () -> Durations.parse(value), value);
    }
    @Test void formattingDoesNotShowZeroBeforeCountdownExpires() {
        assertEquals("00:00:01", TimeFormat.CLOCK.format(1, true));
        assertEquals("00:00:00", TimeFormat.CLOCK.format(999, false));
        assertEquals("25:00:00", TimeFormat.CLOCK.format(90_000_000, false));
        assertEquals("00:00:01.234", TimeFormat.PRECISE.format(1234, false));
    }
    @Test void customPositionsStayInsideDifferentResolutions() {
        for (int width : new int[]{50, 320, 1920}) for (int height : new int[]{40, 240, 1080})
            for (var anchor : HudPosition.Anchor.values()) {
                var point = HudPosition.place(anchor, 0.8, 0.9, width, height, 40, 20);
                assertTrue(point.x() >= 0 && point.x() + 40 <= width);
                assertTrue(point.y() >= 0 && point.y() + 20 <= height);
            }
        assertEquals(new HudPosition.Point(100, 50), HudPosition.place(HudPosition.Anchor.CUSTOM, 1, 1, 140, 70, 40, 20));
    }
    @Test void normalizedDragRoundTripsAndClamps() {
        assertEquals(0.5, HudPosition.normalize(80, 200, 40));
        assertEquals(0, HudPosition.normalize(-20, 200, 40));
        assertEquals(1, HudPosition.normalize(500, 200, 40));
        var point = HudPosition.place(HudPosition.Anchor.CUSTOM, Double.NaN, Double.POSITIVE_INFINITY, 200, 100, 40, 20);
        assertEquals(new HudPosition.Point(80, 40), point);
    }
}
