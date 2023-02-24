/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.mobtotem;

import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.handlers.labels.event.EntityLabelChangedEvent;
import com.wynntils.mc.event.RemoveEntitiesEvent;
import com.wynntils.models.worlds.WorldStateModel;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.type.Location;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MobTotemModel extends Model {
    private static final Pattern MOB_TOTEM_NAME = Pattern.compile("^§f§l(.*)'s§6§l Mob Totem$");
    private static final Pattern MOB_TOTEM_TIMER = Pattern.compile("^§c§l([0-9]+:[0-9]+)$");
    private static final double TOTEM_COORDINATE_DIFFERENCE = 0.2d;

    private final HashMap<Integer, MobTotem> mobTotems = new HashMap<>();

    public MobTotemModel(WorldStateModel worldState) {
        super(List.of(worldState));
    }

    @SubscribeEvent
    public void onTotemRename(EntityLabelChangedEvent e) {
        if (!Models.WorldState.onWorld()) return;

        Entity entity = e.getEntity();
        if (!(entity instanceof ArmorStand as)) return;

        // If a new mob totem just appeared, add it to the unstarted list
        // Totem timers do not match the MOB_TOTEM_NAME pattern
        Matcher nameMatcher = MOB_TOTEM_NAME.matcher(e.getName());
        if (nameMatcher.find()) {
            int mobTotemId = e.getEntity().getId();

            if (mobTotems.containsKey(mobTotemId)) return; // If the totem is already in the list, don't add it again

            mobTotems.put(mobTotemId, new MobTotem(new Location(as), nameMatcher.group(1)));
            return;
        }

        Matcher timerMatcher = MOB_TOTEM_TIMER.matcher(e.getName());
        if (!timerMatcher.find()) return;

        mobTotems.values().stream()
                .filter(
                        // Exact equality is fine here because the totem is stationary
                        mobTotem -> as.getX() == mobTotem.getLocation().x()
                                && as.getY() == (mobTotem.getLocation().y() + TOTEM_COORDINATE_DIFFERENCE)
                                && as.getZ() == mobTotem.getLocation().z())
                .forEach(mobTotem -> mobTotem.setTimerString(timerMatcher.group(1)));
    }

    @SubscribeEvent
    public void onTotemDestroy(RemoveEntitiesEvent e) {
        if (!Models.WorldState.onWorld()) return;

        e.getEntityIds().forEach(mobTotems::remove);
    }

    public List<MobTotem> getMobTotems() {
        return mobTotems.values().stream().toList();
    }

    public static class MobTotem {
        private final Location location;
        private final String owner;
        private String timerString;

        public MobTotem(Location location, String owner) {
            this.location = location;
            this.owner = owner;
        }

        public Location getLocation() {
            return location;
        }

        public String getOwner() {
            return owner;
        }

        public String getTimerString() {
            return timerString;
        }

        public void setTimerString(String timerString) {
            this.timerString = timerString;
        }

        public int getDistanceToPlayer() {
            // y() - 4 because the entity is 4 blocks above the ground
            return (int)
                    Math.sqrt(McUtils.player().distanceToSqr(new Vec3(location.x(), location.y() - 4, location.z())));
        }

        public double getLookAngleDiff() {
            Vec3 playerLook = McUtils.player().getLookAngle();
            double lookAngle = Math.toDegrees(StrictMath.atan2(playerLook.z, playerLook.x));

            Position mobTotemLocation = this.getLocation();
            double angle = Math.toDegrees(StrictMath.atan2(
                    mobTotemLocation.z() - McUtils.player().getZ(),
                    mobTotemLocation.x() - McUtils.player().getX()));

            double angleDiff = lookAngle - angle;
            if (angleDiff < 0) angleDiff = 360 + angleDiff; // Convert negative angles to positive
            // Anglediff is now the angle between the player and the mob totem
            // 0 is straight ahead, 90 is to the left, 180 is behind, 270 is to the right

            return angleDiff;
        }
    }
}