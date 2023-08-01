/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.config;

import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.Translatable;
import com.wynntils.core.json.PersistedValue;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.stream.Stream;

public class Config<T> extends PersistedValue<T> implements Comparable<Config<T>> {
    private ConfigHolder<T> configHolder;

    public Config(T value) {
        super(value);
    }

    <P extends Configurable & Translatable> void createConfigHolder(
            P parent, Field configField, RegisterConfig configInfo) {
        Type valueType = Managers.Json.getJsonValueType(configField);
        String fieldName = configField.getName();

        boolean visible = !(this instanceof HiddenConfig<?>);

        String i18nKey = configInfo.i18nKey();

        configHolder = new ConfigHolder<>(parent, this, fieldName, i18nKey, visible, valueType);
    }

    public ConfigHolder<T> getConfigHolder() {
        return configHolder;
    }

    @Override
    public void touched() {
        Managers.Config.saveConfig();
    }

    public void updateConfig(T value) {
        this.value = value;
    }

    // This must only be called by StorageManager when restoring value from disk
    @SuppressWarnings("unchecked")
    void restoreValue(Object value) {
        this.value = (T) value;
    }

    @Override
    public int compareTo(Config<T> other) {
        return getConfigHolder().getJsonName().compareTo(other.getJsonName());
    }

    public Stream<String> getValidLiterals() {
        return getConfigHolder().getValidLiterals();
    }

    public Type getType() {
        return getConfigHolder().getType();
    }

    public String getFieldName() {
        return getConfigHolder().getFieldName();
    }

    public Configurable getParent() {
        return getConfigHolder().getParent();
    }

    public String getJsonName() {
        return getConfigHolder().getJsonName();
    }

    public boolean isVisible() {
        return getConfigHolder().isVisible();
    }

    public String getDisplayName() {
        return getConfigHolder().getDisplayName();
    }

    public String getDescription() {
        return getConfigHolder().getDescription();
    }

    public T getValue() {
        return getConfigHolder().getValue();
    }

    public String getValueString() {
        return getConfigHolder().getValueString();
    }

    public boolean isEnum() {
        return getConfigHolder().isEnum();
    }

    public T getDefaultValue() {
        return getConfigHolder().getDefaultValue();
    }

    public void setValue(T value) {
        getConfigHolder().setValue(value);
    }

    public boolean valueChanged() {
        return getConfigHolder().valueChanged();
    }

    public void reset() {
        getConfigHolder().reset();
    }

    public T tryParseStringValue(String value) {
        return getConfigHolder().tryParseStringValue(value);
    }
}
