package com.wynntils.model.events;

import com.wynntils.model.states.World;
import net.minecraftforge.fml.common.eventhandler.Event;

public class WorldEvent extends Event {

    public static class Join extends WorldEvent {
        private final World world;

        public Join(World world) {
            this.world = world;
        }

        public World getWorld() {
            return world;
        }
    }

    public static class Leave extends WorldEvent {
    }
}
