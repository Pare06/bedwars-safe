package org.bedwars.game.listeners;

import io.papermc.paper.event.player.PlayerBedFailEnterEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bedwars.Bedwars;
import org.bedwars.config.Achievements;
import org.bedwars.config.ArenaConfig;
import org.bedwars.config.MetadataConfig;
import org.bedwars.game.Arena;
import org.bedwars.game.Team;
import org.bedwars.game.TeamColor;
import org.bedwars.game.TeamState;
import org.bedwars.utils.BWPlayer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;

public class BedwarsListener implements Listener {
    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        if (Arena.isNotArena(block.getWorld())) return;

        Arena arena = Arena.getArena(block.getWorld());
        if (arena.getState() == Arena.ArenaState.STARTED) {
            // segna il blocco come "piazzato da un player"
            block.setMetadata(MetadataConfig.PLAYER_PLACED, new FixedMetadataValue(Bedwars.Plugin, true));
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorBreak(PlayerItemDamageEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material material = event.getBlock().getType();
        Player player = event.getPlayer();

        if (Arena.isNotArena(block.getWorld()) || Arena.getArena(block.getWorld()).getState() != Arena.ArenaState.STARTED) {
            event.setCancelled(true);
            return;
        }

        Arena arena = Arena.getArena(block.getWorld());

        if (material == Material.RED_BED) { // letto rotto?
            Location bedLocation = block.getLocation();
            double[][][] arenaBeds = ArenaConfig.ARENA_BEDS.get(arena.getOriginalName()); // prende tutti i letti dell'arena
            for (int i = 0; i < arenaBeds.length; i++) { // per ogni letto
                double[][] allCoords = arenaBeds[i];
                for (int j = 0; j < 2; j++) { // per entrambi i blocchi del letto
                    double[] possibleCoords = allCoords[j];
                    Location possibleLocation = new Location(block.getWorld(), possibleCoords[0], possibleCoords[1], possibleCoords[2]);
                    if (bedLocation.getX() == possibleLocation.getX()
                            &&  bedLocation.getY() == possibleLocation.getY()
                            &&  bedLocation.getZ() == possibleLocation.getZ()) {
                        Team team = arena.getTeam(TeamColor.values()[i]);

                        // ha rotto il suo stesso letto?
                        if (team.getPlayers().stream().map(OfflinePlayer::getName).toList().contains(player.getName())) {
                            player.sendActionBar(Component.text("Non puoi rompere il tuo stesso letto!")
                                    .color(NamedTextColor.RED));
                            event.setCancelled(true);
                            return;
                        }

                        // cambia la scoreboard
                        Arena.ScoreboardData sharedBoard = arena.getSharedBoard();
                        sharedBoard.setState(TeamState.NO_BED, team.getIndex());

                        // avverte tutti i player del team
                        for (Player p : team.getOnlinePlayers()) {
                            p.showTitle(Title.title(Component.text("LETTO DISTRUTTO!")
                                    .color(NamedTextColor.DARK_RED),
                                    Component.text("Non potrai più respawnare!")
                                            .color(NamedTextColor.RED)));
                            BWPlayer.get(p).getTeam().setTeamState(TeamState.NO_BED);
                            BWPlayer.get(p).setRejoinArena(null); // non può rejoinare
                        }

                        BWPlayer bwPlayer = BWPlayer.get(player);

                        bwPlayer.getGameStats().addBed();

                        if (arena.getAliveTeams() == 1) {
                            arena.endGame();
                        }
                        return;
                    }
                }
            }
        }

        // blocco non piazzato da un player?
        if (!block.hasMetadata(MetadataConfig.PLAYER_PLACED)) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Puoi rompere solo blocchi piazzati da un giocatore!")
                    .color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onRemoveArmor(InventoryClickEvent event) {
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBedEnter(PlayerBedEnterEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBedFailEnter(PlayerBedFailEnterEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerSetSpawnPoint(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() == Material.RED_BED && event.getAction().isRightClick()) {
            if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.AIR
            // per non settare lo spawnpoint
            || !event.getPlayer().getInventory().getItemInMainHand().getType().isBlock()
            || !event.getPlayer().isSneaking())
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (event.getEntity().getItemStack().getType() == Material.RED_BED) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerVoid(PlayerMoveEvent event) {
        if (Arena.isNotArena(event.getTo().getWorld())) return;
        // nel void?
        if (event.getTo().getY() > 0) return;

        Player player = event.getPlayer();
        BWPlayer bwPlayer = BWPlayer.get(player);

        if (bwPlayer.getArena() == null) return;

        killPlayer(player, null, DamageCause.VOID);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack dropped = event.getItemDrop().getItemStack();

        switch (dropped.getType()) {
            case WOODEN_SWORD, WOODEN_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE, DIAMOND_PICKAXE,
                    WOODEN_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE -> event.setCancelled(true);
            case STONE_SWORD, IRON_SWORD, DIAMOND_SWORD -> {
                if (!player.getInventory().contains(Material.STONE_SWORD)
                && !player.getInventory().contains(Material.IRON_SWORD)
                && !player.getInventory().contains(Material.DIAMOND_SWORD)) {
                    player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
                }
            }
            case IRON_INGOT, GOLD_INGOT, DIAMOND, EMERALD -> event.setCancelled(isAboveVoid(player.getLocation()));
        }
    }

    private static boolean isAboveVoid(Location loc) {
        for (int i = loc.getBlockY(); i >= 0; i--) {
            loc.setY(i);
            if (loc.getBlock().getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Material type = event.getItem().getItemStack().getType();

        if (type == Material.STONE_SWORD
        || type == Material.IRON_SWORD
        || type == Material.DIAMOND_SWORD) {
            player.getInventory().remove(Material.WOODEN_SWORD);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (Arena.isNotArena(player.getWorld())) return;
        if (Arena.getArena(player.getWorld()).getState() == Arena.ArenaState.STARTED) {
            killPlayer(player, null, DamageCause.SUICIDE);
        }
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        switch (event.getSpawnReason()) {
            case NATURAL, SILVERFISH_BLOCK -> event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (Arena.isNotArena(player.getWorld())) return;
        if (event.getEntity().hasMetadata("NPC")) return;

        if (event.getDamager() instanceof Fireball fireball) {
            BWPlayer.get(player).setCombatLogPlayer((Player) fireball.getShooter());
            BWPlayer.get(player).setFireballHit(fireball.getLocation());
            new BukkitRunnable() {
                @Override
                public void run() {
                    BWPlayer.get(player).setFireballHit(null);
                }
            }.runTaskLater(Bedwars.Plugin, 20 * 15);
        }
    }

    @EventHandler
    public void onPlayerKill(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (Arena.isNotArena(player.getWorld())) return;
        if (event.getFinalDamage() < player.getHealth()) return; // il player non è morto?
        if (event instanceof EntityDamageByEntityEvent) return; // per non chiamare killPlayer 2 volte
        if (event.getEntity().hasMetadata("NPC")) return;

        event.setCancelled(true);
        killPlayer(player, null, event.getCause());
    }

    @EventHandler
    public void onPlayerKill(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (Arena.isNotArena(player.getWorld())) return;
        if (event.getEntity().hasMetadata("NPC")) return;

        BWPlayer bwPlayer = BWPlayer.get(player);
        if (bwPlayer.getArena().getState() != Arena.ArenaState.STARTED) return;

        Entity killer = event.getDamager();
        if (killer instanceof Player) bwPlayer.setCombatLogPlayer((Player) killer);
        if (event.getFinalDamage() < player.getHealth()) return;

        if (killer.hasMetadata(MetadataConfig.SUMMONED_BY)) {
            killer = (Player) killer.getMetadata(MetadataConfig.SUMMONED_BY).get(0).value();
        }
        if (event.getCause() == DamageCause.ENTITY_EXPLOSION) {
            if (killer instanceof Fireball fireball) killer = (Player) fireball.getShooter();
            if (killer instanceof TNTPrimed tnt) killer = (Player) tnt.getMetadata(MetadataConfig.SUMMONED_BY).get(0).value();
        }
        event.setCancelled(true);
        killPlayer(player, killer, event.getCause());
    }

    @SuppressWarnings("DataFlowIssue")
    private void killPlayer(Player player, Entity killer, DamageCause cause) {
        // tippa il player sopra la mappa
        player.teleport(new Location(
                player.getWorld(),
                0, 128, 0
        ));
        player.setGameMode(GameMode.SPECTATOR);

        BWPlayer bwPlayer = BWPlayer.get(player);
        Arena arena = bwPlayer.getArena();
        boolean withoutBed = bwPlayer.getTeam().getTeamState() == TeamState.NO_BED; // final kill?
        boolean didQuit = cause == DamageCause.SUICIDE;
        bwPlayer.decrementTiers();

        // quello che ha fatto la kill
        BWPlayer bwKiller = killer instanceof Player ? BWPlayer.get((Player) killer) : null;
        NamedTextColor playerColor = bwPlayer.getTeam().getTextColor();
        NamedTextColor killerColor = bwKiller == null ? NamedTextColor.DARK_GRAY : bwKiller.getTeam().getTextColor();

        if (!didQuit) {
            arena.addTask(new BukkitRunnable() {
                @Override
                public void run() {
                    player.teleport(bwPlayer.getTeam().getSpawn());
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setHealth(20);
                    bwPlayer.addInitialItems();
                    bwPlayer.setCombatLogPlayer(null);
                }
            }.runTaskLater(Bedwars.Plugin, 5 * 20));
        }

        if (bwPlayer.getCombatLogPlayer() != null) {
            killer = (Entity) bwPlayer.getCombatLogPlayer();
            bwKiller = BWPlayer.get(bwPlayer.getCombatLogPlayer());
        }

        if (bwPlayer.getFireballHit() != null && isAboveVoid(bwPlayer.getFireballHit())) {
            BWPlayer.get((OfflinePlayer) killer).getAchievements().setAchievement(Achievements.FIREBALL_BRIDGE);
        }

        if (bwKiller != null) {
            int iron = 0, gold = 0, diamond = 0, emerald = 0;
            for (ItemStack stack : player.getInventory()) {
                if (stack == null) continue;
                switch (stack.getType()) {
                    case IRON_INGOT -> iron += stack.getAmount();
                    case GOLD_INGOT -> gold += stack.getAmount();
                    case DIAMOND -> diamond += stack.getAmount();
                    case EMERALD -> emerald += stack.getAmount();
                }
            }
            messageQuantity(killer, Material.IRON_INGOT, iron);
            messageQuantity(killer, Material.GOLD_INGOT, gold);
            messageQuantity(killer, Material.DIAMOND, diamond);
            messageQuantity(killer, Material.EMERALD, emerald);

            bwPlayer.setCombatLogPlayer((Player) killer);

            bwPlayer.getGameStats().addDeath();
            bwKiller.getGameStats().addKill();
            if (withoutBed) {
                bwKiller.getGameStats().addFinal();
            }
        }
        player.getInventory().clear();

        TextComponent deathMessage = switch (cause) {
            case ENTITY_ATTACK -> Component.text(player.getName()) // 1v1 normale
                    .color(playerColor)
                    .append(Component.text(" è stato ucciso da ")
                            .color(NamedTextColor.GRAY))
                    .append(Component.text(killer.getName())
                            .color(killerColor))
                    .append(Component.text(".")
                            .color(NamedTextColor.GRAY));
            case FALL -> { // fall damage
                if (bwPlayer.getCombatLogPlayer() == null) { // morto da solo?
                    yield basicSelfDeathMessage(bwPlayer, " si è spaccato le gambe.");
                } else { // morto durante un fight?
                    yield Component.text(player.getName())
                            .color(playerColor)
                            .append(Component.text(" si è spaccato le gambe cercando di scappare da ")
                                    .color(NamedTextColor.GRAY))
                            .append(Component.text(bwPlayer.getCombatLogPlayer().getName())
                                    .color(BWPlayer.get(bwPlayer.getCombatLogPlayer()).getTeam().getTextColor()))
                            .append(Component.text(".")
                                    .color(NamedTextColor.GRAY));
                }
            }
            case PROJECTILE -> { // freccia
                Player shooter;
                if (killer instanceof Arrow arrow) {
                    shooter = (Player) arrow.getShooter();
                } else if (killer instanceof Fireball fireball) {
                    shooter = (Player) fireball.getShooter();
                } else {
                    throw new IllegalStateException("BedwarsListener:killPlayer - player morto da un proiettile diverso da Arrow o Fireball.");
                }

                @SuppressWarnings("DataFlowIssue")
                BWPlayer bwShooter = BWPlayer.get(shooter);
                NamedTextColor color = bwShooter.getTeam().getTextColor();

                yield Component.text(player.getName())
                        .color(playerColor)
                        .append(killer instanceof Arrow
                                ? Component.text(" è stato sparato da ")
                                : Component.text(" è stato fatto esplodere da ")
                                .color(NamedTextColor.GRAY))
                        .append(Component.text(shooter.getName())
                                .color(color))
                        .append(Component.text(".")
                                .color(NamedTextColor.GRAY));
            }
            case VOID -> { // sotto Y:0
                if (bwPlayer.getCombatLogPlayer() == null) {
                    yield basicSelfDeathMessage(bwPlayer, " è caduto nel vuoto.");
                } else {
                    yield Component.text(player.getName())
                            .color(playerColor)
                            .append(Component.text(" si è buttato nel vuoto cercando di scappare da ")
                                    .color(NamedTextColor.GRAY))
                            .append(Component.text(bwPlayer.getCombatLogPlayer().getName())
                                    .color(BWPlayer.get(bwPlayer.getCombatLogPlayer()).getTeam().getTextColor()))
                            .append(Component.text(".")
                                    .color(NamedTextColor.GRAY));
                }
            }
            case SUICIDE -> { // disconnesso?
                if (bwPlayer.getCombatLogPlayer() == null) {
                    yield basicSelfDeathMessage(bwPlayer, " si è disconnesso.");
                } else {
                    yield Component.text(player.getName())
                            .color(playerColor)
                            .append(Component.text(" si è disconnesso provando a scappare da ")
                                    .color(NamedTextColor.GRAY))
                            .append(Component.text(bwPlayer.getCombatLogPlayer().getName())
                                    .color(BWPlayer.get(bwPlayer.getCombatLogPlayer()).getTeam().getTextColor()))
                            .append(Component.text(".")
                                    .color(NamedTextColor.GRAY));
                }
            }
            case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> {
                if (bwPlayer.getCombatLogPlayer() == null || player.getName().equals(bwPlayer.getCombatLogPlayer().getName())) {
                    yield basicSelfDeathMessage(bwPlayer, " è esploso.");
                } else {
                    yield Component.text(player.getName())
                            .color(playerColor)
                            .append(Component.text(" è stato fatto esplodere da ")
                                    .color(NamedTextColor.GRAY))
                            .append(Component.text(bwPlayer.getCombatLogPlayer().getName())
                                    .color(BWPlayer.get(bwPlayer.getCombatLogPlayer()).getTeam().getTextColor()))
                            .append(Component.text(".")
                                    .color(NamedTextColor.GRAY));
                }
            }
            default -> Component.text("non aggiunto");
        };

        Arena.getArena(player.getWorld()).getWorld().getPlayers().forEach(p -> p.sendMessage(deathMessage));

        if (bwPlayer.getTeam().getTeamState() == TeamState.NO_BED) {
            player.showTitle(Title.title(Component.text("HAI PERSO!")
                            .color(NamedTextColor.RED),
                    Component.text("Vuoi fare un'altra partita?")
                            .color(NamedTextColor.WHITE)));
            bwPlayer.getTeam().removePlayer(player);
            if (arena.getAliveTeams() == 1) { // game finito?
                arena.endGame();
            }
        } else if (!didQuit) {
            arena.addTask(new BukkitRunnable() {
                int countdown = 5;

                @Override
                public void run() {
                    if (countdown == 1) {
                        cancel();
                    }
                    player.showTitle(Title.title(Component.text("Sei morto!")
                                    .color(NamedTextColor.DARK_RED),
                            Component.text(String.format("Tornerai in vita tra %d secondi...", countdown--))
                                    .color(NamedTextColor.RED),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)));
                }
            }.runTaskTimer(Bedwars.Plugin, 0, 20));
        }
    }

    private static TextComponent basicSelfDeathMessage(BWPlayer player, String baseMessage) {
        //noinspection DataFlowIssue
        return Component.text(player.getPlayer().getName())
                .color(player.getTeam().getTextColor())
                .append(Component.text(baseMessage)
                        .color(NamedTextColor.GRAY));
    }

    private static void messageQuantity(Entity player, Material m, int count) {
        if (count > 0) {
            player.sendMessage(Component.text(String.format(
                    "+%d %s", count, materialName(m, count)
            )).color(materialColor(m)));
            ((Player) player).getInventory().addItem(new ItemStack(m, count));
        }
    }

    private static String materialName(Material m, int count) {
        return switch (m) {
            case IRON_INGOT -> "Ferro";
            case GOLD_INGOT -> "Oro";
            case DIAMOND -> count == 1 ? "Diamante" : "Diamanti";
            case EMERALD -> count == 1 ? "Smeraldo" : "Smeraldi";
            default -> throw new IllegalArgumentException();
        };
    }

    private static NamedTextColor materialColor(Material m) {
        return switch (m) {
            case IRON_INGOT -> NamedTextColor.WHITE;
            case GOLD_INGOT -> NamedTextColor.YELLOW;
            case DIAMOND -> NamedTextColor.AQUA;
            case EMERALD -> NamedTextColor.DARK_GREEN;
            default -> throw new IllegalArgumentException();
        };
    }
}
