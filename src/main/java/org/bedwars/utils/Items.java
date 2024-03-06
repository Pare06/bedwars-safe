package org.bedwars.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class Items {
    private Items() { }

    public static ItemStack rename(Material m, String s) {
        return rename(new ItemStack(m), s);
    }

    public static ItemStack rename(ItemStack item, String s) {
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(s)
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack enchant(Material m) {
        return enchant(new ItemStack(m));
    }

    public static ItemStack enchant(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack enchant(Material m, Enchantment ench, int level) {
        return enchant(new ItemStack(m), ench, level);
    }

    public static ItemStack enchant(ItemStack item, Enchantment ench, int level) {
        if (level == 0) return item;
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(ench, level, true);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack renameEnchant(Material m, String s) {
        return enchant(rename(m, s));
    }

    public static ItemStack quantity(Material m, int i) {
        return new ItemStack(m, i);
    }

    public static ItemStack lore(Material m, Component... lines) {
        return lore(new ItemStack(m), lines);
    }

    public static ItemStack lore(ItemStack i, Component... lines) {
        i.lore(Arrays.stream(lines).toList());
        return i;
    }
}
