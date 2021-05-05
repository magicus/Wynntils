/*
 *  * Copyright © Wynntils - 2018 - 2020.
 */

package com.wynntils.model.types;

import java.util.Locale;

public enum ClassType {
    MAGE("Mage/Dark Wizard"),
    ARCHER("Archer/Hunter"),
    WARRIOR("Warrior/Knight"),
    ASSASSIN("Assassin/Ninja"),
    SHAMAN("Shaman/Skyseer"),
    NONE("none");

    private final String displayName;

    ClassType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ClassType fromString(String classString) {
        ClassType selectedClass;

        try {
            selectedClass = ClassType.valueOf(classString.toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            switch (classString) {
                case "Hunter":
                    selectedClass = ClassType.ARCHER;
                    break;
                case "Knight":
                    selectedClass = ClassType.WARRIOR;
                    break;
                case "Dark Wizard":
                    selectedClass = ClassType.MAGE;
                    break;
                case "Ninja":
                    selectedClass = ClassType.ASSASSIN;
                    break;
                case "Skyseer":
                    selectedClass = ClassType.SHAMAN;
                    break;
                default:
                    selectedClass = ClassType.NONE;
            }
        }
        return selectedClass;
    }
}
