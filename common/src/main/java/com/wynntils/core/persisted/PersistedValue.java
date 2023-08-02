/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted;

import com.wynntils.core.components.Managers;

public abstract class PersistedValue<T> implements Comparable<PersistedValue<T>> {
    protected T value;

    protected PersistedValue(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public abstract void touched();

    public void store(T value) {
        this.value = value;
        touched();
    }

    @SuppressWarnings("unchecked")
    void setRaw(Object value) {
        this.value = (T) value;
    }

    public String getJsonName() {
        // Available after owner is registered in registerOwner()
        return Managers.Persisted.getMetadata(this).getJsonName();
    }

    @Override
    public int compareTo(PersistedValue<T> other) {
        return getJsonName().compareTo(other.getJsonName());
    }
}
