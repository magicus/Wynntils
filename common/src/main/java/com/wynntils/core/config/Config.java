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
        return getConfigHolder().getJsonName().compareTo(other.getConfigHolder().getJsonName());
    }
}
