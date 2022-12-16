/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.quests;

import static com.wynntils.wynn.model.quests.QuestManager.MINI_QUEST_PREFIX;

public enum QuestType {
    NORMAL,
    MINIQUEST;

    public static QuestType fromIsMiniQuestBoolean(boolean isMiniQuest) {
        return isMiniQuest ? MINIQUEST : NORMAL;
    }

    public static QuestType fromName(String name) {
        return fromIsMiniQuestBoolean(name.startsWith(MINI_QUEST_PREFIX));
    }

    // Convenience getter
    public boolean isMiniQuest() {
        return this == MINIQUEST;
    }
}
