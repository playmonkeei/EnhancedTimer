package dev.enhancedtimer.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
public final class Platform {
    private Platform() {}
    public static Screen screen() { return Minecraft.getInstance().gui.screen(); }
    public static void screen(Screen screen) { Minecraft.getInstance().gui.setScreen(screen); }
    public static boolean hudHidden() { return Minecraft.getInstance().gui.hud.isHidden(); }
    public static void history(String text) { Minecraft.getInstance().gui.hud.getChat().addRecentChat(text); }
}
