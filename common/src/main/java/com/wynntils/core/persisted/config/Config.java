/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted.config;

import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Configurable;
import com.wynntils.core.persisted.OldPersistedMetadata;
import com.wynntils.core.persisted.PersistedMetadata;
import com.wynntils.core.persisted.PersistedValue;
import java.lang.reflect.Type;
import java.util.stream.Stream;

public class Config<T> extends PersistedValue<T> implements Comparable<Config<T>> {
    public Config(T value) {
        super(value);
    }

    @Override
    public void touched() {
        Managers.Config.saveConfig();
    }

    private PersistedMetadata<T> getPersistedMetadata() {
        return Managers.Persisted.getMetadata(this);
    }

    private OldPersistedMetadata<T> getOldPersistedMetadata() {
        PersistedMetadata<T> metadata = getPersistedMetadata();
        boolean isVisible = this instanceof HiddenConfig<T>;
        return new OldPersistedMetadata<T>(
                (Configurable) metadata.getOwner(),
                this,
                metadata.getFieldName(),
                metadata.getI18nKey(),
                isVisible,
                metadata.getValueType());
    }

    @Override
    public int compareTo(Config<T> other) {
        return getPersistedMetadata().getJsonName().compareTo(other.getJsonName());
    }

    public Stream<String> getValidLiterals() {
        return getOldPersistedMetadata().getValidLiterals();
    }

    public Type getType() {
        return getPersistedMetadata().getValueType();
    }

    public String getFieldName() {
        return getPersistedMetadata().getFieldName();
    }

    public Configurable getParent() {
        return (Configurable) getPersistedMetadata().getOwner();
    }

    public String getJsonName() {
        return getPersistedMetadata().getJsonName();
    }

    public boolean isVisible() {
        return getOldPersistedMetadata().isVisible();
    }

    public String getDisplayName() {
        return getOldPersistedMetadata().getDisplayName();
    }

    public String getDescription() {
        return getOldPersistedMetadata().getDescription();
    }

    public T getValue() {
        return getOldPersistedMetadata().getValue();
    }

    public String getValueString() {
        return getOldPersistedMetadata().getValueString();
    }

    public boolean isEnum() {
        return getOldPersistedMetadata().isEnum();
    }

    public T getDefaultValue() {
        return getPersistedMetadata().getDefaultValue();
    }

    public void setValue(T value) {
        getOldPersistedMetadata().setValue(value);
    }

    void restoreValue(Object value) {
        getOldPersistedMetadata().restoreValue(value);
    }

    public boolean valueChanged() {
        return getOldPersistedMetadata().valueChanged();
    }

    public void reset() {
        getOldPersistedMetadata().reset();
    }

    public T tryParseStringValue(String value) {
        return getOldPersistedMetadata().tryParseStringValue(value);
    }
}
