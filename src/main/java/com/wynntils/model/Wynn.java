package com.wynntils.model;

import com.wynntils.model.states.PlayerClass;
import com.wynntils.model.states.World;

public interface Wynn {
    static Wynn getInstance() {
        return WynnImpl.instance;
    }

    // WORLD MANAGEMENT
    boolean isConnected();

    World getWorld();
    boolean onWorld();

    PlayerClass getPlayerClass();

    // STATS MANAGEMENT
    //
    // FIXME: Only valid if we have a class. Make sure we go via a getPlayer() or so...
    public int getCurrentHealth();
    public int getMaxHealth();

    public int getCurrentMana();
    public int getMaxMana();

    public int getCurrentSoulPoints();
    public int getMaxSoulPoints();

    public int getCurrentXP();
    public int getMaxXP();

    public int getCurrentLevel();
    public int getMaxLevel();


    /*
    WORLD / SERVER:
       private List<String> serverTypes = Lists.newArrayList("WC", "lobby", "GM", "DEV", "WAR", "HB");
titta i CommandServer efter server list.

server ping time. Uptime.

go to lobby.
join specific world from lobby.

WORLD KNOWLEDGE/LAYOUT:
locations
territories
services
NPCs

WARS:
wars stage events:
 public enum WarStage {
        WAITING, WAITING_FOR_TIMER, WAR_STARTING, WAITING_FOR_MOB_TIMER, WAITING_FOR_MOBS, IN_WAR
    }
each stage got a timer.
each war also got a territory where the war is.

    SOCIAL:
    get friend, guild, party list

STATS MANAGEMENT:
      public int getCurrentHealth();
    public int getMaxHealth();

    public int getCurrentMana();
    public int getMaxMana();

      public int getCurrentSoulPoints();
  public int getMaxSoulPoints();
        public int getTicksToNextSoulPoint() {

    public int getLevel() { return currentClass == ClassType.NONE ? -1 : level; }

 ---> den som visar totalt antal xp-poäng:
    public int getCurrentXP() { return currentClass == ClassType.NONE  || mc.player == null? -1 : (int)((getXpNeededToLevelUp()) * mc.player.experience); }
    public float getExperiencePercentage() { return currentClass == ClassType.NONE ? -1 : experiencePercentage; }
--> när current XP når deta börjar man om på 0 men en nivå högre upp
    public int getXpNeededToLevelUp() {
    --> hur många procent jag är på väg mot att levla up
      public String getCurrentXPAsPercentage() { return currentClass == ClassType.NONE || mc.player == null ? "" : perFormat.format(mc.player.experience * 100); }

PLAYER LOCATION:
x,y,z
current TEORRITORY -- checked every third second.

under vatten: mc.player.getAir() != 300
SPELLS AND EFFECTS:
powder effect percentage, and use.

sprint mode, and remaining sprint stamina

partial spell feedback: (sub)title for level 1-11, actionbar for level >= 12.

     * Return an array of the last spell in the action bar.
     * Each value will be {@link #SPELL_LEFT} or {@link #SPELL_RIGHT}.
     *
     * @return A boolean[] whose length is 0, 1, 2 or 3.

    public boolean[] getLastSpell() { -- interact with QuickCastManager

    INVENTORY MANAGEMENT:
    @return Total number of emeralds in inventory (Including blocks and LE)

    public int getMoney() {

     public int getHealthPotions() {
     public int getManaPotions() {
      public UnprocessedAmount getUnprocessedAmount() {
       public int getIngredientPouchCount(boolean countSlotsOnly) {
        public int getFreeInventorySlots() {

        get currentHorse & horse data

        get selected weapon

        get worn equipment

        event when starting to hold certain thing in your hand;
        new weapon, or soul points/compass etc.
----

chat message structure; wynnic parts. sender, type of message, etc. NPC / chat / other
guild + player name chat mentions.

fånga alla chattmeddelanden, konvertera och skicka ut som events per typ, och i slutändan,
konvertera tillbaka till chatt-meddelande (?) och skriv ut, om ingen gjort cancel innan.

byte av class

game events:
level up
player death
quest: start, update, finish
discovery found
gathered resource

social:
party -- created, dissolved, joined_existing, leave, added new member, removed member

guild -- join, leave, members added/removed? created?
  guild extra stats: exp, level, territories, etc

party / guild -- join, leave. get members & owner
friends -- added, removed, discovered

all other players on server -- world, location, etc. entity uuid.
athena: cosmetics, etc

a single object representing another player. that you can ask about position, world,
if it's a friend, guildmember, partymember, uuid, username

borde ha system fdör att skicka chatt-meddelanden och vänta på chatt-svar,
typ för att lista vänner. eller toggles?

--- quest book: "Quests", "Mini-Quests", "Discoveries", "Secret Discoveries"

compass location -- but this is really a *wynntils* concept.

non-model stuff: some better way to intercept packets, and engage in a complex "conversation"
with the server.

better way of interacting with "minecraft", like abstracting away Forge stuff, and
also abstracting away our InventoryRecplaers and Reflection utils.

non-model stuff: how to handle FakeInventory? esp. regarding interaction, selecting items etc.

non-model stuff: interface for extracting info from wiki

GUI stuff: common setting for e.g. rare/mythic colors. Duplicated in e.g. ItemGuide!
player status: can player movement speed be read? changes in speed sent as events?

non-model/wynntils-specific stuff: locked slots in inventory

player - AFK status, has not moved for a certain amount of time

chat in market search

even: on loot chest opened. for each item, check if rarity == mythic. be able to iterate over all
"real" item slots.

open chest: daily rewards
---

about the world: map locations, labels, NPCs, merchants etc.
waypoints: loot chests, -- record opening of chests automatically
gather spots  -- "recorded" points of interest.
 pesonal markers, added manually.

for each map location, store name and type. (type implies icon)

map knows about territories as well.

discoveries:
    TERRITORY(0, TextFormatting.WHITE),
    WORLD(1, TextFormatting.YELLOW),
    SECRET(2, TextFormatting.AQUA); -- get x,z (and lore?) from wiki

quests:
status:    STARTED(0), CAN_START(1), CANNOT_START(2), COMPLETED(3);
questbook analyzed: partial, full
one or zero quests can be tracked. quests can have event started or completed. mini-quests also.
quest can have "next location"

horse:
test if entity is horse and  belongs to player
mount horse
spawn horse (on command)
check if horse is spawned
horse: current xp, level, max level, (max xp =100), tier

soul points: time until next

inventory: sum of all money
inventory: sum of unprocessed materials, and their weight
new feature: copy of my bank account, and what I've got there. also, copy of what I'm selling on the
market.

items-db / identification needs more thinking!!!
inventory: consuming "consumable" - label change or item disappear when it is consumed.
inventory: try to drink potion -- can cancel

inventory: keep track of all consumables.
inventory: keep track of what I'm holding in my main hand. weapon, or consumable? or horse/boat?

opened chests: character info page, bank, market, loot, merchant,
daily rewards, (vanilla) quest book

daily reward timer, available, next available (jfr med next soul point) -- Timed Events?
server restart etc.

items-database: identifications has an order, are grouped, and can be inverted

self: get my current speed
---
all worlds/server: name, uptime, player list (?) -- get from api

player: can get and change nametag!
players: leaderboard, get rank for a player.

---
consumables: fattar inte riktigt. har en effekt, med typ en "identification" typ HealtRegen 35%.
parsar nånting, ID-pattern..?

----
items / identification.
list of all known items.
status for a specific item. identifier or not...
"ID-Bonus" == health regen, etc.. items kan också ha "major ID" == "special-förmågor".

[[[
Types of items:
Weapons, Armor, Accessories. There are six rarities of these (from least rare to rarest): normal, unique, rare, legendary, fabled and mythic. Additionally, there are also Set Items, which increase their stats depending on how many pieces of the set you're wearing.
Crafting materials and ingredients used in crafting.
Usable items - horses, potions, scrolls and dungeon keys.
Other items, such as powders, quest items, runes, etc.
]]]
--
set things: spell cast methods!

player status from action bar:  Powder % | RLR | Sprint | and if there is nothing more coordinates
 -- weapon powder effect load status (0-100%)
 -- sprint status and remaining sprint time (0-100%)
 -- partial spell cast

event when switching to a new weapon; this gets highlighted. and then a new event when highltighing stops.
---
reminder to open daily rewards. time until next rewward. etc?

--- player: get air remaining

for item: detect if food or scroll:
        else if (stack.getItemDamage() >= 70 && stack.getItemDamage() <= 75) // food, 70 <= damage <= 75
                else if (stack.getItemDamage() >= 42 && stack.getItemDamage() <= 44) // scrolls, 42 <= damage <= 44

for item: (from itemstack) get Rarity + get Rarity color!
Normal|Unique|Rare|Legendary|Fabled|Mythic|Set)
parse lore...

item: powder -- get tier
vilka tyyper av items finns det?
weapon, armor, accessories
ingredients, materials (refined + unrefined)
 dungeon keys, scrolls, boat/horse, boat pass, rune
emeralds, blocks and liquid; dungeon rewards
powders
potions, food
gathering tool
misc: misc item, untradable, repair scrap, quest items, no description at all (crushed bigfoot bone), boss altar rerwards (antic bead)

items: armor, weapon: powder slots, and installed powder types

player: objective / daily objective: task, max and current num.
player character info page: daily bonus, streak multiplier;
skill points,

player: time at which we joined server, world, class.

player: applied spell effects (vanish, speed boost, war scream, etc)

war: terrority attacked, defended, captured
     */
}
