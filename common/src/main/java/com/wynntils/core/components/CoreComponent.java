/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.components;

import com.wynntils.core.persisted.storage.Storageable;

public abstract class CoreComponent implements Storageable {
    protected abstract String getComponentType();
}
