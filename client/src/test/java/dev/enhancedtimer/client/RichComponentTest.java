package dev.enhancedtimer.client;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.enhancedtimer.text.RichText;
import net.minecraft.network.chat.ComponentSerialization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

class RichComponentTest {
    @BeforeAll static void bootstrap() { net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap(); }
    @Test void adventureTextRoundTripsIntoVanillaComponentCodec() {
        var json = JsonParser.parseString(RichText.json("<gold><bold>02:00:00</bold></gold>"));
        var result = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json);
        assertTrue(result.result().isPresent(), result.toString());
        assertEquals("02:00:00", result.result().orElseThrow().getString());
    }
    @Test void gradientsRemainValidVanillaText() {
        var json = JsonParser.parseString(RichText.json("<gradient:red:blue>Timer</gradient>"));
        var result = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json);
        assertTrue(result.result().isPresent(), result.toString());
        assertEquals("Timer", result.result().orElseThrow().getString());
    }
}
