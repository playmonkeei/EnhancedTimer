package dev.enhancedtimer.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
public final class Platform {
    private Platform() {}
    public static Screen screen() { return Minecraft.getInstance().screen; }
    public static void screen(Screen screen) { Minecraft.getInstance().setScreen(screen); }
    public static boolean hudHidden() { return Minecraft.getInstance().options.hideGui; }
    public static void history(String text) { Minecraft.getInstance().gui.getChat().addRecentChat(text); }
}
