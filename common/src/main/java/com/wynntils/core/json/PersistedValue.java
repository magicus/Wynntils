/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.json;

public abstract class PersistedValue<T> {
    protected T value;

    public PersistedValue(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void store(T value) {
        this.value = value;
        touched();
    }

    public abstract void touched();
}
