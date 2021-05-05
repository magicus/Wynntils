package com.wynntils.model.updater;

import com.wynntils.core.events.custom.GuiOverlapEvent;
import com.wynntils.core.utils.ItemUtils;
import com.wynntils.model.Wynn;
import com.wynntils.model.WynnImpl;
import com.wynntils.model.events.WorldEvent;
import com.wynntils.model.states.PlayerClass;
import com.wynntils.model.types.ClassType;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClassTypeUpdater {
    private static boolean classChangeRequested = false;
    private ClassType currentClass = ClassType.NONE;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChat(ClientChatEvent e) {
        if (Wynn.getInstance().onWorld() && e.getMessage().startsWith("/class")) {
            classChangeRequested = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void receiveTp(GuiScreenEvent.DrawScreenEvent.Post e) {
        if (classChangeRequested) {
            WynnImpl.getInstance().setPlayerClass(PlayerClass.NONE);
            classChangeRequested = false;
        }
    }

    /**
     * Detects the user class based on the class selection GUI
     * This detection happens when the user click on an item that contains the class name pattern, inside the class selection GUI
     *
     * @param e Represents the click event
     */
    @SubscribeEvent
    public void changeClass(GuiOverlapEvent.ChestOverlap.HandleMouseClick e) {
        if (!e.getGui().getLowerInv().getName().contains("Select a Class")) return;

        if (e.getMouseButton() != 0
                || e.getSlotIn() == null
                || !e.getSlotIn().getHasStack()
                || !e.getSlotIn().getStack().hasDisplayName()
                || !e.getSlotIn().getStack().getDisplayName().contains("[>] Select")) return;

        // Now we know an actual class was selected
        int classId = e.getSlotId();

        String classLore = ItemUtils.getLore(e.getSlotIn().getStack()).get(1);
        String classString = classLore.substring(classLore.indexOf(TextFormatting.WHITE.toString()) + 2);

        ClassType selectedClass = ClassType.fromString(classString);

        WynnImpl.getInstance().setPlayerClass(PlayerClass.of(selectedClass, classId));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldJoin(WorldEvent.Join e) {
        WynnImpl.getInstance().setPlayerClass(WynnImpl.getInstance().getSavedPlayerClass());
    }

    @SubscribeEvent
    public void onWorldLeave(WorldEvent.Leave e) {
        WynnImpl.getInstance().setPlayerClass(PlayerClass.NONE);
    }
}
