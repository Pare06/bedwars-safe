package org.bedwars.game.shop;

import net.kyori.adventure.text.Component;
import org.bedwars.Bedwars;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ShopItem {
    private static int items = 0;
    private final int cost;
    private final Material costingMaterial;
    private final ItemStack item;
    private final Function<Player, Boolean> function; // input: Player -> output: false se non si può completare
                                                      //                          la funzione
    private final boolean shouldGiveItem; // false per l'armatura, sennò true

    private static final List<ShopItem> allShopItems = new ArrayList<>();

    public ShopItem(Material m, int c, Material cM) {
        this(new ItemStack(m), c, cM);
    }

    public ShopItem(ItemStack i, int c, Material cM) {
        this(i, c, cM, null, (x) -> true, true);
    }

    public ShopItem(Material m, int c, Material cM, List<Component> lore, Function<Player, Boolean> func, boolean give) {
        this(new ItemStack(m), c, cM, lore, func, give);
    }

    public ShopItem(ItemStack i, int c, Material cM, List<Component> lore, Function<Player, Boolean> func, boolean give) {
        this(i, c, cM, lore, func, give, "");
    }

    public ShopItem(ItemStack i, int c, Material cM, List<Component> lore, Function<Player, Boolean> func, boolean give, String upgradeName) {
        if (lore == null) lore = new ArrayList<>();
        lore.add(Component.empty());
        int id = items++;
        ItemMeta meta = i.getItemMeta();
        meta.lore(lore);
        // alternativa poco goofy al metadata per gli itemstack
        meta.getPersistentDataContainer().set(new NamespacedKey(Bedwars.Plugin, "shop_id"), PersistentDataType.STRING, String.valueOf(id));
        i.setItemMeta(meta);
        item = i;
        cost = c;
        costingMaterial = cM;
        function = func;
        shouldGiveItem = give;
        allShopItems.add(this);
    }

    public boolean shouldBeGiven() {
        return shouldGiveItem;
    }

    public boolean run(Player p) {
        return function.apply(p);
    }

    public static Optional<ShopItem> get(ItemStack i) {
        ItemStack item = i.clone();
        item.lore(null);
        for (ShopItem x : allShopItems) {
            ItemStack shop = x.getItem().clone();
            shop.lore(null);
            if (item.isSimilar(shop)) {
                return Optional.of(x);
            }
        }
        return Optional.empty();
    }

    public int getCost() {
        return cost;
    }

    public Material getCostingMaterial() {
        return costingMaterial;
    }

    public ItemStack getItem() {
        return item;
    }

    public static String getMaterialName(Material m) {
        return switch (m) {
            case IRON_INGOT -> "Ferro";
            case GOLD_INGOT -> "Oro";
            case DIAMOND -> "Diamanti";
            case EMERALD -> "Smeraldi";
            default -> throw new IllegalArgumentException("oggetto non aggiunto");
        };
    }
}