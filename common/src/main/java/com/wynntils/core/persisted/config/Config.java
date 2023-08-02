/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted.config;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Configurable;
import com.wynntils.core.consumers.features.Translatable;
import com.wynntils.core.persisted.PersistedMetadata;
import com.wynntils.core.persisted.PersistedValue;
import com.wynntils.utils.EnumUtils;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.client.resources.language.I18n;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;

public class Config<T> extends PersistedValue<T> {
    private boolean userEdited = false;

    public Config(T value) {
        super(value);
    }

    @Override
    public void touched() {
        Managers.Config.saveConfig();
    }

    public boolean isVisible() {
        return true;
    }

    public T getValue() {
        return get();
    }

    public void setValue(T value) {
        if (value == null && !getMetadata().isAllowNull()) {
            WynntilsMod.warn("Trying to set null to config " + getJsonName() + ". Will be replaced by default.");
            reset();
            return;
        }

        Managers.Persisted.setRaw(this, value);
        ((Configurable) getMetadata().getOwner()).updateConfigOption(this);
        this.userEdited = true;
    }

    public void reset() {
        T defaultValue = getMetadata().getDefaultValue();

        // deep copy because writeField set's the field to be our default value instance when resetting, making default
        // value change with the field's actual value
        setValue(Managers.Json.deepCopy(defaultValue, getMetadata().getValueType()));
        // reset this flag so option is no longer saved to file
        this.userEdited = false;
    }

    public boolean valueChanged() {
        if (this.userEdited) {
            return true;
        }

        T defaultValue = getMetadata().getDefaultValue();
        boolean deepEquals = Objects.deepEquals(getValue(), defaultValue);

        if (deepEquals) {
            return false;
        }

        try {
            return !EqualsBuilder.reflectionEquals(getValue(), defaultValue);
        } catch (RuntimeException ignored) {
            // Reflection equals does not always work, use deepEquals instead of assuming no change
            // Since deepEquals is already false when we reach this, we can assume change
            return true;
        }
    }

    void restoreValue(Object value) {
        setValue((T) value);
    }

    public Type getType() {
        return getMetadata().getValueType();
    }

    public String getFieldName() {
        return getMetadata().getFieldName();
    }

    public Configurable getParent() {
        return (Configurable) getMetadata().getOwner();
    }

    private String getI18nKeyOverride() {
        return getMetadata().getI18nKeyOverride();
    }

    public boolean isEnum() {
        return getMetadata().getValueType() instanceof Class<?> clazz && clazz.isEnum();
    }

    public T getDefaultValue() {
        return getMetadata().getDefaultValue();
    }

    private PersistedMetadata<T> getMetadata() {
        return Managers.Persisted.getMetadata(this);
    }

    public String getDisplayName() {
        return getI18n(".name");
    }

    public String getDescription() {
        return getI18n(".description");
    }

    private String getI18n(String suffix) {
        if (!getI18nKeyOverride().isEmpty()) {
            return I18n.get(getI18nKeyOverride() + suffix);
        }
        return ((Translatable) getParent()).getTranslation(getFieldName() + suffix);
    }

    public Stream<String> getValidLiterals() {
        if (isEnum()) {
            return EnumUtils.getEnumConstants((Class<?>) getType()).stream().map(EnumUtils::toJsonFormat);
        }
        if (getType().equals(Boolean.class)) {
            return Stream.of("true", "false");
        }
        return Stream.of();
    }

    public String getValueString() {
        if (value == null) return "(null)";

        if (isEnum()) {
            return EnumUtils.toNiceString((Enum<?>) value);
        }

        return value.toString();
    }

    public <E extends Enum<E>> T tryParseStringValue(String value) {
        if (isEnum()) {
            return (T) EnumUtils.fromJsonFormat((Class<E>) getType(), value);
        }

        try {
            Class<?> wrapped = ClassUtils.primitiveToWrapper(((Class<?>) getType()));
            return (T) wrapped.getConstructor(String.class).newInstance(value);
        } catch (Exception ignored) {
        }

        // couldn't parse value
        return null;
    }
}
