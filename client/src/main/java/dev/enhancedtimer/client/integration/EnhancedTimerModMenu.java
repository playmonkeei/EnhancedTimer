package dev.enhancedtimer.client.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.enhancedtimer.client.ui.TimerScreen;

/** Loaded by Mod Menu only when it is installed (Fabric/Quilt). */
public final class EnhancedTimerModMenu implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() { return TimerScreen::new; }
}
