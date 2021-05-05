package com.wynntils.model.states;

import com.wynntils.model.types.ClassType;

public class PlayerClass {
    public final static int NO_CLASS_ID = -1;
    public final static PlayerClass NONE = of(ClassType.NONE, NO_CLASS_ID);
    private final ClassType classType;
    private final int classId;

    private PlayerClass(ClassType classType, int classId) {
        this.classType = classType;
        this.classId = classId;
    }

    public static PlayerClass of(ClassType classType, int classId) {
        return new PlayerClass(classType, classId);
    }

    public int getClassId() {
        return classId;
    }

    public ClassType getClassType() {
        return classType;
    }
}
