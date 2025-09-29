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
import com.wynntils.handlers.chat.type.NpcDialogueType;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.mc.event.MobEffectEvent;
import com.wynntils.mc.event.SystemMessageEvent;
import com.wynntils.mc.event.TickEvent;
import java.util.List;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class ChatHandlerNew extends Handler {
    ChatPageDetector pageDetector = new ChatPageDetector();
    ChatPageProcessor pageProcessor = new ChatPageProcessor();

    @SubscribeEvent
    public void onConnectionChange(WynncraftConnectionEvent.Connected event) {
        if (needPageDetector()) {
            pageDetector.reset();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (needPageDetector()) {
            pageDetector.onTick();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSystemChatReceived(SystemMessageEvent.ChatReceivedEvent event) {
        if (needPageDetector()) {
            pageDetector.handleIncomingChatLine(event);
        } else {
            handleIncomingChatLine(event);
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
     * Callback from ChatPageDetector when a page is found.
     */
    protected void handlePage(List<StyledText> pageContent, boolean lastPage) {
        pageProcessor.handlePage(pageContent, lastPage);
    }

    private void handleIncomingChatLine(SystemMessageEvent.ChatReceivedEvent event) {
        StyledText styledText = StyledText.fromComponent(event.getMessage());

        // This is a normal one line chat, or we pass a chat screen through
        StyledText updatedMessage = postChatLine(styledText, MessageType.FOREGROUND);

        if (updatedMessage == null) {
            event.setCanceled(true);
        } else if (!updatedMessage.equals(styledText)) {
            event.setMessage(updatedMessage.getComponent());
        }
    }

    /**
     * Return a "massaged" version of the message, or null if we should cancel the
     * message entirely.
     */
    private StyledText postChatLine(StyledText styledText, MessageType messageType) {
        String plainText = styledText.getStringWithoutFormatting();

        // Normally § codes are stripped from the log; need this to be able to debug chat formatting
        WynntilsMod.info("[CHAT] " + styledText.getString().replace("§", "&"));
        RecipientType recipientType = getRecipientType(styledText, messageType);

        if (recipientType == RecipientType.NPC) {
            if (needPageDetector()) {
                handleNpcDialogue(List.of(styledText), NpcDialogueType.CONFIRMATIONLESS, false);
                // We need to cancel the original chat event, if any
                return null;
            } else {
                // Reclassify this as a INFO type for the chat
                recipientType = RecipientType.INFO;
            }
        }

        ChatMessageEvent.Match receivedEvent = new ChatMessageEvent.Match(styledText, messageType, recipientType);
        WynntilsMod.postEvent(receivedEvent);
        if (receivedEvent.isCanceled()) return null;

        ChatMessageEvent.Edit rewriteEvent = new ChatMessageEvent.Edit(styledText, messageType, recipientType);
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

    private void handleNpcDialogue(List<StyledText> dialogue, NpcDialogueType type, boolean isProtected) {
        Models.NpcDialogue.handleDialogue(dialogue, isProtected, type);
    }

    private boolean needPageDetector() {
        return Models.NpcDialogue.isNpcDialogExtractionRequired();
    }
}
