package com.wynntils.model.updater;

import com.wynntils.Reference;
import com.wynntils.core.events.custom.PacketEvent;
import com.wynntils.core.framework.enums.ClassType;
import com.wynntils.core.framework.instances.PlayerInfo;
import com.wynntils.core.utils.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerStatsUpdater {
    private String lastActionBar;
    private String specialActionBar = null;
    private static final Pattern actionbarPattern = Pattern.compile("(?:§❤ *([0-9]+)/([0-9]+))?.*? {2,}(?:§([LR])§-(?:§([LR])§-§([LR])?)?)?.*".replace("§", "(?:§[0-9a-fklmnor])*"));
    private static final boolean[] noSpell = new boolean[0];
    /** Represents `L` in the currently casting spell */
    public static final boolean SPELL_LEFT = false;
    /** Represents `R` in the currently casting spell */
    public static final boolean SPELL_RIGHT = true;

    private static final int[] xpNeeded = new int[] {110,190,275,385,505,645,790,940,1100,1370,1570,1800,2090,2400,2720,3100,3600,4150,4800,5300,5900,6750,7750,8900,10200,11650,13300,15200,17150,19600,22100,24900,28000,31500,35500,39900,44700,50000,55800,62000,68800,76400,84700,93800,103800,114800,126800,140000,154500,170300,187600,206500,227000,249500,274000,300500,329500,361000,395000,432200,472300,515800,562800,613700,668600,728000,792000,860000,935000,1040400,1154400,1282600,1414800,1567500,1730400,1837000,1954800,2077600,2194400,2325600,2455000,2645000,2845000,3141100,3404710,3782160,4151400,4604100,5057300,5533840,6087120,6685120,7352800,8080800,8725600,9578400,10545600,11585600,12740000,14418250,16280000,21196500,23315500,25649000,249232940};

    private int health = -1;
    private int maxHealth = -1;
    private boolean[] lastSpell = noSpell;

    private int level = -1;
    private float experiencePercentage = -1;

    public void updateActionBar(String actionBar) {
   //     if (currentClass == ClassType.NONE) return;

        // Actionbar gives us: [health], max health, [mana]
        // weapon powder effect (if available)
        // sprint status (if available)

        // spell progress

        // what if both sprinting and powder?
        // sprint overrides, goes directly back to powder (if not, it's lost)

        // and what is priority of spell progress?
        // get mana from hunger instead?
        // how does this health differ from                     lastHealth = Minecraft.getMinecraft().player.getHealth();????
        // Avoid useless processing
        if (this.lastActionBar == null || !this.lastActionBar.equals(actionBar)) {
            this.lastActionBar = actionBar;

            if (actionBar.contains("|") || actionBar.contains("_")) {
                specialActionBar = StringUtils.getCutString(actionBar, "    ", "    " + TextFormatting.AQUA, false);
            } else {
                specialActionBar = null;
            }

            Matcher match = actionbarPattern.matcher(actionBar);

            if (match.matches()) {
                if (match.group(1) != null) {
                    this.health = Integer.parseInt(match.group(1));
                    this.maxHealth = Integer.parseInt(match.group(2));
                }

                if (match.group(3) != null) {
                    int size;
                    for (size = 1; size < 3; ++size) {
                        if (match.group(size + 3) == null) break;
                    }

                    lastSpell = new boolean[size];
                    for (int i = 0; i < size; ++i) {
                        lastSpell[i] = match.group(i + 3).charAt(0) == 'R' ? SPELL_RIGHT : SPELL_LEFT;
                    }
                }
            }
        }

        this.level = Minecraft.getMinecraft().player.experienceLevel;
        this.experiencePercentage = Minecraft.getMinecraft().player.experience;
    }

    public int getCurrentMana() {  return Minecraft.getMinecraft().player.getFoodStats().getFoodLevel(); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdateActionBar(PacketEvent<SPacketChat> e) {
        if (!Reference.onServer || e.getPacket().getType() != ChatType.GAME_INFO) return;


        // chat type GAME_INFO == action bar
        updateActionBar(e.getPacket().getChatComponent().getUnformattedText());
        e.setCanceled(true);
    }

    /**
     * @return The maximum number of soul points the current player can have
     *
     * Note: If veteran, this should always be 15, but currently might return the wrong value
     */
    public int getMaxSoulPoints() {
        int maxIfNotVeteran = 10 + MathHelper.clamp(Minecraft.getMinecraft().player.experienceLevel / 15, 0, 5);
        if (getSoulPoints() > maxIfNotVeteran) {
            return 15;
        }
        return maxIfNotVeteran;
    }

    /**
     * @return The current number of soul points the current player has
     *
     * -1 if unable to determine
     */
    public int getSoulPoints() {
   //     if (currentClass == ClassType.NONE || mc.player == null) return -1;
        ItemStack soulPoints = Minecraft.getMinecraft().player.inventory.mainInventory.get(8);
        if (soulPoints.getItem() != Items.NETHER_STAR && soulPoints.getItem() != Item.getItemFromBlock(Blocks.SNOW_LAYER)) {
            return -1;
        }
        return soulPoints.getCount();
    }
    public int getXpNeededToLevelUp() {
        return xpNeeded[Minecraft.getMinecraft().player.experienceLevel - 1];
        /*
        // Quick fix for crash bug - more investigation to be done.
        try {
            if (mc.player != null
                    && mc.player.experienceLevel != 0
                    && currentClass != ClassType.NONE
                    && mc.player.experienceLevel <= xpNeeded.length
                    && lastLevel != mc.player.experienceLevel) {
                lastLevel = mc.player.experienceLevel;
                lastXp = xpNeeded[mc.player.experienceLevel - 1];
            }
            return currentClass == ClassType.NONE || (mc.player != null && (mc.player.experienceLevel == 0 || mc.player.experienceLevel > xpNeeded.length)) ? -1 : lastXp;
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return -1;
        }*/
    }

}


