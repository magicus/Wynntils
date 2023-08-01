/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings.widgets;

import com.wynntils.core.config.Config;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import java.util.List;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public class ResetButton extends GeneralSettingsButton {
    private final Config<?> configHolder;
    private final Runnable onClick;

    ResetButton(Config<?> configHolder, Runnable onClick, int x, int y) {
        super(
                x,
                y,
                35,
                12,
                Component.translatable("screens.wynntils.settingsScreen.reset.name"),
                List.of(Component.translatable("screens.wynntils.settingsScreen.reset.description")));
        this.configHolder = configHolder;
        this.onClick = onClick;
    }

    @Override
    protected CustomColor getTextColor() {
        return configHolder.getConfigHolder().valueChanged() ? CommonColors.WHITE : CommonColors.GRAY;
    }

    @Override
    protected CustomColor getBackgroundColor() {
        return configHolder.getConfigHolder().valueChanged() ? super.getBackgroundColor() : BACKGROUND_COLOR;
    }

    @Override
    public void playDownSound(SoundManager handler) {
        if (!configHolder.getConfigHolder().valueChanged()) return;
        super.playDownSound(handler);
    }

    @Override
    public void onPress() {
        if (!configHolder.getConfigHolder().valueChanged()) return;
        configHolder.getConfigHolder().reset();
        onClick.run();
    }
}
