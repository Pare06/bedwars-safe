package org.bedwars.npc.traits;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bedwars.config.ShopConfig;
import org.bedwars.game.shop.ShopItem;
import org.bedwars.utils.BWPlayer;
import org.bedwars.utils.GUI;
import org.bedwars.utils.Items;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static org.bedwars.config.ShopConfig.allItems;
import static org.bedwars.config.ShopConfig.sectionNames;
import static org.bedwars.game.listeners.LoadingListener.woolMaterials;
import static org.bedwars.game.shop.ShopItem.getMaterialName;

@TraitName("BaseShop")
public class BaseShop extends Trait {
    public BaseShop() {
        super("BaseShop");
    }

    @EventHandler
    public void onPlayerInteractAtEntity(NPCRightClickEvent event) {
        Player player = event.getClicker();

        if (event.getNPC() == this.getNPC() && BWPlayer.get(player).getTeam() != null) {
            event.setCancelled(true);
            openShop(player, Material.NETHER_STAR, true);
        }
    }

    public static void openShop(Player player, Material section, boolean isNew) {
        GUI shop = isNew
                ? new GUI(sectionNames.get(sectionMaterials.indexOf(section)), 45)
                : BWPlayer.get(player).getGui();

        shop.setPattern("012345678" +
                "GGGGGGGGG" +
                " abcdefg " +
                " hijklmn " +
                " opqrstu ");
        shop.setItem('0', Material.NETHER_STAR, "Scelta rapida");
        shop.setItem('1', Material.IRON_SWORD, "Armi");
        shop.setItem('2', Material.IRON_CHESTPLATE, "Armature");
        shop.setItem('3', Material.BOW, "Archi");

        BWPlayer bwPlayer = BWPlayer.get(player);
        bwPlayer.setShopSection(section);
        int woolIndex = BWPlayer.get(player).getIndex();
        Material woolMaterial = woolMaterials[woolIndex];
        shop.setItem('4', woolMaterial, "Blocchi");
        shop.setItem('5', Material.IRON_PICKAXE, "Strumenti");

        for (Material material : sectionMaterials) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            List<Component> lore = new ArrayList<>();

            if(material == section) {
                Items.enchant(item);
                lore.add(Component.text("Selezionato")
                        .color(NamedTextColor.GREEN));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        // e possibilmente altre 3 sezioni (7 8 9)
        shop.setItem('G', Material.WHITE_STAINED_GLASS_PANE);

        loadSection(section, player);
        shop.applyPattern();
        if (isNew) shop.showToPlayer(player);
    }

    @SuppressWarnings("DataFlowIssue")
    public static void loadSection(Material section, Player player) {
        // funzione taglia-incollata da openShop. da cambiare?
        List<ShopItem> sectionItems = new ArrayList<>(allItems.get(sectionMaterials.indexOf(section)));
        BWPlayer bwPlayer = BWPlayer.get(player);
        GUI shop = bwPlayer.getGui();
        String alphabet = "abcdefghijklmnopqrstu";

        if (section == Material.IRON_PICKAXE) {
            addTools(sectionItems, bwPlayer.getPickaxeTier(), bwPlayer.getAxeTier());
        }

        int index = 0;
        for (char ch : alphabet.toCharArray()) { // cancella tutti gli item
            shop.setItem(ch, Material.AIR);
        }
        for (int i = 17; i < 44; i++) { // file 3, 4, 5
            if (sectionItems.isEmpty()) break; // sezione finita?

            if (section == Material.NETHER_STAR) {
                // scelta  rapida
            } else {
                ShopItem sItem = sectionItems.remove(0);
                ItemStack item = sItem.getItem().clone();

                ItemMeta meta = item.getItemMeta();
                List<Component> lore = meta.lore();
                lore.add(Component.text("Costo: ")
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(String.format("%d %s", sItem.getCost(), getMaterialName(sItem.getCostingMaterial())))
                                .color(player.getInventory().contains(sItem.getCostingMaterial(), sItem.getCost()) // il player ha abbastanza materiali?
                                        ? NamedTextColor.GREEN
                                        : NamedTextColor.RED)));
                meta.lore(lore);
                item.setItemMeta(meta);

                // lana?
                if (item.getType() == Material.DEAD_BUSH) {
                    item.setType(woolMaterials[bwPlayer.getIndex()]);
                    item.setAmount(16);
                }

                shop.setItem(alphabet.charAt(index), item);
                index++;
            }
        }
        shop.applyPattern();
    }

    private static void addTools(List<ShopItem> items, int pickaxe, int axe) {
        items.add(0, ShopConfig.pickaxes.get(pickaxe));
        items.add(1, ShopConfig.axes.get(axe));
    }

    public static final List<Material> sectionMaterials = List.of(new Material[] {
            Material.NETHER_STAR, Material.IRON_SWORD, Material.IRON_CHESTPLATE,
            Material.BOW, Material.DEAD_BUSH, Material.IRON_PICKAXE
    });
}
