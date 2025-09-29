/*
 * Copyright © Wynntils 2022-2025.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.chat;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.type.NpcDialogueType;
import com.wynntils.mc.event.MobEffectEvent;
import com.wynntils.utils.mc.McUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * The responsibility of this class is to act as the first gateway for incoming
 * chat messages from Wynncraft. Chat messages in vanilla comes in three types,
 * CHAT, SYSTEM and GAME_INFO. The latter is the "action bar", and is handled
 * elsewhere. However, starting with Minecraft 1.19, Wynncraft will send all chat
 * messages as SYSTEM, so we will ignore the CHAT type.
 * <p>
 * Using the regexp patterns in RecipientType, we classify the incoming messages
 * according to if they are sent to the guild, party, global chat, etc. Messages
 * that do not match any of these categories are called "info" messages, and are
 * typically automated responses or announcements. Messages that do match any other
 * category, are sent by other users (what could really be termed "chat"). The one
 * exception is guild messages, which can also be e.g. WAR announcements.
 * (Unfortunately, there is no way to distinguish these from chat sent by a build
 * member named "WAR", or "INFO", or..., so if these need to be separated, it has
 * to happen in a later stage).
 * <p>
 * The final problem this class needs to resolve is how Wynncraft handles NPC
 * dialogs. When you enter a NPC dialog, Wynncraft start sending "screens" once a
 * second or so, which is multi-line messages that repeat the chat history, and add
 * the NPC dialog at the end. This way, the vanilla client will always show the NPC
 * dialog, so it is a clever hack in that respect. But it makes our life harder. We
 * solve this by detecting when a multiline "screen" happens, look for the last
 * real chat message we received, and splits of the rest as the "newLines". These
 * are in turn examined, since they can contain the actual NPC dialog, or they can
 * contain new chat messages sent while the user is in the NPC dialog.
 * <p>
 * These new chat messages are the real problematic thing here. They are
 * differently formatted to be gray and tuned-down, which makes the normal regexp
 * matching fail. They are also sent as pure strings with formatting codes, instead
 * of Components as normal one-line chats are. This mean things like hover and
 * onClick information is lost. (There is nothing we can do about this, it is a
 * Wynncraft limitation.) We send out these chat messages one by one, as they would
 * have appeared if we were not in a NPC dialog, but we tag them as BACKGROUND to
 * signal that formatting is different.
 * <p>
 * In a normal vanilla setting, the last "screen" that Wynncraft sends out, the
 * messages are re-colored to have their normal colors restored (hover and onClick
 * as still missing, though). Currently, we do not handle this, since it would mean
 * sending out information that already sent chat lines would need to be updated to
 * a different formatting. This could be done, but requires extra logic, and most
 * importantly, a way to update already printed chat lines.
 */
public final class ChatPageProcessor {
    // Test in ChatHandler_NPC_CONFIRM_PATTERN
    private static final Pattern NPC_CONFIRM_PATTERN =
            Pattern.compile("^ *§[47]Press §[cf](SNEAK|SHIFT) §[47]to continue$");

    // Test in ChatHandler_NPC_SELECT_PATTERN
    private static final Pattern NPC_SELECT_PATTERN =
            Pattern.compile("^ *§[47cf](Select|CLICK) §[47cf]an option (§[47])?to continue$");

    private static final Pattern EMPTY_LINE_PATTERN = Pattern.compile("^\\s*(§r|À+)?\\s*$");
    private static final long SLOWDOWN_PACKET_TICK_DELAY = 20;
    private static final int CHAT_SCREEN_TICK_DELAY = 1;

    private String lastRealChat = null;

    // This is used to detect when the lastRealChat message
    // is actually a confirmationless dialogue, but not a standard one,
    // and we can't parse it properly. That makes it be the last "real" chat,
    // so when we receive the clear screen, we think that all the messages are new.
    // By keeping track of the last two real chats, we can detect this case.
    private String oneBeforeLastRealChat = null;

    private long lastSlowdownApplied = 0;
    private List<StyledText> lastScreenNpcDialogue = List.of();
    private StyledText lastConfirmationlessDialogue = null;
    private List<StyledText> delayedDialogue;
    private NpcDialogueType delayedType;
    private long chatScreenTicks = 0;
    private List<StyledText> collectedLines = new ArrayList<>();

    public void reset() {
        // Reset chat handler
        collectedLines = new ArrayList<>();
        chatScreenTicks = 0;
        lastRealChat = null;
        oneBeforeLastRealChat = null;
        lastSlowdownApplied = 0;
        lastScreenNpcDialogue = List.of();
        lastConfirmationlessDialogue = null;
        delayedDialogue = null;
        delayedType = NpcDialogueType.NONE;
    }

    @SubscribeEvent
    public void onStatusEffectUpdate(MobEffectEvent.Update event) {
        if (event.getEntity() != McUtils.player()) return;

        if (event.getEffect().equals(MobEffects.MOVEMENT_SLOWDOWN.value())
                && event.getEffectAmplifier() == 3
                && event.getEffectDurationTicks() == 32767) {
            if (delayedDialogue != null) {
                List<StyledText> dialogue = delayedDialogue;
                delayedDialogue = null;

                handleNpcDialogue(dialogue, delayedType, true);
            } else {
                lastSlowdownApplied = McUtils.mc().level.getGameTime();
            }
        }
    }

    @SubscribeEvent
    public void onStatusEffectRemove(MobEffectEvent.Remove event) {
        if (event.getEntity() != McUtils.player()) return;

        if (event.getEffect().equals(MobEffects.MOVEMENT_SLOWDOWN.value())) {
            lastSlowdownApplied = 0;
        }
    }

    public boolean hasSlowdown() {
        return lastSlowdownApplied != 0;
    }

    public void handleEndOfDialogue() {
        // No new lines has appeared since last registered chat line.
        // We could just have a dialog that disappeared, so we must signal this
        handleNpcDialogue(List.of(), NpcDialogueType.NONE, false);
    }

    private void processNewLines(LinkedList<StyledText> newLines, boolean expectedConfirmationlessDialogue) {
        // We have new lines added to the bottom of the chat screen. They are either a dialogue,
        // or new background chat messages. Separate them in two parts
        LinkedList<StyledText> newChatLines = new LinkedList<>();
        LinkedList<StyledText> dialogue = new LinkedList<>();

        StyledText firstText = newLines.getFirst();
        boolean isNpcConfirm = firstText.find(NPC_CONFIRM_PATTERN);
        boolean isNpcSelect = firstText.find(NPC_SELECT_PATTERN);

        if (isNpcConfirm || isNpcSelect) {
            // This is an NPC dialogue screen.
            // First remove the "Press SHIFT/Select an option to continue" trailer.
            newLines.removeFirst();

            // If this happens, the "Press SHIFT/Select an option to continue" got appended to the last dialogue
            // NOTE: Currently, we do nothing in this case, as it seems to work without any issues
            //       In the future, additional handling for converting temporary confirmationless dialogues
            //       to normal dialogues may be needed
            if (newLines.isEmpty()) {
                WynntilsMod.info("[NPC] - Control message appended to the last dialogue");
                return;
            }

            if (newLines.getFirst().getString().isEmpty()) {
                // After this we assume a blank line
                newLines.removeFirst();
            } else {
                WynntilsMod.warn("Malformed dialog [#1]: " + newLines.getFirst());
            }

            boolean dialogDone = false;
            // This need to be false if we are to look for options
            boolean optionsFound = !isNpcSelect;

            // Separate the dialog part from any potential new "real" chat lines
            for (StyledText line : newLines) {
                if (!dialogDone) {
                    if (line.find(EMPTY_LINE_PATTERN)) {
                        if (!optionsFound) {
                            // First part of the dialogue found
                            optionsFound = true;
                            dialogue.push(line);
                        } else {
                            dialogDone = true;
                        }
                        // Intentionally throw away this line
                    } else {
                        dialogue.push(line);
                    }
                } else {
                    // If there is anything after the dialogue, it is new chat lines
                    if (!line.find(EMPTY_LINE_PATTERN)) {
                        newChatLines.push(line);
                    }
                }
            }
        } else if (expectedConfirmationlessDialogue) {
            if (newLines.size() != 1) {
                WynntilsMod.warn("New lines has an unexpected dialogue count [#1]: " + newLines);
            }

            // This is a confirmationless dialogue
            handleNpcDialogue(List.of(newLines.getFirst()), NpcDialogueType.CONFIRMATIONLESS, false);

            // If we expect a confirmationless dialogue, we should only have one line,
            // so we don't have to do any separation logic
            return;
        } else {
            // After a NPC dialogue screen, Wynncraft sends a "clear screen" with line of ÀÀÀ...
            // We just ignore that part. Also, remove empty lines or lines with just the §r code
            while (!newLines.isEmpty() && newLines.getFirst().find(EMPTY_LINE_PATTERN)) {
                newLines.removeFirst();
            }

            // But we may also handle new messages during the NPC dialogue screen here
            // If so, we need to separate the repeated dialogue and the new chat lines
            // The repeated dialogue starts with an empty line, followed by the actual dialogue

            // Reverse back the list, so it's in the order it was received
            Collections.reverse(newLines);

            // Add the lines to the new chat lines, until we find an empty line
            // If an empty line is found, check to see if it's followed by
            // either a confirmationless or a normal dialogue
            // If so, the rest of the lines are dialogues, so ignore them
            // If not, continue adding the lines to the new chat lines, and check for empty lines again,
            // if any are found
            while (!newLines.isEmpty()) {
                StyledText line = newLines.removeFirst();
                if (line.find(EMPTY_LINE_PATTERN)) {
                    if (newLines.isEmpty()) {
                        // If there are no more lines, we can't do anything
                        break;
                    }

                    StyledText nextLine = newLines.getFirst();
                    if (nextLine.equals(lastConfirmationlessDialogue)) {
                        // The rest of the lines is a re-sent confirmationless dialogue
                        if (newLines.size() > 1) {
                            // There should not be any more lines after this
                            WynntilsMod.warn("Unexpected lines after a confirmationless dialogue: " + newLines);
                        }

                        break;
                    }

                    // Check if the following lines match the last NPC screen dialogue
                    // Otherwise, treat them as new chat lines
                    for (StyledText dialogueLine : lastScreenNpcDialogue) {
                        if (newLines.isEmpty()) {
                            // If there are no more lines, we can't do anything
                            break;
                        }

                        StyledText nextDialogueLine = newLines.getFirst();
                        if (!nextDialogueLine.equals(dialogueLine)) {
                            // If the next line does not match the dialogue line, it's a new chat line
                            break;
                        }

                        // If the next line matches the dialogue line, remove it
                        newLines.removeFirst();
                    }

                    // If we have removed all the lines, we don't need to do anything more
                    if (newLines.isEmpty()) {
                        break;
                    }
                }

                // This was not found to be a dialogue line, so add it to the new chat lines
                newChatLines.addLast(line);
            }
        }

        // Register all new chat lines
        //   newChatLines.forEach(this::handleFakeChatLine);

        // Handle the dialogue, if any
        handleScreenNpcDialog(dialogue, isNpcSelect);
    }

    private void handleScreenNpcDialog(List<StyledText> dialogues, boolean isSelection) {
        if (dialogues.isEmpty()) {
            // dialog could be the empty list, this means the last dialog is removed
            handleNpcDialogue(dialogues, NpcDialogueType.NONE, false);
            return;
        }

        NpcDialogueType type = isSelection ? NpcDialogueType.SELECTION : NpcDialogueType.NORMAL;

        if (McUtils.mc().level.getGameTime() <= lastSlowdownApplied + SLOWDOWN_PACKET_TICK_DELAY) {
            // This is a "protected" dialogue if we have gotten slowdown effect just prior to the chat message
            // This is the normal case
            handleNpcDialogue(dialogues, type, true);
            return;
        }

        // Maybe this should be a protected dialogue but packets came in the wrong order.
        // Wait a tick for slowdown, and then send the event
        delayedDialogue = dialogues;
        delayedType = type;
        Managers.TickScheduler.scheduleNextTick(() -> {
            if (delayedDialogue != null) {
                List<StyledText> dialogToSend = delayedDialogue;
                delayedDialogue = null;
                // If we got here, then we did not get the slowdown effect, otherwise we would
                // have sent the dialogue already
                handleNpcDialogue(dialogToSend, delayedType, false);
            }
        });
    }

    private void handleNpcDialogue(List<StyledText> dialogue, NpcDialogueType type, boolean isProtected) {
        if (type == NpcDialogueType.NONE) {
            // Ignore any delayed dialogues, since they are now obsolete
            delayedDialogue = null;
        }

        // Confirmationless dialogues bypass the lastScreenNpcDialogue check
        if (type == NpcDialogueType.CONFIRMATIONLESS) {
            if (dialogue.size() != 1) {
                WynntilsMod.warn("Confirmationless dialogues should only have one line: " + dialogue);
            }

            // Store the last confirmationless dialogue, but it may be repeated,
            // so we need to check that it's not duplicated when a message is sent during the dialogue
            lastConfirmationlessDialogue = dialogue.getFirst();
        } else {
            if (lastScreenNpcDialogue.equals(dialogue)) return;

            lastScreenNpcDialogue = dialogue;
        }

        Models.NpcDialogue.handleDialogue(dialogue, isProtected, type);
    }
}
