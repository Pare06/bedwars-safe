package org.bedwars.game.shop.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bedwars.config.ShopConfig;
import org.bedwars.game.listeners.LoadingListener;
import org.bedwars.game.shop.ShopItem;
import org.bedwars.npc.traits.BaseShop;
import org.bedwars.utils.BWPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class ShopListener implements Listener {
    @EventHandler
    public void onSectionClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) {
            return;
        }
        if (ShopConfig.sectionNames.contains(event.getView().title())) {
            int index = ShopConfig.sectionNames.indexOf(event.getCurrentItem().getItemMeta().displayName());
            if (index == -1) return; // non è lo shop?
            BaseShop.openShop((Player) event.getWhoClicked(), BaseShop.sectionMaterials.get(index), false);
        }
    }

    @EventHandler
    public void onItemClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() == event.getWhoClicked().getInventory()) return;
        if (!ShopConfig.sectionNames.contains(event.getView().title())) return;
        if (ShopConfig.sectionNames.contains(event.getCurrentItem().getItemMeta().displayName())) return;

        Player player = (Player) event.getWhoClicked();

        ShopItem sItem;
        if (List.of(LoadingListener.woolMaterials).contains(event.getCurrentItem().getType())) { // lana?
            sItem = ShopConfig.woolItem;
        } else {
            Optional<ShopItem> optional = ShopItem.get(event.getCurrentItem().clone());
            if (optional.isEmpty()) return;
            sItem = optional.get();
        }

        if (!player.getInventory().contains(sItem.getCostingMaterial(), sItem.getCost())) {
            player.sendMessage(Component.text("Non hai abbastanza materiali!")
                    .color(NamedTextColor.RED));
            return;
        }

        if (sItem.run(player)) { // l'oggetto si può comprare?
            player.getInventory().removeItem(new ItemStack(sItem.getCostingMaterial(), sItem.getCost()));
            if (sItem.shouldBeGiven()) {
                ItemStack item = sItem.getItem().clone();
                item.lore(null);
                player.getInventory().addItem(item);
            }
        }
        BaseShop.loadSection(BWPlayer.get(player).getShopSection(), player);
    }
}
