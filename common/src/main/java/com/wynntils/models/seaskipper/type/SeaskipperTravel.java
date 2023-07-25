/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.seaskipper.type;

import com.wynntils.models.items.items.gui.SeaskipperDestinationItem;
import com.wynntils.models.map.pois.SeaskipperPoi;

public record SeaskipperTravel(SeaskipperDestinationItem destinationItem, SeaskipperPoi destinationPoi, int slot) {}
