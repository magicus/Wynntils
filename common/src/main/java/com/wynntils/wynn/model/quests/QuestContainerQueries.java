/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.quests;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.managers.Managers;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wynn.model.container.ContainerContent;
import com.wynntils.wynn.model.container.ScriptedContainerQuery;
import com.wynntils.wynn.utils.ContainerUtils;
import com.wynntils.wynn.utils.InventoryUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class QuestContainerQueries {
    private static final int NEXT_PAGE_SLOT = 8;
    private static final int MINI_QUESTS_SLOT = 53;
    private static final Map<QuestType, Integer> MAX_PAGE = Map.of(QuestType.NORMAL, 4, QuestType.MINIQUEST, 3);

    private List<QuestInfo> collectedQuests;
    private QuestInfo foundTrackedQuest;

    /**
     * Trigger a rescan of the quest book. When the rescan is done, a QuestBookReloadedEvent will
     * be sent. The available quests are then available using getQuests.
     */
    protected void queryQuestBook(QuestType type) {
        ScriptedContainerQuery.QueryBuilder queryBuilder = ScriptedContainerQuery.builder(
                        "Quest Book Query [" + type + "]")
                .onError(msg -> {
                    WynntilsMod.warn("Problem querying Quest Book [" + type + "]: " + msg);
                    McUtils.sendMessageToClient(
                            new TextComponent("Error updating quest book.").withStyle(ChatFormatting.RED));
                })
                .useItemInHotbar(InventoryUtils.QUEST_BOOK_SLOT_NUM)
                .matchTitle(Managers.Quest.getQuestBookTitle(1));

        if (type == QuestType.MINIQUEST) {
            queryBuilder.clickOnSlot(MINI_QUESTS_SLOT).matchTitle(getQuestBookTitle(1, QuestType.MINIQUEST));
        }

        queryBuilder.processContainer(c -> processQuestBookPage(c, 1, type, MAX_PAGE.get(type)));

        for (int i = 2; i <= MAX_PAGE.get(type); i++) {
            final int page = i; // Lambdas need final variables
            queryBuilder
                    .clickOnSlotWithName(NEXT_PAGE_SLOT, Items.GOLDEN_SHOVEL, getNextPageButtonName(page))
                    .matchTitle(getQuestBookTitle(page, type))
                    .processContainer(c -> processQuestBookPage(c, page, type, MAX_PAGE.get(type)));
        }

        queryBuilder.build().executeQuery();
    }

    private void processQuestBookPage(ContainerContent container, int page, QuestType type, int maxPage) {
        // Quests are in the top-left container area
        if (page == 1) {
            // Build new set of quests without disturbing current set
            collectedQuests = new ArrayList<>();
        }
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                int slot = row * 9 + col;

                // Very first slot for normal quests is chat history
                if (type == QuestType.NORMAL && slot == 0) continue;

                ItemStack item = container.items().get(slot);
                QuestInfo questInfo = QuestInfoParser.parseItem(item, page, type);
                if (questInfo == null) continue;

                collectedQuests.add(questInfo);
                if (questInfo.isTracked()) {
                    foundTrackedQuest = questInfo;
                }
            }
        }

        if (page == maxPage) {
            // Last page finished
            reportDiscoveredQuests(type, collectedQuests, foundTrackedQuest);
        }
    }

    private void reportDiscoveredQuests(QuestType type, List<QuestInfo> quests, QuestInfo trackedQuest) {
        // Call back to manager with our result
        Managers.Quest.updateQuestsFromQuery(type, quests, trackedQuest);
    }

    protected void toggleTracking(QuestInfo questInfo) {
        ScriptedContainerQuery.QueryBuilder queryBuilder = ScriptedContainerQuery.builder("Quest Book Quest Pin Query")
                .onError(msg -> WynntilsMod.warn("Problem pinning quest in Quest Book: " + msg))
                .useItemInHotbar(InventoryUtils.QUEST_BOOK_SLOT_NUM)
                .matchTitle(Managers.Quest.getQuestBookTitle(1));

        if (questInfo.getQuest().getType().isMiniQuest()) {
            queryBuilder.clickOnSlot(MINI_QUESTS_SLOT).matchTitle(getQuestBookTitle(1, QuestType.MINIQUEST));
        }

        if (questInfo.getPageNumber() > 1) {
            for (int i = 2; i <= questInfo.getPageNumber(); i++) {
                queryBuilder // we ignore this because this is not the correct page
                        .clickOnSlotWithName(NEXT_PAGE_SLOT, Items.GOLDEN_SHOVEL, getNextPageButtonName(i))
                        .matchTitle(Managers.Quest.getQuestBookTitle(i));
            }
        }
        queryBuilder
                .processContainer(c -> findQuestForTracking(c, questInfo))
                .build()
                .executeQuery();
    }

    private void findQuestForTracking(ContainerContent container, QuestInfo questInfo) {
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                int slot = row * 9 + col;

                // Very first slot is chat history
                if (slot == 0) continue;

                ItemStack item = container.items().get(slot);

                String questName = QuestInfoParser.getQuestName(item);
                if (Objects.equals(questName, questInfo.getQuest().getName())) {
                    ContainerUtils.clickOnSlot(
                            slot, container.containerId(), GLFW.GLFW_MOUSE_BUTTON_LEFT, container.items());
                    return;
                }
            }
        }
    }

    private String getNextPageButtonName(int nextPageNum) {
        return "[§f§lPage " + nextPageNum + "§a >§2>§a>§2>§a>]";
    }

    private String getQuestBookTitle(int pageNum, QuestType type) {
        return "^§0\\[Pg. " + pageNum + "\\] §8.*§0 " + (type.isMiniQuest() ? "Mini-" : "") + "Quests$";
    }
}
