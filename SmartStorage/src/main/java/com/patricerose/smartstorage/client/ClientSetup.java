package com.patricerose.smartstorage.client;

import com.patricerose.smartstorage.SmartStorage;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only setup: tells the game which screen to show for the terminal. */
@Mod.EventBusSubscriber(modid = SmartStorage.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(SmartStorage.TERMINAL_MENU.get(), TerminalScreen::new));
    }
}
