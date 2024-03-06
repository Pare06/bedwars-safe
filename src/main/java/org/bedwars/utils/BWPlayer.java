package org.bedwars.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bedwars.Bedwars;
import org.bedwars.config.ShopConfig;
import org.bedwars.game.Arena;
import org.bedwars.game.Team;
import org.bedwars.stats.ChatFlair;
import org.bedwars.stats.GameStats;
import org.bedwars.stats.achievements.AchievementData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BWPlayer {
    private final OfflinePlayer player;
    private GUI gui;
    private Material shopSection; // sezione aperta
    private Arena arena;
    private Arena rejoinArena;
    private Team team;
    private BWScoreboard scoreboard;
    private ChatFlair flair;
    private final GameStats stats;
    private final AchievementData achievements;
    private int availableFlairs;
    private static Map<String, BWPlayer> allPlayers;

    private OfflinePlayer combatLogPlayer; // con chi sta combattendo?
    private int combatLogTime; // quanto manca alla fine?
    private static final int COMBAT_LOG_DEFAULT = 10; // quando dura il combat?

    private final List<ItemStack> armorItems; // l'armatura
    private int pickaxeTier; // 0 - no, 1 - wooden, 2 - iron, 3 - gold, 4 - diamond
    private int axeTier;

    // achievement
    private Location fireballHit;

    private BWPlayer(Player p) {
        player = p;
        arena = null;
        rejoinArena = null;
        team = null;
        gui = null;
        stats = new GameStats(this);
        achievements = new AchievementData(p);
        scoreboard = null;
        armorItems = new ArrayList<>();
        combatLogPlayer = null;
        combatLogTime = 0;
        pickaxeTier = 0;
        axeTier = 0;
        fireballHit = null;
        giveInitialItems();
        loadAchievements();
    }

    public static ItemStack getPlayerHead(Player p) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skukllMeta = (SkullMeta) skull.getItemMeta();
        skukllMeta.setOwningPlayer(p);
        skull.setItemMeta(skukllMeta);

        return skull;
    }

    public void giveInitialItems() {
        armorItems.clear();
        armorItems.add(new ItemStack(Material.LEATHER_HELMET));
        armorItems.add(new ItemStack(Material.LEATHER_CHESTPLATE));
        armorItems.add(new ItemStack(Material.LEATHER_LEGGINGS));
        armorItems.add(new ItemStack(Material.LEATHER_BOOTS));
        armorItems.get(0).addEnchantment(Enchantment.WATER_WORKER, 1); // aqua affinity
    }

    public void loadAchievements() {
        try {
            PreparedStatement psSelectA = Bedwars.database.prepareStatement("SELECT * FROM achievements WHERE name = ?");
            psSelectA.setString(1, player.getName());
            getAchievements().loadFromQuery(psSelectA.executeQuery());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("DataFlowIssue") // come farebbe player.getName() a ritornare null se è online?
    public void addInitialItems() {
        if (!player.isOnline()) return;

        PlayerInventory inventory = Bukkit.getPlayer(player.getName()).getInventory();

        inventory.addItem(Items.enchant(Material.WOODEN_SWORD, Enchantment.DAMAGE_ALL, team.getUpgrade("swordDamage")));
        inventory.setItem(EquipmentSlot.HEAD, getArmorPiece(EquipmentSlot.HEAD));
        inventory.setItem(EquipmentSlot.CHEST, getArmorPiece(EquipmentSlot.CHEST));
        inventory.setItem(EquipmentSlot.LEGS, getArmorPiece(EquipmentSlot.LEGS));
        inventory.setItem(EquipmentSlot.FEET, getArmorPiece(EquipmentSlot.FEET));
        if (pickaxeTier != 0) player.getPlayer().getInventory().addItem(ShopConfig.pickaxes.get(pickaxeTier - 1).getItem());
        if (axeTier != 0) player.getPlayer().getInventory().addItem(ShopConfig.axes.get(axeTier - 1).getItem());
    }

    public GUI getGui() {
        return gui;
    }

    public void setGui(GUI gui) {
        this.gui = gui;
    }

    public int getIndex() {
        return team.getIndex();
    }

    public static BWPlayer get(OfflinePlayer p) {
        return allPlayers.get(p.getName());
    }

    public static void addBWPlayer(Player p) {
        if (allPlayers.containsKey(p.getName())) { // già loggato?
            BWPlayer bwPlayer = BWPlayer.get(p);
            bwPlayer.getGameStats().refresh(p);
            bwPlayer.getAchievements().refresh(p);
        } else {
            allPlayers.putIfAbsent(p.getName(), new BWPlayer(p)); // altrimenti aggiungi ai player
        }

        int flairId, unlockedFlairs;
        try {
            PreparedStatement psFlair = Bedwars.database.prepareStatement("SELECT flair FROM players WHERE name = ?");
            psFlair.setString(1, p.getName());
            flairId = psFlair.executeQuery().getInt("flair");

            PreparedStatement psUnlockedFlairs = Bedwars.database.prepareStatement("SELECT unlockedFlairs FROM players WHERE name = ?");
            psUnlockedFlairs.setString(1, p.getName());
            unlockedFlairs = psUnlockedFlairs.executeQuery().getInt("unlockedFlairs");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        BWPlayer.get(p).setFlair(ChatFlair.getFlair(flairId));
        BWPlayer.get(p).setAvailableFlairs(unlockedFlairs);
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public Arena getArena() {
        return arena;
    }

    public void setArena(Arena a) {
        arena = a;
        if (a != null) a.addPlayer(this.getPlayer());
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        if (this.team != null) {
            this.team.removePlayer((Player) player);
        }
        if (team != null) {
            team.addPlayer(player);
        }

        this.team = team;
    }

    public void setScoreboard(BWScoreboard scoreboard) {
        if (this.scoreboard != null) {
            this.scoreboard.disable();
        }
        this.scoreboard = scoreboard;
        this.scoreboard.enable();
    }

    public ItemStack getArmorPiece(EquipmentSlot slot) {
        int index = switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> throw new IllegalArgumentException();
        };

        return armorItems.get(index);
    }

    public void setArmorPiece(EquipmentSlot slot, ItemStack i) {
        int index = switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> throw new IllegalArgumentException();
        };

        armorItems.set(index, i);
    }

    public static void initialize() {
        allPlayers = new HashMap<>();
    }

    public OfflinePlayer getCombatLogPlayer() {
        return combatLogPlayer;
    }

    public void setCombatLogPlayer(Player combatLogPlayer) {
        setCombatLogPlayer(combatLogPlayer, COMBAT_LOG_DEFAULT);
    }

    public void setCombatLogPlayer(Player combatLogPlayer, int combatCooldown) {
        if (this.combatLogPlayer == combatLogPlayer) return;
        this.combatLogPlayer = combatLogPlayer;
        if (combatLogPlayer == null) return;

        combatLogTime = combatCooldown;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (combatLogTime > 0) {
                    combatLogTime--;
                } else {
                    setCombatLogPlayer(null);
                    cancel();
                }
            }
        }.runTaskTimer(Bedwars.Plugin, 0, 20);
    }

    public Material getShopSection() {
        return shopSection;
    }

    public void setShopSection(Material shopSection) {
        this.shopSection = shopSection;
    }

    public Arena getRejoinArena() {
        return rejoinArena;
    }

    public void setRejoinArena(Arena a) {
        rejoinArena = a;
    }

    public GameStats getGameStats() {
        return stats;
    }

    public static Map<String, BWPlayer> getAllPlayers() {
        return allPlayers;
    }

    public int getPickaxeTier() {
        return pickaxeTier;
    }

    public void incrementPickaxeTier() {
        if (pickaxeTier < 4) pickaxeTier++;
    }

    public int getAxeTier() {
        return axeTier;
    }

    public void incrementAxeTier() {
        if (axeTier < 4) axeTier++;
    }

    public void decrementTiers() {
        if (pickaxeTier > 1) pickaxeTier--;
        if (axeTier > 1) axeTier--;
    }

    public void resetTiers() {
        pickaxeTier = 0;
        axeTier = 0;
    }

    public AchievementData getAchievements() {
        return achievements;
    }

    public Location getFireballHit() {
        return fireballHit;
    }

    public void setFireballHit(Location fireballHit) {
        this.fireballHit = fireballHit;
    }

    public ChatFlair getFlair() {
        return flair;
    }

    public int getAvailableFlairs() {
        return availableFlairs;
    }

    public boolean hasFlair(int id) {
        return (availableFlairs >>> id & 1) == 1;
    }

    public void setFlair(ChatFlair flair) {
        this.flair = flair;
    }

    public void setAvailableFlairs(int availableFlairs) {
        this.availableFlairs = availableFlairs;
    }

    public void addFlair(int id) {
        this.availableFlairs |= 1 << id;
    }

    public void showFlairGUI() {
        Inventory inv = Bukkit.createInventory(null, (int) (Math.ceil(ChatFlair.getFlairNumber() / 9.0) * 9), Component.text("Seleziona flair"));

        for (int i = 0; i < ChatFlair.getFlairNumber(); i++) {
            ChatFlair flair = ChatFlair.getFlair(i);

            ItemStack item;
            if (hasFlair(i)) {
                item = new ItemStack(Material.NAME_TAG);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(flair.getGUIView());
                meta.lore(List.of(flair.getDescription()));
                item.setItemMeta(meta);
            } else {
                item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(flair.getChatView());
                meta.lore(List.of(Component.text("Non hai sbloccato questo flair!").color(NamedTextColor.RED)));
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }
        ((Player) player).openInventory(inv);
    }
}
