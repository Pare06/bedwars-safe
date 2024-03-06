package org.bedwars.stats.achievements;

import net.kyori.adventure.text.format.NamedTextColor;

public enum Rarity {
    COMMON(NamedTextColor.WHITE),
    RARE(NamedTextColor.AQUA),
    EPIC(NamedTextColor.DARK_PURPLE),
    LEGENDARY(NamedTextColor.GOLD),
    MYTHIC(NamedTextColor.DARK_RED);

    Rarity(NamedTextColor color) {
        this.color = color;
    }

    private static final Rarity[] rarities = values();
    private final NamedTextColor color;

    public static Rarity getIndex(int tier) {
        return rarities[tier - 1];
    }

    public NamedTextColor getColor() {
        return color;
    }
}
