/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.persisted.storage;

import com.wynntils.core.components.Managers;
import com.wynntils.core.persisted.PersistedValue;
import java.lang.reflect.Type;

public class Storage<T> extends PersistedValue<T> {
    public Storage(T value) {
        super(value);
    }

    @Override
    public void touched() {
        Managers.Storage.persist();
    }

    @Override
    public Type getType() {
        return Managers.Storage.getType(this);
    }
}
