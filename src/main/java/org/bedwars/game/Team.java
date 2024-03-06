package org.bedwars.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bedwars.Bedwars;
import org.bedwars.config.ArenaConfig;
import org.bedwars.config.ArenaConfig.XYZCoords;
import org.bedwars.config.MetadataConfig;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static org.bedwars.game.listeners.LoadingListener.woolColors;
import static org.bedwars.game.listeners.LoadingListener.woolNames;

public class Team {
    private final List<OfflinePlayer> players;
    private final TeamColor color;
    private final int mode; // 1-4
    private TeamState teamState;
    private final Location spawn;
    private final HashMap<String, Integer> upgrades;

    // generatori

    private int ironCooldown = 20;
    private int goldCooldown = 8 * 20;
    private boolean areGensUpgraded = false;

    public Team(TeamColor c, int m, Location l) {
        players = new ArrayList<>();
        color = c;
        mode = m;
        this.spawn = l;
        teamState = TeamState.WITH_BED;
        upgrades = new HashMap<>();
    }

    public int getIndex() {
        return List.of(TeamColor.values()).indexOf(color);
    }

    public List<OfflinePlayer> getPlayers() {
        return players;
    }

    public List<Player> getOnlinePlayers() {
        return players.stream().filter(OfflinePlayer::isOnline).map(OfflinePlayer::getPlayer).toList();
    }

    /**
     * aggiunge un player
     */
    public void addPlayer(OfflinePlayer player) {
        removeAllCopies(player);
        if (players.size() == mode) throw new UnsupportedOperationException("Il team è pieno.");
        // getName() non ritornerà null a meno che player non sia offline
        // (e non dovrebbe esserlo quando viene chiamato addPlayer)
        //noinspection DataFlowIssue
        players.add(Bukkit.getPlayer(player.getName())); // refresha il player
    }

    public boolean containsPlayer(OfflinePlayer player) {
        return players.stream().anyMatch(p -> Objects.equals(p.getName(), player.getName()));
    }

    public void removePlayer(Player player) {
        Arena arena = Arena.getArena(player.getWorld());
        if (getNPlayers() == 1 && arena.getState() == Arena.ArenaState.STARTED) {
            arena.getOnlinePlayers().forEach(p -> p.sendMessage(
                    Component.text("Il team ")
                            .color(NamedTextColor.DARK_GRAY)
                            .append(Component.text(woolNames[getIndex()])
                                    .color(getTextColor())
                            .append(Component.text(" è stato ")
                                    .color(NamedTextColor.DARK_GRAY))
                            .append(Component.text("eliminato!")
                                    .color(NamedTextColor.DARK_RED)))));
            teamState = TeamState.ELIMINATED;
            arena.getSharedBoard().setState(TeamState.ELIMINATED, getIndex());
        }
        removeAllCopies(player);
    }

    public void clear() {
        players.clear();
    }

    // [5 righe di sfogo]
    private void removeAllCopies(OfflinePlayer player) {
        players.removeIf(p -> Objects.equals(p.getName(), player.getName()));
    }

    public boolean isFull() {
        return players.size() == mode;
    }

    public TeamColor getColor() {
        return color;
    }

    public void setTeamState(TeamState teamState) {
        this.teamState = teamState;
    }

    public TeamState getTeamState() {
        return teamState;
    }

    public int getNPlayers() {
        return players.size();
    }

    public int getMaxPlayers() {
        return mode;
    }

    public Location getSpawn() {
        return spawn;
    }

    public NamedTextColor getTextColor() {
        return woolColors[getIndex()];
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public void setTasks(String originalName, World world) {
        startGen(Material.IRON_INGOT, originalName, world, ironCooldown);
        startGen(Material.GOLD_INGOT, originalName, world, goldCooldown);
    }

    private void startGen(Material m, String originalName, World world, int cd) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (areGensUpgraded) { // il gen. è appena stato upgradato?
                    areGensUpgraded = false;
                    cancel();
                    setTasks(originalName, world); // restarta i BukkitRunnable ma col cooldown aggiornato
                } else {
                    dropMaterial(m, originalName, world);
                }
            }
        }.runTaskTimer(Bedwars.Plugin, 0, cd);
    }

    private void dropMaterial(Material m, String originalName, World w) {
        // lo spawner di questo team
        XYZCoords spawner = ArenaConfig.TEAM_SPAWNERS.get(originalName).get(getIndex());
        Location loc = new Location(
                w,
                spawner.x(),
                spawner.y(),
                spawner.z()
        );

        int maxOres = m == Material.IRON_INGOT ? ArenaConfig.MAX_IRON_SPAWNED : ArenaConfig.MAX_GOLD_SPAWNED;
        int nearbyOres = (int) loc.getNearbyEntities(2, 2, 2).stream() // tutte le entità a 2 blocchi di distanza
                .filter(e -> e.hasMetadata(MetadataConfig.GEN_SPAWNED)) // spawnate dal gen
                .filter(e -> ((Item) e).getItemStack().getType() == m) // dello stesso materiale da droppare
                .count();

        // limite max. di ore spawnati?
        if (nearbyOres >= maxOres) {
            return;
        }

        Item item = w.dropItem(loc, new ItemStack(m));
        item.setVelocity(new Vector(0, 0, 0));
        item.setMetadata(MetadataConfig.GEN_SPAWNED, new FixedMetadataValue(Bedwars.Plugin, true));
        item.setUnlimitedLifetime(true);
    }

    public int addUpgrade(String upgrade) {
        if (upgrades.containsKey(upgrade)) {
            upgrades.put(upgrade, upgrades.get(upgrade) + 1);
        } else {
            upgrades.put(upgrade, 1);
        }
        return upgrades.get(upgrade);
    }

    public int getUpgrade(String upgrade) {
        return upgrades.getOrDefault(upgrade, 0);
    }

    public void setIronCooldown(int cd) {
        ironCooldown = cd;
        areGensUpgraded = true;
    }

    public void setGoldCooldown(int cd) {
        goldCooldown = cd;
        areGensUpgraded = true;
    }
}
