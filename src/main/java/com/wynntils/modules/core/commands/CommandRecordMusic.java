/*
 *  * Copyright © Wynntils - 2018 - 2020.
 */

package com.wynntils.modules.core.commands;

import com.wynntils.core.events.custom.ClientEvent;
import com.wynntils.modules.core.events.ClientEvents;
import com.wynntils.webapi.WebManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.IClientCommand;

public class CommandRecordMusic extends CommandBase implements IClientCommand {

    @Override
    public boolean allowUsageWithoutPrefix(ICommandSender sender, String message) {
        return false;
    }

    @Override
    public String getName() {
        return "record";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "Record music";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        String song = args[0];

        if (song == null ||  song.isEmpty()) {
            throw new CommandException("Must specify song nane!");
        }
        if (song.equals("stop")) {
            ClientEvents.stopRecording();
            return;
        }
        TextComponentString text = new TextComponentString("Starting recording of " + song + "...");
        text.getStyle().setColor(TextFormatting.AQUA);
        sender.sendMessage(text);
        ClientEvents.startRecording(song);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
