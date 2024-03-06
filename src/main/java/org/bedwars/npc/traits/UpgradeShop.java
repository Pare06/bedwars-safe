package org.bedwars.npc.traits;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bedwars.config.NPCConfig;
import org.bedwars.config.UpgradeShopConfig;
import org.bedwars.game.shop.ShopItem;
import org.bedwars.utils.BWPlayer;
import org.bedwars.utils.GUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;

@TraitName("UpgradeShop")
public class UpgradeShop extends Trait {

    public UpgradeShop() {
        super("UpgradeShop");
    }

    @EventHandler
    public void onPlayerInteractAtEntity(NPCRightClickEvent event) {
        Player player = event.getClicker();

        if (event.getNPC() == this.getNPC() && BWPlayer.get(player).getTeam() != null) {
            event.setCancelled(true);
            upgradeShop(player);
        }
    }

    public static void upgradeShop(Player player) {
        GUI gui = new GUI(NPCConfig.UPGRADE_SHOP_NAME, 45);

        gui.setPattern("         " +
                       " 0123456 " +
                       "GGGGGGGGG" +
                       "   789   " +
                       "         " );

        gui.setItem('G', Material.BLACK_STAINED_GLASS);
        loadUpgrades(player, gui);
        gui.applyPattern();
        gui.showToPlayer(player);
    }

    @SuppressWarnings("DataFlowIssue")
    private static void loadUpgrades(Player player, GUI gui) {
        for (int i = 0; i < UpgradeShopConfig.upgradeItems.size(); i++) {
            int index = BWPlayer.get(player).getTeam().getUpgrade(UpgradeShopConfig.upgradeNames.get(i));
            ShopItem sItem = UpgradeShopConfig.upgradeItems.get(i).get(index);

            ItemStack item = sItem.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            List<Component> lore = meta.lore();
            if (sItem.getCost() != -1) { // maxato?
                lore.add(Component.text("Costo: ")
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(String.format("%d %s", sItem.getCost(), getMaterialName(sItem.getCostingMaterial())))
                                .color(player.getInventory().contains(sItem.getCostingMaterial(), sItem.getCost()) // il player ha abbastanza materiali?
                                        ? NamedTextColor.GREEN
                                        : NamedTextColor.RED)));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            gui.setItem(String.valueOf(i).charAt(0), item);
        }
        gui.applyPattern();
    }

    @EventHandler
    public void onItemClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        if (event.getClickedInventory() == event.getWhoClicked().getInventory()) return;
        if (event.getView().title() != NPCConfig.UPGRADE_SHOP_NAME) return;

        Optional<ShopItem> optional = ShopItem.get(event.getCurrentItem().clone());
        if (optional.isEmpty()) return;
        ShopItem sItem = optional.get();

        Player player = (Player) event.getWhoClicked();

        if (!player.getInventory().contains(sItem.getCostingMaterial(), sItem.getCost())) {
            player.sendMessage(Component.text("Non hai abbastanza materiali!")
                    .color(NamedTextColor.RED));
            return;
        }

        player.getInventory().removeItem(new ItemStack(sItem.getCostingMaterial(), sItem.getCost()));
        sItem.run(player);
        loadUpgrades(player, BWPlayer.get(player).getGui());
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