/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.quests;

import com.wynntils.utils.Pair;
import java.util.List;
import java.util.Objects;

public class Quest {
    // Quest metadata is forever constant
    private final String name;
    private final int level;
    private final QuestType type;
    private final QuestLength length;

    /** Additional requirements as pairs of <"profession name", minLevel> */
    private final List<Pair<String, Integer>> additionalRequirements;

    public Quest(
            String name,
            int level,
            QuestType type,
            QuestLength length,
            List<Pair<String, Integer>> additionalRequirements) {
        this.name = name;
        this.level = level;
        this.type = type;
        this.length = length;
        this.additionalRequirements = additionalRequirements;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public QuestType getType() {
        return type;
    }

    public QuestLength getLength() {
        return length;
    }

    public String getFullName() {
        return type.isMiniQuest() ? "Mini-Quest - " + name : name;
    }

    public List<Pair<String, Integer>> getAdditionalRequirements() {
        return additionalRequirements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quest quest = (Quest) o;
        return level == quest.level
                && type == quest.type
                && Objects.equals(name, quest.name)
                && length == quest.length
                && Objects.equals(additionalRequirements, quest.additionalRequirements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, length, level, additionalRequirements, type);
    }

    @Override
    public String toString() {
        return "Quest[" + "name='"
                + name + '\'' + ", level="
                + level + ", type="
                + type + ", length="
                + length + ", additionalRequirements="
                + additionalRequirements + ']';
    }
}
