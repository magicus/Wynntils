package com.wynntils.model.updater;

import com.mojang.authlib.GameProfile;
import com.wynntils.Reference;
import com.wynntils.core.events.custom.PacketEvent;
import com.wynntils.core.utils.reflections.ReflectionMethods;
import com.wynntils.model.WynnImpl;
import com.wynntils.model.events.WynncraftServerEvent;
import com.wynntils.model.states.World;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.UUID;

public class WorldUpdater {
    private static final UUID WORLD_UUID = UUID.fromString("16ff7452-714f-3752-b3cd-c3cb2068f6af");
    private String lastWorld = "";
    private boolean acceptLeft = false;

    @SubscribeEvent
    public void onTabListChange(PacketEvent<SPacketPlayerListItem> e) {
        if (!Reference.onServer) return;
        if (e.getPacket().getAction() != SPacketPlayerListItem.Action.UPDATE_DISPLAY_NAME && e.getPacket().getAction() != SPacketPlayerListItem.Action.REMOVE_PLAYER)
            return;

        // DO NOT remove cast or reflection otherwise the build will fail
        for (Object player : (List<?>) e.getPacket().getEntries()) {
            // world handling below
            GameProfile profile = (GameProfile) ReflectionMethods.SPacketPlayerListItem$AddPlayerData_getProfile.invoke(player);
            if (profile.getId().equals(WORLD_UUID)) {
                if (e.getPacket().getAction() == SPacketPlayerListItem.Action.UPDATE_DISPLAY_NAME) {
                    ITextComponent nameComponent = (ITextComponent) ReflectionMethods.SPacketPlayerListItem$AddPlayerData_getDisplayName.invoke(player);
                    if (nameComponent == null) continue;
                    String name = nameComponent.getUnformattedText();
                    String worldName = name.substring(name.indexOf("[") + 1, name.indexOf("]"));

                    if (worldName.equalsIgnoreCase(lastWorld)) continue;

                    World world = World.fromName(worldName);
                    WynnImpl.getInstance().setWorld(world);
                    lastWorld = worldName;
                    acceptLeft = true;
                } else if (acceptLeft) {
                    acceptLeft = false;
                    lastWorld = "";
                    WynnImpl.getInstance().setWorld(World.NONE);
                }
            }
        }
    }

    @SubscribeEvent
    public void onServerLeave(WynncraftServerEvent.Leave e) {
        WynnImpl.getInstance().setWorld(World.NONE);
    }

}
