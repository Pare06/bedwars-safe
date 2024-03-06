package org.bedwars.game.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bedwars.Bedwars;
import org.bedwars.config.InventoryConfig;
import org.bedwars.config.LobbyConfig;
import org.bedwars.game.Team;
import org.bedwars.game.TeamColor;
import org.bedwars.general.listeners.GeneralListener;
import org.bedwars.lobby.LobbyInterface;
import org.bedwars.utils.BWPlayer;
import org.bedwars.utils.GUI;
import org.bedwars.config.ArenaConfig;
import org.bedwars.game.Arena;
import org.bedwars.utils.BWScoreboard;
import org.bedwars.utils.Items;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoadingListener implements Listener {
    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerJoin(PlayerChangedWorldEvent event) {
        if (event.getFrom() != LobbyConfig.LOBBY) return;

        Player player = event.getPlayer();
        World world = player.getWorld();
        Arena arena = Arena.getArena(world);
        Inventory inventory = player.getInventory();
        BWPlayer bwPlayer = BWPlayer.get(player);

        bwPlayer.setArena(arena);
        // givva gli item
        inventory.clear();
        inventory.setItem(0, Items.rename(Material.WHITE_WOOL, Component.text("Seleziona team")
                .decoration(TextDecoration.BOLD, false)
                .content()));
        inventory.setItem(4, Items.rename(Material.BOOK, "Statistiche"));
        inventory.setItem(8, Items.rename(Material.RED_BED, "Ritorna alla lobby"));

        arena.removePlayer(player);
        arena.addPlayer(Bukkit.getPlayer(player.getName())); // registra il player nel game

        BWScoreboard scoreboard = new BWScoreboard(player);
        List<String> scores = new ArrayList<>();
        scores.add("Mappa: " + ArenaConfig.MAP_NAMES.get(arena.getOriginalName())); // 6
        scores.add(""); // players, 5
        scores.add(" "); // 4
        scores.add("  "); // countdown, 3
        scores.add("Modalità: " + ChatColor.GRAY + arena.getModeName()); // 2
        scores.add("   "); // 1
        scores.add("mc.feargames.it"); // 0
        scoreboard.loadScores(scores);
        bwPlayer.setScoreboard(scoreboard);

        if (arena.getCountdown() != -1 && arena.getCountdown() < 5) {
            arena.setCountdown(5);
        }

        // si ricarica ogni tick
        new BukkitRunnable() { // aggiorna la scoreboard
            @Override
            public void run() {
                // game startato?
                if (arena.getState() == Arena.ArenaState.STARTED) {
                    cancel();
                }

                String countdownStr;

                if (arena.getMaxPlayers() / 2 <= arena.getNPlayers()) { // numero min. di player raggiunto?
                    arena.start(player);
                    countdownStr = String.format("Inizia tra: %s%d", countdownColor(arena.getCountdown()), arena.getCountdown());
                } else {
                    countdownStr = String.format("Aspettando %d giocatori...", arena.getMaxPlayers() / 2 - arena.getNPlayers());
                }

                String players = String.format("Giocatori: %s%d/%d",
                        playersColor(arena.getNPlayers(), arena.getMaxPlayers()),
                        arena.getNPlayers(), arena.getMaxPlayers());
                scoreboard.set(countdownStr, 3);
                scoreboard.set(players, 5);
            }
        }.runTaskTimer(Bedwars.Plugin, 0, 1);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || Arena.isNotArena(event.getPlayer().getWorld())) return;
        if (Arena.getArena(event.getPlayer().getWorld()).getState() == Arena.ArenaState.STARTED) return;

        Player player = event.getPlayer();

        // lana?
        if (List.of(woolMaterials).contains(event.getItem().getType())) {
            GUI gui = new GUI(InventoryConfig.TEAM_SELECTOR,36);

            gui.setPattern("         " +
                           " 0 1 2 3 " +
                           " 4 5 6 7 " +
                           "         " );

            Arena arena = Arena.getArena(player.getWorld());

            for (int i = 0; i < woolMaterials.length; i++) { // per ogni team
                ItemStack item = Items.enchant(new ItemStack(woolMaterials[i]));
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component.text("Team ")
                        .append( // necessario per non colorare "Team"
                                Component.text(woolNames[i])
                                        .color(woolColors[i])
                        )
                        .decoration(TextDecoration.ITALIC, false));

                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(Component.text("Giocatori: ")
                        .color(NamedTextColor.WHITE));
                lore.add(Component.empty());
                Team team = arena.getTeam(TeamColor.values()[i]);
                // aggiunge i player del team
                for (OfflinePlayer p : team.getPlayers()) {
                    //noinspection DataFlowIssue
                    lore.add(Component.text(p.getPlayer().getName())
                            .color(woolColors[i]));
                }
                if (lore.size() == 3) { // nessun player?
                    lore.clear();
                    lore.add(Component.empty());
                    lore.add(Component.text("Nessun giocatore")
                            .color(woolColors[i]));
                }
                lore.replaceAll(s -> s.decoration(TextDecoration.ITALIC, false));

                meta.lore(lore);
                item.setItemMeta(meta);

                gui.setItem(String.valueOf(i).charAt(0), item);
            }

            gui.applyPattern();
            gui.showToPlayer(player);
        } else if (event.getItem().getType() == Material.BOOK) {
            BWPlayer.get(player).getGameStats().showGUI();
        } else if (event.getItem().getType() == Material.RED_BED) {
            removePlayer(player);
            GeneralListener.teleportSpawn(player);
            LobbyInterface.sendLobbyScoreboard(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;
        if (event.getView().title().equals(InventoryConfig.TEAM_SELECTOR)) {
            event.setCancelled(true);

            Player player = (Player) event.getWhoClicked();
            BWPlayer bwPlayer = BWPlayer.get(player);

            List<Material> teamSelect = new ArrayList<>(List.of(woolMaterials));

            ArrayList<TeamColor> teamColors = new ArrayList<>(List.of(TeamColor.values()));

            Optional<Material> optionalClicked = teamSelect.stream() // l'item cliccato
                    .filter(m -> m == event.getCurrentItem().getType()).findFirst();

            if (optionalClicked.isEmpty()) return; // ha cliccato l'aria?

            Material material = optionalClicked.get();
            Arena arena = Arena.getArena(player.getWorld());
            int index = teamSelect.indexOf(material);
            Team team = arena.getTeam(teamColors.get(index));

            if (team.containsPlayer(player)) { // stesso team?
                player.sendMessage(Component.text("Sei già in questo team!")
                    .color(NamedTextColor.RED));
            } else if (team.isFull()) { // team pieno?
                player.sendMessage(Component.text("Il team è pieno!")
                    .color(NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text("Selezionato il team ")
                    .append(
                        Component.text(woolNames[index])
                            .color(woolColors[index])
                        )
                        .append(
                            Component.text("!")
                                .color(NamedTextColor.WHITE)
                        )
                    );
                bwPlayer.setTeam(team);
                player.getInventory().setItem(0, Items.enchant(material));
                // wool -> glass
                player.getInventory().setHelmet(new ItemStack(glassMaterials[List.of(woolMaterials).indexOf(material)]));
            }

            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        if (Arena.isNotArena(event.getPlayer().getWorld())) return;

        Player player = event.getPlayer();
        Arena arena = Arena.getArena(player.getWorld());

        if (arena.getState() == Arena.ArenaState.STARTED || arena.getState() == Arena.ArenaState.ENDED) return;

        removePlayer(player);
    }

    private void removePlayer(Player player) {
        BWPlayer bwPlayer = BWPlayer.get(player);
        Arena arena = bwPlayer.getArena();

        bwPlayer.setTeam(null);
        arena.removePlayer(player);
        bwPlayer.setArena(null);
        bwPlayer.setRejoinArena(null);
        if (bwPlayer.getTeam() != null) {
            bwPlayer.getTeam().removePlayer(player);
        }
        // non ci sono più abbastanza player?
        if (arena.getMaxPlayers() / 2 > arena.getNPlayers() && (arena.getState() != Arena.ArenaState.STARTED
                &&  arena.getState() != Arena.ArenaState.ENDED)) {
            arena.setCountdown(30);
            arena.setState(Arena.ArenaState.WAITING);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        Entity player = event.getEntity();
        World world = player.getWorld();

        if (!(player instanceof Player)) return;
        if (Arena.isNotArena(world)) return;

        Arena arena = Arena.getArena(player.getWorld());

        // annulla il danno
        if (arena.getState() != Arena.ArenaState.STARTED) event.setDamage(0);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (Arena.isNotArena(world)) return;
        if (Arena.getArena(world).getState() == Arena.ArenaState.STARTING
        ||  Arena.getArena(world).getState() == Arena.ArenaState.WAITING) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation") // finchè non si trova un modo di usare Component nella scoreboard questo rimane
    private static ChatColor playersColor(int count, int max) {
        if (count < max / 2) { // non abbastanza?
            if (count == max / 2 - 1) { // manca 1?
                return ChatColor.YELLOW;
            }
            return ChatColor.RED;
        }
        if (count == max) return ChatColor.DARK_GREEN; // max. di player?
        return ChatColor.GREEN;
    }

    @SuppressWarnings("deprecation") // leggi sopra
    private static ChatColor countdownColor(int countdown) {
        if (countdown < 5) return ChatColor.RED;
        if (countdown < 15) return ChatColor.YELLOW;
        return ChatColor.GREEN;
    }

    public static final Material[] woolMaterials = {
        Material.RED_WOOL,
        Material.BLUE_WOOL,
        Material.GREEN_WOOL,
        Material.YELLOW_WOOL,
        Material.LIGHT_BLUE_WOOL,
        Material.WHITE_WOOL,
        Material.PINK_WOOL,
        Material.GRAY_WOOL
    };

    public static final Material[] glassMaterials = {
        Material.RED_STAINED_GLASS,
        Material.BLUE_STAINED_GLASS,
        Material.GREEN_STAINED_GLASS,
        Material.YELLOW_STAINED_GLASS,
        Material.LIGHT_BLUE_STAINED_GLASS,
        Material.WHITE_STAINED_GLASS,
        Material.PINK_STAINED_GLASS,
        Material.GRAY_STAINED_GLASS
    };

    public static final String[] woolNames = {
        "rosso", "blu", "verde", "giallo", "azzurro", "bianco", "rosa", "grigio"
    };

    public static final NamedTextColor[] woolColors = new NamedTextColor[] {
        NamedTextColor.RED, NamedTextColor.BLUE, NamedTextColor.GREEN, NamedTextColor.YELLOW,
        NamedTextColor.AQUA, NamedTextColor.WHITE, NamedTextColor.LIGHT_PURPLE, NamedTextColor.GRAY
    };
}
