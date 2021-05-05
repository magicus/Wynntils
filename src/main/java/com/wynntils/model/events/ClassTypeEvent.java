/*
 *  * Copyright © Wynntils - 2018 - 2020.
 */

package com.wynntils.model.events;

import com.wynntils.model.states.PlayerClass;
import com.wynntils.model.types.ClassType;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Triggered when the player changes a class inside the Wynncraft Server
 */
public class ClassTypeEvent extends Event {
    public static class Join extends ClassTypeEvent {
        private final PlayerClass playerClass;

        public Join(PlayerClass playerClass) {
            this.playerClass = playerClass;
        }

        public PlayerClass getPlayerClass() {
            return playerClass;
        }
    }

    public static class Leave extends ClassTypeEvent {
    }
}
