/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.overlays;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.core.features.overlays.OverlayPosition;
import com.wynntils.core.features.overlays.annotations.OverlayInfo;
import com.wynntils.core.features.overlays.sizes.GuiScaledOverlaySize;
import com.wynntils.core.features.properties.FeatureCategory;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.handlers.item.event.ItemRenamedEvent;
import com.wynntils.mc.event.RenderEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.spells.event.SpellEvent;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(category = FeatureCategory.OVERLAYS)
public class SpellCastRenderFeature extends UserFeature {
    private static final int SHOW_TICKS = 40;
    public static final int FADE_TICKS = 4;

    @OverlayInfo(renderType = RenderEvent.ElementType.GUI)
    public Overlay spellCastOverlay = new SpellCastOverlay();

    @Config
    public boolean renderVignette = false;

    @Config
    public float vignetteIntensity = 1.3f;

    @Config
    public CustomColor vignetteColor = new CustomColor(0, 0, 255);

    private int spellTimer;
    private String spellMessage;
    private float intensity;

    @SubscribeEvent
    public void onItemRename(ItemRenamedEvent event) {
        ItemStack itemStack = event.getItemStack();
        Optional<GearItem> gearItemOpt = Models.Item.asWynnItem(itemStack, GearItem.class);
        if (gearItemOpt.isEmpty()) return;

        GearItem gearItem = gearItemOpt.get();
        if (!gearItem.getGearInfo().type().isWeapon()) return;

        // Hide vanilla item rename popup
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onSpellCast(SpellEvent.Cast event) {
        int manaCost = event.getManaCost();
        spellMessage = "§7" + event.getSpellType().getName() + " spell cast! §3[§b-" + manaCost + " ✺§3]";

        // An relativeCost of 1.0 means we just used all mana we have left
        float relativeCost = (float) manaCost / Models.Character.getCurrentMana();
        intensity = vignetteIntensity * relativeCost;
        spellTimer = SHOW_TICKS;
    }

    @SubscribeEvent
    public void onSpellFailed(SpellEvent.Failed event) {
        spellMessage = event.getFailureReason().getMessage();
        intensity = 0f;
        spellTimer = SHOW_TICKS;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (spellTimer <= 0) return;

        spellTimer--;
    }

    @SubscribeEvent
    public void onRender(RenderEvent.Post event) {
        if (!renderVignette || intensity <= 0f) return;

        int shownTicks = SHOW_TICKS - spellTimer;
        int fade = FADE_TICKS - shownTicks;
        if (fade > 0) {
            float alpha = intensity * ((float) fade / FADE_TICKS);
            RenderUtils.renderVignetteOverlay(event.getPoseStack(), vignetteColor, alpha);
        }
    }

    public class SpellCastOverlay extends Overlay {
        protected SpellCastOverlay() {
            super(
                    new OverlayPosition(
                            -83,
                            0,
                            VerticalAlignment.Bottom,
                            HorizontalAlignment.Center,
                            OverlayPosition.AnchorSection.BottomMiddle),
                    new GuiScaledOverlaySize(200, 20),
                    HorizontalAlignment.Center,
                    VerticalAlignment.Bottom);
        }

        @Override
        public void render(
                PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, float partialTicks, Window window) {
            if (spellTimer <= 0) return;

            // Render it the same way vanilla renders item changes
            int alpha = (int) Math.min((float) spellTimer * 256.0F / 10.0F, 255.0F);
            if (alpha <= 0) return;

            BufferedFontRenderer.getInstance()
                    .renderAlignedTextInBox(
                            poseStack,
                            bufferSource,
                            spellMessage,
                            this.getRenderX(),
                            this.getRenderX() + this.getWidth(),
                            this.getRenderY(),
                            this.getRenderY() + this.getHeight(),
                            this.getWidth(),
                            CommonColors.WHITE.withAlpha(alpha),
                            this.getRenderHorizontalAlignment(),
                            this.getRenderVerticalAlignment(),
                            TextShadow.NORMAL);
        }

        @Override
        protected void onConfigUpdate(ConfigHolder configHolder) {}
    }
}
