/*
 * Copyright © Wynntils 2022-2025.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.gear.type;

import com.wynntils.core.text.StyledText;
import com.wynntils.models.character.type.ClassType;
import com.wynntils.utils.mc.LoreUtils;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;

public enum GearType {
    SPEAR(ClassType.WARRIOR, Items.IRON_HORSE_ARMOR, 437, 497, 0),
    WAND(ClassType.MAGE, Items.IRON_HORSE_ARMOR, 308, 372, 1),
    DAGGER(ClassType.ASSASSIN, Items.IRON_HORSE_ARMOR, 244, 307, 2),
    BOW(ClassType.ARCHER, Items.IRON_HORSE_ARMOR, 182, 243, 3),
    RELIK(ClassType.SHAMAN, Items.IRON_HORSE_ARMOR, 373, 436, 4),
    // This is a fallback for signed, crafted gear with a skin
    WEAPON(null, Items.IRON_HORSE_ARMOR, 0, 0, 12),
    // Note: This fallback should basically be never be matched, but we use it in item encoding
    //       (as it's the same as WEAPON, and we have no other info)
    ACCESSORY(null, Items.IRON_HORSE_ARMOR, 0, 0, 13),
    RING(null, Items.IRON_HORSE_ARMOR, 134, 150, 5),
    BRACELET(null, Items.IRON_HORSE_ARMOR, 151, 164, 6),
    NECKLACE(null, Items.IRON_HORSE_ARMOR, 165, 181, 7),
    HELMET(
            null,
            Items.IRON_HORSE_ARMOR,
            498,
            629,
            List.of(
                    Items.LEATHER_HELMET,
                    Items.CHAINMAIL_HELMET,
                    Items.IRON_HELMET,
                    Items.GOLDEN_HELMET,
                    Items.DIAMOND_HELMET),
            8),
    CHESTPLATE(
            null,
            Items.LEATHER_CHESTPLATE,
            0,
            0,
            List.of(
                    Items.CHAINMAIL_CHESTPLATE,
                    Items.IRON_CHESTPLATE,
                    Items.GOLDEN_CHESTPLATE,
                    Items.DIAMOND_CHESTPLATE),
            9),
    LEGGINGS(
            null,
            Items.LEATHER_LEGGINGS,
            0,
            0,
            List.of(Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.DIAMOND_LEGGINGS),
            10),
    BOOTS(
            null,
            Items.LEATHER_BOOTS,
            0,
            0,
            List.of(Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.GOLDEN_BOOTS, Items.DIAMOND_BOOTS),
            11),
    MASTERY_TOME(null, Items.IRON_HORSE_ARMOR, 76, 82, -1),
    CHARM(null, Items.CLAY_BALL, 0, 0, -1);

    private static final Pattern SKINNED_GEAR_PATTERN = Pattern.compile("^§7\\[Active (.+) Skin: (.+)]$");

    private final ClassType classReq;
    private final Item defaultItem;
    private final List<Float> models;
    private final List<Item> otherItems;
    private final int encodingId;

    GearType(
            ClassType classReq,
            Item defaultItem,
            int firstModel,
            int lastModel,
            List<Item> otherItems,
            int encodingId) {
        this.classReq = classReq;
        this.defaultItem = defaultItem;
        this.models = firstModel == 0 && lastModel == 0
                ? List.of()
                : IntStream.rangeClosed(firstModel, lastModel)
                        .mapToObj(i -> (float) i)
                        .toList();
        this.otherItems = otherItems;
        this.encodingId = encodingId;
    }

    GearType(ClassType classReq, Item defaultItem, int firstModel, int lastModel, int encodingId) {
        this(classReq, defaultItem, firstModel, lastModel, List.of(), encodingId);
    }

    public static GearType fromString(String typeStr) {
        try {
            return GearType.valueOf(typeStr.toUpperCase(Locale.ROOT).replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static GearType fromItemStack(ItemStack itemStack) {
        Item item = itemStack.getItem();
        for (GearType gearType : values()) {
            // We only want to match for proper gear, not rewards
            if (gearType.isReward()) continue;

            CustomModelData customModelData = itemStack.get(DataComponents.CUSTOM_MODEL_DATA);

            // Special case for armor gear
            // Only check for the models in case the item is the default item, otherwise accept any item
            if (customModelData == null && (gearType.defaultItem.equals(item) && gearType.models.isEmpty())
                    || gearType.otherItems.contains(item)) {
                return gearType;
            }

            if (customModelData != null) {
                List<Float> customModelDataValue = customModelData.floats();
                for (Float modelValue : customModelDataValue) {
                    if (gearType.defaultItem.equals(item) && gearType.models.contains(modelValue)) {
                        return gearType;
                    }
                }
            }
        }

        // Gears have a lot of different skinned items, checking for the string in lore is more reliable
        List<StyledText> lore = LoreUtils.getLore(itemStack);
        if (!lore.isEmpty()) {
            Matcher matcher = lore.getLast().getMatcher(SKINNED_GEAR_PATTERN);
            if (matcher.matches()) {
                return GearType.fromString(matcher.group(1));
            }
        }

        return null;
    }

    public static GearType fromClassType(ClassType classType) {
        for (GearType gearType : values()) {
            if (gearType.classReq == classType) return gearType;
        }
        return null;
    }

    public static GearType fromEncodingId(int id) {
        for (GearType gearType : values()) {
            if (gearType.encodingId == id) return gearType;
        }
        return null;
    }

    public ClassType getClassReq() {
        return classReq;
    }

    public Item getDefaultItem() {
        return defaultItem;
    }

    public float getDefaultModel() {
        return models.getFirst();
    }

    public int getEncodingId() {
        return encodingId;
    }

    public boolean isReward() {
        return this == MASTERY_TOME || this == CHARM;
    }

    public boolean isWeapon() {
        return (classReq != null) || this == WEAPON;
    }

    public boolean isAccessory() {
        // Flint and steel is used for crafted items, normal items are horse armor
        return defaultItem == Items.FLINT_AND_STEEL || defaultItem == Items.IRON_HORSE_ARMOR;
    }

    public boolean isArmor() {
        return switch (this) {
            case HELMET, CHESTPLATE, LEGGINGS, BOOTS -> true;
            default -> false;
        };
    }

    public boolean isValidWeapon(ClassType classType) {
        if (!isWeapon()) return false;
        // We can't really know what kind of weapon this is, so assume it is valid
        if (this == WEAPON) return true;

        return classReq == classType;
    }
}
