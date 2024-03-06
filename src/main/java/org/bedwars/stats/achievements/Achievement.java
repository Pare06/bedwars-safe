package org.bedwars.stats.achievements;

import org.bukkit.Material;

public class Achievement {
    public static int NEXT_ID = 0;
    private final String name;
    private final String description;
    private final Material item;
    private final int id;
    private final Rarity rarity;

    public Achievement(String name, String description, Material item, Rarity rarity) {
        this.name = name;
        this.description = description;
        this.item = item;
        this.rarity = rarity;
        this.id = NEXT_ID++;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Material getItem() {
        return item;
    }

    public int getId() {
        return id;
    }

    public Rarity getRarity() {
        return rarity;
    }
}
