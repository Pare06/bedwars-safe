package org.bedwars.game;

import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bedwars.Bedwars;
import org.bedwars.config.MetadataConfig;
import org.bedwars.config.NPCConfig;
import org.bedwars.general.listeners.GeneralListener;
import org.bedwars.lobby.LobbyInterface;
import org.bedwars.npc.traits.UpgradeShop;
import org.bedwars.npc.traits.BaseShop;
import org.bedwars.utils.BWPlayer;
import org.bedwars.utils.BWScoreboard;
import org.bedwars.utils.Hologram;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.checkerframework.checker.units.qual.C;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.bedwars.config.ArenaConfig.*;
import static org.bedwars.game.listeners.LoadingListener.woolNames;

public class Arena {
    private final String worldName; // modalita_nome_*
    private final String originalName; // modalita_nome
    private final List<OfflinePlayer> players;
    private final int maxPlayers;
    private final String modeName; // solo/doubles/trio/4v4
    private final List<Team> teams;
    private ArenaState state;
    private int countdown; // per l'inizio del game
    private boolean starting;
    private final World world;
    private ScoreboardData sharedBoard;
    private ArenaUpgrades upgrades;
    private int nextUpgradeCooldown;
    private int diamondCooldown;
    private int diamondCdRemaining; // quanto manca?
    private int emeraldCooldown;
    private int emeraldCdRemaining;
    private final List<BukkitTask> tasks; // tasks da fermare quando finisce il game
    private final List<NPC> npcs; // shop
    private final Arena thisArena; // per i BukkitRunnable

    public Arena(String world, String original) {
        worldName = world;
        originalName = original;
        players = new ArrayList<>();
        countdown = 30;
        this.world = Bukkit.getWorld(worldName);
        diamondCooldown = DIAMOND_COOLDOWN;
        emeraldCooldown = EMERALD_COOLDOWN;
        diamondCdRemaining = diamondCooldown;
        emeraldCdRemaining = emeraldCooldown;
        upgrades = ArenaUpgrades.JUST_STARTED;
        nextUpgradeCooldown = 0;
        tasks = new ArrayList<>();
        npcs = new ArrayList<>();

        int modeInt;
        switch (worldName.substring(0, 3)) {
            case "sol" -> {
                maxPlayers = 8;
                modeName = "Solo";
                modeInt = 1;
            }
            case "duo" -> {
                maxPlayers = 16;
                modeName = "Doubles";
                modeInt = 2;
            }
            case "tri" -> {
                maxPlayers = 12;
                modeName = "Trio";
                modeInt = 3;
            }
            case "4v4" -> {
                maxPlayers = 16;
                modeName = "4v4";
                modeInt = 4;
            }
            default -> throw new IllegalStateException("Il mondo dell'arena non contiene il tipo dei team.");
        }

        teams = new ArrayList<>();
        for (int i = 0; i < TeamColor.values().length; i++) {
            // crea i team vuoti
            teams.add(new Team(TeamColor.values()[i], modeInt, toLocation(ARENA_SPAWNS.get(originalName).get(i))));
        }
        state = ArenaState.WAITING;

        thisArena = this;
    }

    public static Arena getArena(World world) {
        for (Arena arena : ArenaLoader.getArenas()) {
            if (world.getName().equals(arena.getWorldName())) {
                return arena;
            }
        }
        throw new NullPointerException(String.format("L'arena %s non esiste.", world.getName()));
    }

    public void teleportPlayer(Player player) {
        World world = Bukkit.getWorld(worldName);
        double[] coords = SPAWN_COORDS.get(originalName);
        player.teleport(new Location(world, coords[0], coords[1], coords[2]));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // findFirst() troverà sempre un team
    public Team getTeam(TeamColor color) {
        return teams.stream().filter(t -> t.getColor() == color).findFirst().get();
    }

    public World getWorld() {
        return world;
    }

    public String getWorldName() {
        return worldName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public List<Player> getOnlinePlayers() {
        return players.stream().filter(OfflinePlayer::isOnline).map(OfflinePlayer::getPlayer).toList();
    }

    public int getNPlayers() {
        return players.size();
    }

    public void addPlayer(OfflinePlayer player) {
        // non posso fare if players.contains(player) perchè OfflinePlayer è una classe perfetta, senza errori nè problemi di design.
        if (players.stream().noneMatch(p -> Objects.equals(p.getName(), player.getName()))) {
            players.add(player);
        }
    }

    public void removePlayer(OfflinePlayer player) {
        players.removeIf(p -> Objects.equals(p.getName(), player.getName()));
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public String getModeName() {
        return modeName;
    }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
        if (state == ArenaState.WAITING) starting = false;
    }

    public void setDiamondCooldown(int cd) {
        diamondCooldown = cd;
    }

    public void setEmeraldCooldown(int cd) {
        emeraldCooldown = cd;
    }

    public static boolean isNotArena(World world) {
        return !isArena(world.getName());
    }

    private static boolean isArena(String s) {
        return s.startsWith("solo")
                || s.startsWith("duo")
                || s.startsWith("trio")
                || s.startsWith("4v4");
    }

    public int getCountdown() {
        return countdown;
    }

    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }

    public ScoreboardData getSharedBoard() {
        return sharedBoard;
    }

    public long getAliveTeams() {
        return teams.stream().filter(t -> !t.isEmpty()).count();
    }

    public void reassignPlayer(Player p) {
        for (Team t : teams) {
            if (t.containsPlayer(p)) {
                t.addPlayer(p);
            }
        }
    }

    public void start(Player player) {
        if (!starting) {
            state = ArenaState.STARTING;

            starting = true;
            countdown = 30;

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) cancel();
                    if (countdown <= 0) {
                        startGame();
                        cancel();
                    } else {
                        countdown--;
                    }
                }
            }.runTaskTimer(Bedwars.Plugin, 20, 20);
        }
    }

    public void startGame() {
        if (state == ArenaState.STARTED) return;
        state = ArenaState.STARTED;

        players.forEach(p -> BWPlayer.get(p).setRejoinArena(this));

        List<Player> withoutTeam = new ArrayList<>(getOnlinePlayers().stream()
                .filter(p -> BWPlayer.get(p).getTeam() == null).toList()); // rimangono solo quelli senza team
        List<Team> notFull = new ArrayList<>(teams.stream()
                .filter(t -> t.getPlayers().size() != t.getMaxPlayers()).toList()); // rimangono quelli non pieni
        notFull.sort(Comparator.comparing(Team::getNPlayers)); // ordinati per n. di player

        assignTeams(notFull, withoutTeam);

        // i team senza player volano via
        teams.stream().filter(Team::isEmpty).forEach(t -> t.setTeamState(TeamState.ELIMINATED));

        // tippa tutti i player al loro spawn
        List<XYZRotation> spawns = ARENA_SPAWNS.get(originalName);
        for (Team team : teams) {
            for (Player p : team.getOnlinePlayers()) {
                XYZRotation coords = spawns.get(teams.indexOf(team));
                // Debug.printObjectInfo(p, p.getName());

                p.teleport(toLocation(coords));
            }
        }
        initializePlayers();

        // inizializza gli spawner
        teams.stream().filter(t -> !t.isEmpty()).forEach(t -> t.setTasks(originalName, world));

        // spawna tutti i villager
        List<XYZRotation> shopSpawns = SHOP_LOCATIONS.get(originalName);
        List<XYZRotation> upgradeSpawns = UPGRADE_LOCATION.get(originalName);
        for (int i = 0; i < teams.size(); i++) {
            if (!teams.get(i).isEmpty()) {
                // TODO trovare delle skin decenti per gli npc

                NPC villager = Bedwars.npcRegistry.createNPC(EntityType.PLAYER, NPCConfig.SHOP_NAME);
                villager.setProtected(true);
                villager.getOrAddTrait(BaseShop.class);
                villager.spawn(toLocation(shopSpawns.get(i)));

                NPC upgradeVillager = Bedwars.npcRegistry.createNPC(EntityType.PLAYER, NPCConfig.UPGRADE_NAME);
                upgradeVillager.setProtected(true);
                upgradeVillager.getOrAddTrait(UpgradeShop.class);
                upgradeVillager.spawn(toLocation(upgradeSpawns.get(i)));

                npcs.add(villager);
                npcs.add(upgradeVillager);
            }
        }

        // generatori + ologrammi
        startGen(Material.DIAMOND, diamondCooldown);
        startGen(Material.EMERALD, emeraldCooldown);
        for (XYZCoords spawner : DIAMOND_SPAWNERS.get(originalName)) {
            Hologram.placeHologram(toLocation(spawner), MetadataConfig.DIAMOND_SPAWNER, world, Component.text(""));
        }
        for (XYZCoords spawner : EMERALD_SPAWNERS.get(originalName)) {
            Hologram.placeHologram(toLocation(spawner), MetadataConfig.EMERALD_SPAWNER, world, Component.text(""));
        }
        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                diamondCdRemaining -= 20;
                emeraldCdRemaining -= 20;
                for (ArmorStand stand : Hologram.getHolograms(MetadataConfig.DIAMOND_SPAWNER, world)) {
                    stand.customName(Component.text("Spawna in ")
                            .color(NamedTextColor.WHITE)
                            .append(Component.text(diamondCdRemaining / 20)
                                    .color(NamedTextColor.BLUE))
                            .append(Component.text(" secondi")
                                    .color(NamedTextColor.WHITE)));
                }
                for (ArmorStand stand : Hologram.getHolograms(MetadataConfig.EMERALD_SPAWNER, world)) {
                    stand.customName(Component.text("Spawna in ")
                            .color(NamedTextColor.WHITE)
                            .append(Component.text(emeraldCdRemaining / 20)
                                    .color(NamedTextColor.GREEN))
                            .append(Component.text(" secondi")
                                    .color(NamedTextColor.WHITE)));
                }
            }
        }.runTaskTimer(Bedwars.Plugin, 0, 20));

        // rompe tutti i letti nelle isole senza team
        double[][][] allBeds = ARENA_BEDS.get(originalName);
        for (int i = 0; i < teams.size(); i++) {
            if (teams.get(i).isEmpty()) {
                for (int j = 0; j < 2; j++) { // per entrambi i blocchi del letto
                    Location bed = new Location(
                            world,
                            allBeds[i][j][0], // x
                            allBeds[i][j][1], // y
                            allBeds[i][j][2]  // z
                    );
                    world.getBlockAt(bed).setType(Material.AIR, false); // scoppia
                }
            }
        }

        // inizializza la scoreboard condivisa
        sharedBoard = new ScoreboardData();
        for (Player p : getOnlinePlayers()) {
            BWScoreboard scoreboard = new BWScoreboard(p);
            scoreboard.loadScores(sharedBoard.getScores());
            BWPlayer.get(p).setScoreboard(scoreboard);

            tasks.add(new BukkitRunnable() {
                @Override
                public void run() {
                    scoreboard.loadScores(sharedBoard.getScores());
                }
            }.runTaskTimer(Bedwars.Plugin, 0, 5));
        }

        // upgrades
        nextUpgrade(ArenaUpgrades.JUST_STARTED);
        decreaseCooldown();
    }

    private void assignTeams(List<Team> notFull, List<Player> withoutTeam) {
        for (Team team : notFull) {
            if (withoutTeam.isEmpty()) break;
            while (!team.isFull()) {
                if (withoutTeam.isEmpty()) break;
                Player player = randomItem(withoutTeam);
                BWPlayer bwPlayer = BWPlayer.get(player);
                bwPlayer.setTeam(team);
                withoutTeam.remove(player);
            }
        }
    }

    private void initializePlayers() {
        for (Player p : getOnlinePlayers()) {
            // inventario
            BWPlayer bwPlayer = BWPlayer.get(p);
            bwPlayer.setArena(this);
            p.getInventory().clear();
            bwPlayer.addInitialItems();
        }
    }

    private void nextUpgrade(ArenaUpgrades upgrade) {
        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                ARENA_UPGRADES.get(upgrade).apply(thisArena);

                int index = List.of(ArenaUpgrades.values()).indexOf(upgrade);

                try {
                    // prossimo upgrade
                    ArenaUpgrades next = ArenaUpgrades.values()[index + 1];
                    upgrades = next;
                    nextUpgrade(next);
                } catch (ArrayIndexOutOfBoundsException ex) {
                    // upgrade finiti
                    // TODO
                }
            }
        }.runTaskLater(Bedwars.Plugin, UPGRADE_COOLDOWNS.get(upgrade)));
    }

    private void decreaseCooldown() {
        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                nextUpgradeCooldown--;

                try {
                    if (nextUpgradeCooldown <= 0) {
                        nextUpgradeCooldown = UPGRADE_COOLDOWNS.get(ArenaUpgrades.values()[upgrades.ordinal() + 1]);
                    }
                } catch (NullPointerException ex) {
                    // upgrade finiti
                    // TODO
                }
            }
        }.runTaskTimer(Bedwars.Plugin, 0, 1));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void endGame() {
        state = ArenaState.ENDED;

        // cancella tutte le task
        tasks.forEach(BukkitTask::cancel);

        ArrayList<String> winners = new ArrayList<>();

        // quando finisce il game c'è solo un team rimasto - non importa controllare se teams ha dei team non vuoti
        for (Player p : teams.stream().filter(t -> !t.isEmpty()).findFirst().get().getOnlinePlayers()) {
            p.showTitle(Title.title(Component.text("HAI VINTO!")
                            .color(NamedTextColor.RED),
                    Component.text("Vuoi fare un'altra partita?")
                            .color(NamedTextColor.WHITE)));
            BWPlayer.get(p).getGameStats().addWin();
            winners.add(p.getName());
        }

        for (OfflinePlayer p : players) {
            if (!winners.contains(p.getName())) {
                BWPlayer.get(p).getGameStats().addLoss();
            }
        }

        // tippa via tutti dopo 5 secondi
        new BukkitRunnable() {
            @Override
            public void run() {
                //noinspection DataFlowIssue (world non è null)
                for (Player p : world.getPlayers()) {
                    BWPlayer bwPlayer = BWPlayer.get(p);

                    bwPlayer.setArena(null);
                    bwPlayer.setRejoinArena(null);
                    bwPlayer.setTeam(null);
                    bwPlayer.resetTiers();

                    GeneralListener.teleportSpawn(p);
                    LobbyInterface.sendLobbyScoreboard(p);
                }
                npcs.forEach(NPC::destroy);
                teams.forEach(Team::clear);

                Bukkit.unloadWorld(world, false);
                ArenaLoader.deleteArena(thisArena);
            }
        }.runTaskLater(Bedwars.Plugin, 5 * 20);
    }

    public void addTask(BukkitTask task) {
        tasks.add(task);
    }

    private static String formatTime(int ticks) {
        ticks /= 20; // tick -> secondi
        int seconds = ticks % 60;
        // per fare i secondi sempre a 2 cifre
        return String.format("%d:%s", ticks / 60, (seconds < 10) ? "0" + seconds : seconds);
    }

    private void startGen(Material m, int cd) {
        tasks.add(new BukkitRunnable() {
            @Override
            public void run() {
                dropMaterial(m);
                if (m == Material.DIAMOND) diamondCdRemaining = diamondCooldown;
                else emeraldCdRemaining = emeraldCooldown;
                startGen(m, m == Material.DIAMOND ? diamondCooldown : emeraldCooldown);
            }
        }.runTaskLater(Bedwars.Plugin, cd));
    }

    private void dropMaterial(Material m) {
        Map<String, List<XYZCoords>> spawnerList = m == Material.DIAMOND
                ? DIAMOND_SPAWNERS
                : EMERALD_SPAWNERS;
        for (XYZCoords spawner : spawnerList.get(originalName)) {
            Location loc = new Location(
                    world,
                    spawner.x(),
                    spawner.y(),
                    spawner.z()
            );

            int nearbyOres = (int) loc.getNearbyEntities(1, 5, 1).stream() // tutte le entità a 5 blocchi Y di distanza
                    .filter(e -> e.hasMetadata(MetadataConfig.GEN_SPAWNED)) // spawnate dal gen
                    .filter(e -> ((Item) e).getItemStack().getType() == m) // dello stesso materiale da droppare
                    .count();

            // limite max. di ore spawnati?
            if (nearbyOres >= (m == Material.DIAMOND
                    ? MAX_DIAMONDS_SPAWNED
                    : MAX_EMERALDS_SPAWNED)) {
                break;
            }

            Item item = world.dropItem(loc, new ItemStack(m));
            item.setVelocity(new Vector(0, 0, 0));
            item.setMetadata(MetadataConfig.GEN_SPAWNED, new FixedMetadataValue(Bedwars.Plugin, true));
            item.setUnlimitedLifetime(true);
        }
    }

    private static <E> E randomItem(List<E> list) {
        return list.get(new Random().nextInt(list.size()));
    }

    private Location toLocation(XYZCoords coords) {
        return new Location(
                world,
                coords.x(),
                coords.y(),
                coords.z()
        );
    }

    private static Location toLocation(XYZRotation coords, World world) {
        return new Location(
                world,
                coords.x(),
                coords.y(),
                coords.z(),
                (float) coords.yaw(),
                (float) coords.pitch()
        );
    }

    private Location toLocation(XYZRotation coords) {
        return toLocation(coords, world);
    }

    public enum ArenaState {
        WAITING,
        STARTING,
        STARTED,
        ENDED
    }

    public class ScoreboardData {
        private final LocalDateTime today;
        private final List<TeamState> states;

        public ScoreboardData() {
            today = LocalDateTime.now();
            states = new ArrayList<>();
            loadStates();
        }

        public void loadStates() {
            states.clear();
            for (Team t : teams) {
                states.add(t.getTeamState());
            }
        }

        @SuppressWarnings("deprecation") // perchè sulla scoreboard Component non funziona
        public List<String> getScores() {
            List<String> scores = new ArrayList<>();
            scores.add(ChatColor.GRAY + today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            scores.add("");
            // quanto manca al prossimo upgrade?
            scores.add(String.format(UPGRADE_STRINGS.get(thisArena.upgrades), formatTime(nextUpgradeCooldown)));
            scores.add(" ");

            for (int i = 0; i < states.size(); i++) {
                StringBuilder string = new StringBuilder(woolNames[i]);
                string.setCharAt(0, Character.toUpperCase(string.charAt(0)));
                scores.add(String.format("%s%s: %s", woolColors[i], string, woolColors[i] + getStateString(states.get(i), i)));
            }

            scores.add("  ");
            scores.add("mc.epiccity.it");
            return scores;
        }

        @SuppressWarnings("deprecation") // leggi sopra
        private static final ChatColor[] woolColors = {
            ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, ChatColor.YELLOW,
            ChatColor.AQUA, ChatColor.WHITE, ChatColor.LIGHT_PURPLE, ChatColor.GRAY
        };

        private String getStateString(TeamState state, int index) {
            // TODO: simboli più umani (https://imgur.com/a/jFpKFO2)
            return switch (state) {
                case WITH_BED -> "✓";
                case NO_BED -> String.valueOf(teams.get(index).getPlayers().size());
                case ELIMINATED -> "✘";
            };
        }

        public void setState(TeamState state, int index) {
            states.set(index, state);
        }
    }
}
