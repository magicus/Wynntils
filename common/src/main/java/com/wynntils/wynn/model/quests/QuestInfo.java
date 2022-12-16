/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.quests;

import com.wynntils.core.managers.Managers;
import com.wynntils.mc.objects.Location;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.utils.Pair;
import com.wynntils.utils.StringUtils;
import com.wynntils.wynn.objects.profiles.ingredient.ProfessionType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;

public class QuestInfo {
    private static final int NEXT_TASK_MAX_WIDTH = 200;
    private static final Pattern COORDINATE_PATTERN = Pattern.compile(".*\\[(-?\\d+), ?(-?\\d+), ?(-?\\d+)\\].*");

    // Quest metadata is forever constant
    private final Quest quest;

    // Quest progress can change over time
    private QuestStatus status;
    private final int pageNumber;
    private boolean tracked;
    private String nextTask;

    protected QuestInfo(Quest quest, QuestStatus status, int pageNumber, boolean tracked, String nextTask) {
        this.quest = quest;
        this.status = status;
        this.pageNumber = pageNumber;
        this.tracked = tracked;
        this.nextTask = nextTask;
    }

    protected QuestInfo(
            String name,
            QuestStatus status,
            QuestLength length,
            int level,
            String nextTask,
            List<Pair<String, Integer>> additionalRequirements,
            boolean isMiniQuest,
            int pageNumber,
            boolean tracked) {
        this(
                new Quest(
                        name,
                        level,
                        QuestType.fromIsMiniQuestBoolean(isMiniQuest),
                        length,
                        additionalRequirements),
                status,
                pageNumber, tracked, nextTask
        );
    }

    public Quest getQuest() {
        return quest;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public boolean isTrackable() {
        return status == QuestStatus.CAN_START || status == QuestStatus.STARTED;
    }

    public Optional<Location> getNextLocation() {
        Matcher matcher = COORDINATE_PATTERN.matcher(ComponentUtils.stripFormatting(nextTask));
        if (!matcher.matches()) return Optional.empty();

        return Optional.of(new Location(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))));
    }

    public int getSortLevel() {
        if (quest.getType().isMiniQuest() || quest.getAdditionalRequirements().isEmpty()) return quest.getLevel();

        return quest.getAdditionalRequirements().get(0).b();
    }

    public String getNextTask() {
        return nextTask;
    }

    public void setNextTask(String nextTask) {
        this.nextTask = nextTask;
    }

    public List<Pair<String, Integer>> getAdditionalRequirements() {
        return getQuest().getAdditionalRequirements();
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public boolean isTracked() {
        return tracked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || (!(o instanceof QuestInfo questInfo))) return false;

        // Consider it the same if the base quest is the same
        return this.quest.equals(questInfo.quest);
    }

    @Override
    public int hashCode() {
        return quest.hashCode();
    }

    @Override
    public String toString() {
        return "QuestInfo[" + "quest=" + quest + ", " + "status=" + status + ", " + "nextTask=\"" + nextTask + "]";
    }

    public static List<Component> generateTooltipForQuest(QuestInfo questInfo) {
        List<Component> tooltipLines = new ArrayList<>();

        tooltipLines.add(new TextComponent(questInfo.getQuest().getName())
                .withStyle(ChatFormatting.BOLD)
                .withStyle(ChatFormatting.WHITE));
        tooltipLines.add(questInfo.getStatus().getQuestBookComponent());
        tooltipLines.add(new TextComponent(""));
        // We always parse level as one, so check if this mini-quest does not have a min combat level
        if (!questInfo.getQuest().getType().isMiniQuest() || questInfo.getAdditionalRequirements().isEmpty()) {
            tooltipLines.add((Managers.Character.getCharacterInfo().getLevel() >= questInfo.getQuest().getLevel()
                            ? new TextComponent("✔").withStyle(ChatFormatting.GREEN)
                            : new TextComponent("✖").withStyle(ChatFormatting.RED))
                    .append(new TextComponent(" Combat Lv. Min: ").withStyle(ChatFormatting.GRAY))
                    .append(new TextComponent(String.valueOf(questInfo.getQuest().getLevel())).withStyle(ChatFormatting.WHITE)));
        }

        for (Pair<String, Integer> additionalRequirement : questInfo.getAdditionalRequirements()) {
            MutableComponent base = Managers.Character.getCharacterInfo()
                                    .getProfessionInfo()
                                    .getLevel(ProfessionType.fromString(additionalRequirement.a()))
                            >= additionalRequirement.b()
                    ? new TextComponent("✔ ").withStyle(ChatFormatting.GREEN)
                    : new TextComponent("✖ ").withStyle(ChatFormatting.RED);

            tooltipLines.add(base.append(new TextComponent(additionalRequirement.a() + " Lv. Min: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(new TextComponent(String.valueOf(additionalRequirement.b()))
                            .withStyle(ChatFormatting.WHITE))));
        }

        tooltipLines.add(new TextComponent("-")
                .withStyle(ChatFormatting.GREEN)
                .append(new TextComponent(" Length: ").withStyle(ChatFormatting.GRAY))
                .append(new TextComponent(StringUtils.capitalizeFirst(
                                questInfo.getQuest().getLength().toString().toLowerCase(Locale.ROOT)))
                        .withStyle(ChatFormatting.WHITE)));

        if (questInfo.getStatus() != QuestStatus.COMPLETED) {
            tooltipLines.add(new TextComponent(""));
            String nextTask = questInfo.getNextTask();
            String[] lines = StringUtils.wrapTextBySize(nextTask, NEXT_TASK_MAX_WIDTH);

            for (String line : lines) {
                tooltipLines.add(new TextComponent(line).withStyle(ChatFormatting.GRAY));
            }
        }

        return tooltipLines;
    }
}
