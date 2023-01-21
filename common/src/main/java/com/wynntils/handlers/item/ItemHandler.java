/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.item;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handler;
import com.wynntils.mc.event.ContainerSetContentEvent;
import com.wynntils.mc.event.SetSlotEvent;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.ItemUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.wynn.WynnUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ItemHandler extends Handler {
    private final List<ItemAnnotator> annotators = new ArrayList<>();
    private Map<Class<?>, Integer> profilingTimes = new HashMap<>();
    private Map<Class<?>, Integer> profilingCounts = new HashMap<>();
    // Keep this as a field just of performance reasons to skip a new allocation in annotate()
    private List<ItemAnnotator> crashedAnnotators = new ArrayList<>();

    public static Optional<ItemAnnotation> getItemStackAnnotation(ItemStack item) {
        if (item == null) return Optional.empty();

        ItemAnnotation annotation = ((AnnotatedItemStack) item).getAnnotation();
        return Optional.ofNullable(annotation);
    }

    public void registerAnnotator(ItemAnnotator annotator) {
        annotators.add(annotator);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSetSlot(SetSlotEvent.Pre event) {
        updateAnnotation(event.getContainer().getItem(event.getSlot()), event.getItem());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onContainerSetContent(ContainerSetContentEvent.Pre event) {
        NonNullList<ItemStack> existingItems;
        if (event.getContainerId() == 0) {
            // Set all for inventory
            existingItems = McUtils.player().getInventory().items;
        } else if (event.getContainerId() == McUtils.player().containerMenu.containerId) {
            // Set all for the currently open container. Vanilla has copied inventory in the last
            // slots
            existingItems = McUtils.player().containerMenu.getItems();
        } else {
            // No matching container, just ignore this
            return;
        }

        List<ItemStack> newItems = event.getItems();

        if (newItems.size() != existingItems.size()) {
            // This is not a proper update, just annotate everyting
            for (ItemStack newItem : newItems) {
                annotate(newItem);
            }
            return;
        }

        for (int i = 0; i < newItems.size(); i++) {
            updateAnnotation(existingItems.get(i), newItems.get(i));
        }
    }

    private void updateAnnotation(ItemStack existingItem, ItemStack newItem) {
        ItemAnnotation annotation = ((AnnotatedItemStack) existingItem).getAnnotation();
        if (annotation != null && ItemUtils.isEquals(existingItem, newItem)) {
            // Copy existing annotation
            ((AnnotatedItemStack) newItem).setAnnotation(annotation);
        } else {
            // Calculate a new annotation
            annotate(newItem);
        }
    }

    private void annotate(ItemStack item) {
        ItemAnnotation annotation = null;

        long startTime = System.currentTimeMillis();
        String name = WynnUtils.normalizeBadString(ComponentUtils.getCoded(item.getHoverName()));

        for (ItemAnnotator annotator : annotators) {
            try {
                annotation = annotator.getAnnotation(item, name);
                if (annotation != null) {
                    break;
                }
            } catch (Throwable t) {
                String annotatorName = annotator.getClass().getSimpleName();
                WynntilsMod.error("Exception when processing item annotator " + annotatorName, t);
                WynntilsMod.warn("This annotator will be disabled");
                WynntilsMod.warn("Problematic item:" + item);
                WynntilsMod.warn("Problematic item name:" + ComponentUtils.getCoded(item.getHoverName()));
                WynntilsMod.warn("Problematic item tags:" + item.getTag());
                McUtils.sendMessageToClient(Component.literal("Wynntils error: Item Annotator '" + annotatorName
                                + "' has crashed and will be disabled. Not all items will be properly parsed.")
                        .withStyle(ChatFormatting.RED));
                // We can't disable it right away since that will cause ConcurrentModificationException
                crashedAnnotators.add(annotator);
            }
        }

        // Hopefully we have none :)
        for (ItemAnnotator annotator : crashedAnnotators) {
            annotators.remove(annotator);
        }
        crashedAnnotators.clear();

        if (annotation == null) return;

        // Measure performance
        logProfilingData(startTime, annotation);

        ((AnnotatedItemStack) item).setAnnotation(annotation);
    }

    private void logProfilingData(long startTime, ItemAnnotation annotation) {
        long endTime = System.currentTimeMillis();
        int timeSpent = (int) (endTime - startTime);
        int allTime = profilingTimes.getOrDefault(annotation.getClass(), 0);
        profilingTimes.put(annotation.getClass(), allTime + timeSpent);

        int allCount = profilingCounts.getOrDefault(annotation.getClass(), 0);
        profilingCounts.put(annotation.getClass(), allCount + 1);
    }

    public Map<Class<?>, Integer> getProfilingTimes() {
        return profilingTimes;
    }

    public Map<Class<?>, Integer> getProfilingCounts() {
        return profilingCounts;
    }

    public void resetProfiling() {
        profilingTimes.clear();
        profilingCounts.clear();
    }
}