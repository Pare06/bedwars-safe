package org.bedwars.stats.achievements;

import org.bukkit.Material;

import java.util.*;

public class TieredAchievement {
    private final String name;
    private final String description;
    private final Material[] items;
    private final int[] tiers;
    private final int maxTier;
    private final int id;

    public TieredAchievement(String name, String description, int[] tiers, Material item) {
        this(name, description, tiers, filledArray(item, tiers.length));
    }

    public TieredAchievement(String name, String description, int[] tiers, Material... items) {
        this.name = name;
        this.description = description;
        this.items = items;
        this.tiers = tiers;
        this.id = Achievement.NEXT_ID;
        Achievement.NEXT_ID += tiers.length;
        maxTier = tiers.length - 1;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Material[] getItems() {
        return items;
    }

    // 1 - primo tier, 2 - secondo, ... , 0 - nessun tier
    public int highestTier(int value) {
        int tier = 0;
        for (; tier != tiers.length && value >= tiers[tier]; tier++);
        return tier;
    }

    public int[] getTiers() {
        return tiers;
    }

    public int getMaxTiers() {
        return maxTier;
    }

    public int getId() {
        return id;
    }

    private static Material[] filledArray(Material m, int times) {
        Material[] array = new Material[times];
        Arrays.fill(array, m);
        return array;
    }
}
