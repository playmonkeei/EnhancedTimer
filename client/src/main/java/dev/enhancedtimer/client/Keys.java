package dev.enhancedtimer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class Keys {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("enhancedtimer", "controls"));
    public static final KeyMapping MENU = new KeyMapping("key.enhancedtimer.menu", InputConstants.KEY_B, CATEGORY);
    public static final KeyMapping TOGGLE = new KeyMapping("key.enhancedtimer.toggle", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping STOP = new KeyMapping("key.enhancedtimer.stop", InputConstants.UNKNOWN.getValue(), CATEGORY);
    private Keys() {}
}

