/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted;

import java.lang.reflect.Type;

public class PersistedMetadata<T> {
    private final PersistedOwner owner;
    private final String fieldName;
    private final String i18nKey;
    private final Type valueType;
    private final boolean allowNull;

    private final T defaultValue;

    private boolean userEdited = false;

    public PersistedMetadata(
            PersistedOwner owner, String fieldName, Type valueType, T defaultValue, String i18nKey, boolean allowNull) {
        this.owner = owner;
        this.fieldName = fieldName;
        this.valueType = valueType;
        this.defaultValue = defaultValue;
        this.i18nKey = i18nKey;
        this.allowNull = allowNull;
    }

    public Type getValueType() {
        return valueType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public PersistedOwner getOwner() {
        return owner;
    }

    public String getJsonName() {
        // FIXME
        // return owner.getPersistedJsonName() + "." + fieldName;
        return "";
    }

    public String getI18nKey() {
        return i18nKey;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public boolean isUserEdited() {
        return userEdited;
    }

    public boolean isAllowNull() {
        return allowNull;
    }
}
