/*
 * Copyright © Wynntils 2022-2025.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.chat;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Handler;
import com.wynntils.core.components.Models;
import com.wynntils.core.mod.event.WynncraftConnectionEvent;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageEvent;
import com.wynntils.handlers.chat.type.MessageType;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.mc.event.MobEffectEvent;
import com.wynntils.mc.event.SystemMessageEvent;
import com.wynntils.mc.event.TickEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class ChatHandlerNew extends Handler {
    private final ChatPageDetector pageDetector = new ChatPageDetector();
    private final ChatPageProcessor pageProcessor = new ChatPageProcessor();

    @SubscribeEvent
    public void onConnectionChange(WynncraftConnectionEvent.Connected event) {
        pageDetector.reset();
        pageProcessor.reset();
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        pageDetector.onTick();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSystemChatReceived(SystemMessageEvent.ChatReceivedEvent event) {
        if (needPageDetector()) {
            pageDetector.handleIncomingChatMessage(event);
        } else {
            handleIncomingChatMessage(event);
        }
    }

    @SubscribeEvent
    public void onStatusEffectUpdate(MobEffectEvent.Update event) {
        pageProcessor.onStatusEffectUpdate(event);
    }

    @SubscribeEvent
    public void onStatusEffectRemove(MobEffectEvent.Remove event) {
        pageProcessor.onStatusEffectRemove(event);
    }

    /**
     * Callback from ChatPageDetector when a page is detected.
     */
    void handlePage(List<StyledText> pageContent, boolean lastPage) {
        pageProcessor.handlePage(pageContent, lastPage);
    }

    /**
     * Callback from ChatPageDetector when chat message needs to be sent with a delay due to analysis reasons
     */
    void sendDelayedChat(Component msg) {
        StyledText message = StyledText.fromComponent(msg);
        StyledText newMessage = processChatMessage(message, MessageType.FOREGROUND);

        if (newMessage == null) return;

        // Send it without triggering any new events
        McUtils.mc().gui.getChat().addMessage(newMessage.getComponent());
    }

    /**
     * Callback from ChatPageDetector when background chat message is detected
     */
    void handleBackgroundLine(StyledText message) {
        StyledText newMessage = processChatMessage(message, MessageType.BACKGROUND);

        if (newMessage == null) return;

        // Send it without triggering any new events
        McUtils.mc().gui.getChat().addMessage(newMessage.getComponent());
    }

    private void handleIncomingChatMessage(SystemMessageEvent.ChatReceivedEvent event) {
        StyledText message = StyledText.fromComponent(event.getMessage());
        StyledText newMessage = processChatMessage(message, MessageType.FOREGROUND);

        if (newMessage == null) {
            event.setCanceled(true);
            return;
        }
        if (!newMessage.equals(message)) {
            event.setMessage(newMessage.getComponent());
        }
    }

    private StyledText processChatMessage(StyledText message, MessageType messageType) {
        // All chat messages will pass through this method, one way or another
        RecipientType recipientType = getRecipientType(message, messageType);

        // Normally § codes are stripped from the log; need this to be able to debug chat formatting
        WynntilsMod.info("[CHAT/" + recipientType + "] " + message.getString().replace("§", "&"));

        ChatMessageEvent.Match receivedEvent = new ChatMessageEvent.Match(message, messageType, recipientType);
        WynntilsMod.postEvent(receivedEvent);
        if (receivedEvent.isCanceled()) return null;

        ChatMessageEvent.Edit rewriteEvent = new ChatMessageEvent.Edit(message, messageType, recipientType);
        WynntilsMod.postEvent(rewriteEvent);
        return rewriteEvent.getMessage();
    }

    private RecipientType getRecipientType(StyledText codedMessage, MessageType messageType) {
        // Check if message match a recipient category
        for (RecipientType recipientType : RecipientType.values()) {
            if (recipientType.matchPattern(codedMessage, messageType)) {
                return recipientType;
            }
        }

        // If no specific recipient matched, it is an "info" message
        return RecipientType.INFO;
    }

    private boolean needPageDetector() {
        // This is still a bit wonky...
        return Models.NpcDialogue.isNpcDialogExtractionRequired();
    }
}
