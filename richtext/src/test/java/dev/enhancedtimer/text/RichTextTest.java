package dev.enhancedtimer.text;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RichTextTest {
    @Test void miniMessageSupportsColorsDecorationAndGradients() {
        String json = RichText.json("<bold><gradient:#ff0000:#0000ff>12:34</gradient></bold>").toLowerCase(java.util.Locale.ROOT);
        assertTrue(json.contains("bold")); assertTrue(json.contains("#ff0000")); assertTrue(json.contains("#0000ff"));
        assertTrue(RichText.legacy("<gold>Timer</gold>").contains("Timer"));
    }
    @Test void phaseIsBoundedAndOversizedInputRejected() {
        assertEquals("<gradient:red:blue:0.000>x</gradient>", RichText.animate("<gradient:red:blue:{phase}>x</gradient>", 0));
        assertThrows(IllegalArgumentException.class, () -> RichText.json("a".repeat(1025)));
    }
}
