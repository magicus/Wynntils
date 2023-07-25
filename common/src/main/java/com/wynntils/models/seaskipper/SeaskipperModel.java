/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.seaskipper;

import com.google.common.reflect.TypeToken;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.net.Download;
import com.wynntils.core.net.UrlId;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.ContainerSetSlotEvent;
import com.wynntils.mc.event.MenuEvent;
import com.wynntils.models.containers.ContainerModel;
import com.wynntils.models.items.items.gui.SeaskipperDestinationItem;
import com.wynntils.models.map.pois.SeaskipperPoi;
import com.wynntils.models.seaskipper.type.SeaskipperTravel;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.ContainerUtils;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public final class SeaskipperModel extends Model {
    private static final StyledText OAK_BOAT_NAME = StyledText.fromString("§bOak Boat");

    private final Map<String, SeaskipperPoi> allSeaskipperPois = new HashMap<>();

    private int underlyingContainerId = -2;
    private int boatSlot;
    private SeaskipperPoi currentPoi;
    private List<SeaskipperTravel> currentDestinations = new ArrayList<>();

    public SeaskipperModel(ContainerModel containerModel) {
        super(List.of(containerModel));

        loadSeaskipperPois();
    }

    @SubscribeEvent
    public void onMenuOpened(MenuEvent.MenuOpenedEvent event) {
        if (Models.Container.isSeaskipper(ComponentUtils.getUnformatted(event.getTitle()))) {
            underlyingContainerId = event.getContainerId();
            currentDestinations.clear();
            currentPoi = findCurrentPoi();
        }
    }

    @SubscribeEvent
    public void onMenuClosed(MenuEvent.MenuClosedEvent event) {
        underlyingContainerId = -2;
    }

    @SubscribeEvent
    public void onSetSlot(ContainerSetSlotEvent event) {
        if (event.getContainerId() != underlyingContainerId) return;

        if (StyledText.fromComponent(event.getItemStack().getHoverName()).equals(OAK_BOAT_NAME)) {
            this.boatSlot = event.getSlot();
            return;
        }

        Optional<SeaskipperDestinationItem> destinationItemOpt =
                Models.Item.asWynnItem(event.getItemStack(), SeaskipperDestinationItem.class);
        if (destinationItemOpt.isEmpty()) return;
        SeaskipperDestinationItem destinationItem = destinationItemOpt.get();
        SeaskipperPoi poi = allSeaskipperPois.get(destinationItem.getDestination());
        if (poi == null) {
            WynntilsMod.warn("Unknown Seaskipper destination: " + destinationItem.getDestination());
            return;
        }

        currentDestinations.add(new SeaskipperTravel(destinationItem, poi, event.getSlot()));
    }

    public List<SeaskipperDestinationItem> getDestinations() {
        return currentDestinations.stream().map(d -> d.destinationItem()).toList();
    }

    public List<SeaskipperPoi> getAvailableDestinations() {
        return currentDestinations.stream().map(d -> d.destinationPoi()).toList();
    }

    public List<SeaskipperPoi> getAllSeaskipperPois() {
        return allSeaskipperPois.values().stream().toList();
    }

    public void purchaseBoat() {
        clickSlot(boatSlot);
    }

    public SeaskipperPoi getCurrentPoi() {
        return currentPoi;
    }

    public void purchasePass(SeaskipperPoi destinationPoi) {
        Optional<SeaskipperTravel> destination = currentDestinations.stream()
                .filter(d -> d.destinationPoi().equals(destinationPoi))
                .findFirst();
        if (destination.isEmpty()) return;

        clickSlot(destination.get().slot());
    }

    private void clickSlot(int slot) {
        AbstractContainerMenu menu = McUtils.player().containerMenu;
        if (menu.containerId != underlyingContainerId) {
            WynntilsMod.error("Mismatched container id: " + menu.containerId + " vs " + underlyingContainerId);
            return;
        }

        ContainerUtils.clickOnSlot(slot, underlyingContainerId, GLFW.GLFW_MOUSE_BUTTON_LEFT, menu.getItems());
    }

    private void loadSeaskipperPois() {
        Download dl = Managers.Net.download(UrlId.DATA_STATIC_SEASKIPPER_LOCATIONS);
        dl.handleReader(reader -> {
            Type type = new TypeToken<ArrayList<SeaskipperProfile>>() {}.getType();
            List<SeaskipperProfile> seaskipperProfiles = WynntilsMod.GSON.fromJson(reader, type);

            for (SeaskipperProfile profile : seaskipperProfiles) {
                allSeaskipperPois.put(
                        profile.destination,
                        new SeaskipperPoi(
                                profile.destination,
                                profile.combatLevel,
                                profile.startX,
                                profile.startZ,
                                profile.endX,
                                profile.endZ));
            }
        });
    }

    private SeaskipperPoi findCurrentPoi() {
        for (SeaskipperPoi poi : allSeaskipperPois.values()) {
            if (poi.isPlayerInside()) {
                return poi;
            }
        }

        return null;
    }

    private static final class SeaskipperProfile {
        String destination;
        int combatLevel;
        int startX;
        int startZ;
        int endX;
        int endZ;
    }
}
