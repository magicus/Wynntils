package com.wynntils.model.updater;

import com.wynntils.ModCore;
import com.wynntils.model.WynnImpl;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.Locale;

public class WynncraftServerUpdater {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerJoin(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        ServerData currentServer = ModCore.mc().getCurrentServerData();
        String lowerCaseIP = currentServer == null || currentServer.serverIP == null ? null : currentServer.serverIP.toLowerCase(Locale.ROOT);
        boolean onServer = !ModCore.mc().isSingleplayer() && lowerCaseIP != null && !currentServer.isOnLAN() && lowerCaseIP.contains("wynncraft");

        WynnImpl.getInstance().setConnected(onServer);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onServerLeave(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        WynnImpl.getInstance().setConnected(false);
    }

    @SubscribeEvent
    public void onWorldLeave(GuiOpenEvent e) {
        if (e.getGui() instanceof GuiDisconnected) {
            WynnImpl.getInstance().setConnected(false);
        }
    }
}
