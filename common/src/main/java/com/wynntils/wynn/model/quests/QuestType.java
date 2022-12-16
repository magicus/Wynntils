/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.quests;

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
