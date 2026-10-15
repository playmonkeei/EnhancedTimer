package dev.enhancedtimer.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimerColorTest {
    @Test void paletteMatchesMinecraftColorsAndRestoresOldSelections() {
        int[] colors = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
                0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
        for (int i = 0; i < colors.length; i++) {
            var mode = TimerColor.values()[i];
            assertEquals(0xFF000000 | colors[i], mode.argb(0x123456, 123456789, 4));
            assertEquals(mode, TimerColor.fromLegacyColor(colors[i]));
        }
        assertEquals(TimerColor.CUSTOM, TimerColor.fromLegacyColor(0xFF123456));
    }

    @Test void customColorStaysOpaqueAndIndependentOfAnimation() {
        assertEquals(0xFF123456, TimerColor.CUSTOM.argb(0x00123456, 0, 0));
        assertEquals(0xFF123456, TimerColor.CUSTOM.argb(0x00123456, 987654321, 9));
    }

    @Test void rainbowMovesAcrossCharactersAndWrapsWithoutAJumptoAnotherColor() {
        assertEquals(0xFFFF0000, TimerColor.RAINBOW.argb(0, 0, 0));
        assertEquals(0xFF00FFFF, TimerColor.RAINBOW.argb(0, 0, 6));
        assertEquals(0xFF00FFFF, TimerColor.RAINBOW.argb(0, 4_000_000_000L, 0));
        assertEquals(TimerColor.RAINBOW.argb(0, 0, 0), TimerColor.RAINBOW.argb(0, 8_000_000_000L, 0));
        assertEquals(TimerColor.RAINBOW.argb(0, -1, 0), TimerColor.RAINBOW.argb(0, 7_999_999_999L, 0));
        for (int i = 0; i < 12; i++) assertEquals(0xFF000000,
                TimerColor.RAINBOW.argb(0, Long.MIN_VALUE, i) & 0xFF000000);
    }
}
