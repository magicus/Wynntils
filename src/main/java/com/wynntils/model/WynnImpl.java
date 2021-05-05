package com.wynntils.model;

import com.wynntils.core.framework.settings.annotations.Setting;
import com.wynntils.core.framework.settings.instances.SettingsClass;
import com.wynntils.model.events.ClassTypeEvent;
import com.wynntils.model.events.WorldEvent;
import com.wynntils.model.events.WynncraftServerEvent;
import com.wynntils.model.states.PlayerClass;
import com.wynntils.model.states.World;
import com.wynntils.modules.core.CoreModule;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;

public class WynnImpl extends SettingsClass implements Wynn {
    protected final static WynnImpl instance = new WynnImpl();

    private boolean connected = false;
    private World world = World.NONE;
    private PlayerClass playerClass = PlayerClass.NONE;

    @Setting(upload = false)
    public PlayerClass lastPlayerClass = PlayerClass.NONE;

    public static WynnImpl getInstance() {
        return instance;
    }

    public void setConnected(boolean connected) {
        if (connected == this.connected) return;

        this.connected = connected;

        if (connected) {
            post(new WynncraftServerEvent.Join());
        } else {
            post(new WynncraftServerEvent.Leave());
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    public void setWorld(World world) {
        if (world == this.world) return;

        this.world = world;
        if (world != World.NONE) {
            post(new WorldEvent.Join(world));
        } else {
            post(new WorldEvent.Leave());
        }
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public boolean onWorld() {
        return getWorld() != World.NONE && getWorld() != World.LOBBY;
    }

    private void savePlayerClass(PlayerClass playerClass) {
        if (playerClass != PlayerClass.NONE) {
            lastPlayerClass = playerClass;
            saveSettings(CoreModule.getModule());
        }
    }

    public void setPlayerClass(PlayerClass playerClass) {
        if (playerClass == this.playerClass) return;

        this.playerClass = playerClass;

        if (playerClass != PlayerClass.NONE) {
            post(new ClassTypeEvent.Join(playerClass));
        } else {
            post(new ClassTypeEvent.Leave());
        }

        // Make it persistent
        savePlayerClass(playerClass);
    }

    public PlayerClass getSavedPlayerClass() {
        return lastPlayerClass;
    }

    @Override
    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    // Health, mana and xp are fluent, change all the time, no event at change,
    // only for max change

    // soul points and leve only change infrequently, and can trigger event at change.

    @Override
    public int getCurrentHealth() {
        // update by callback
        // NO EVENT -- to frequent? or should we have one?
        // can we calculate given max health and player.getHealth()?
        return 0;
    }

    @Override
    public int getMaxHealth() {
        // update by callback
        // should fire event when changed
        return 0;
    }

    @Override
    public int getCurrentMana() {
        // read mc variable
        // NO EVENT -- to frequent? or..?
        return 0;
    }

    @Override
    public int getMaxMana() {
        // constant
        // NO EVENT - constant
        return 0;
    }

    @Override
    public int getCurrentXP() {
        // read from mc variable
        // No event, too frequent -- or..?
        return 0;
    }

    @Override
    public int getMaxXP() {
        // dependent on level (updates when level updates)
        // should fire event when changed
        return 0;
    }


    @Override
    public int getCurrentSoulPoints() {
        // read from inventory, but could also be updated by callback when changed (?)
        // should fire event when changed
        return 0;
    }

    @Override
    public int getMaxSoulPoints() {
        // dependent on level (updates when level updates)
        // should fire event when changed
        return 0;
    }


    @Override
    public int getCurrentLevel() {
        // read from mc variable, but could also be updated by callback when changed
        // should fire event when changed
        return 0;
    }

    @Override
    public int getMaxLevel() {
        // constant
        // NO event, constant
        return 0;
    }

    public boolean post(Event event) {
        return MinecraftForge.EVENT_BUS.post(event);
    }
}
