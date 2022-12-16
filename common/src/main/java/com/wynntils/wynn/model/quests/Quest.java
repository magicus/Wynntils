/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.quests;

import com.wynntils.utils.Pair;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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

    public List<Pair<String, Integer>> getAdditionalRequirements() {
        return additionalRequirements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || (!(o instanceof Quest quest))) return false;

        return new EqualsBuilder()
                .append(level, quest.level)
                .append(name, quest.name)
                .append(type, quest.type)
                .append(length, quest.length)
                .append(additionalRequirements, quest.additionalRequirements)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(level)
                .append(type)
                .append(length)
                .append(additionalRequirements)
                .toHashCode();
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

    public enum QuestType {
        NORMAL,
        MINIQUEST;

        public static QuestType fromIsMiniQuestBoolean(boolean isMiniQuest) {
            return isMiniQuest ? MINIQUEST : NORMAL;
        }

        // Convenience getter
        public boolean isMiniQuest() {
            return this == MINIQUEST;
        }
    }
}
