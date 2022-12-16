/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.event;

import com.wynntils.wynn.model.quests.QuestType;
import net.minecraftforge.eventbus.api.Event;

public abstract class QuestBookReloadedEvent extends Event {
    public static class QuestsReloaded extends QuestBookReloadedEvent {
        private final QuestType type;

        public QuestsReloaded(QuestType type) {
            this.type = type;
        }

        public QuestType getType() {
            return type;
        }
    }

    public static class DialogueHistoryReloaded extends QuestBookReloadedEvent {}
}
