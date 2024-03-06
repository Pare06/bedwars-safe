package org.bedwars.game.listeners;

import org.bedwars.Bedwars;
import org.bedwars.config.MetadataConfig;
import org.bedwars.game.Arena;
import org.bedwars.utils.BWPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class ItemListener implements Listener {
    // 1° location: blocco (nullable), 2[0]: player
    private final static Map<Material, BiFunction<Location, List<Object>, Boolean>> ITEM_EFFECTS = new HashMap<>();

    public static void initialize() {
        ITEM_EFFECTS.put(Material.TNT, (blockLoc, list) -> {
            if (blockLoc != null) {
                TNTPrimed tnt = spawn(blockLoc, TNTPrimed.class);
                tnt.setFuseTicks(2 * 20);
                tnt.setMetadata(MetadataConfig.SUMMONED_BY, new FixedMetadataValue(Bedwars.Plugin, list.get(0)));
            }
            return blockLoc != null;
        });
        ITEM_EFFECTS.put(Material.FIRE_CHARGE, (blockLoc, list) -> {
            Player shooter = (Player) list.get(0);
            Location playerLoc = shooter.getLocation();
            Vector playerVec = playerLoc.getDirection().clone();
            playerVec.multiply(1.5);
            playerVec.setY(0);
            Location fireballLoc = playerLoc.add(0, 1, 0).add(playerVec);

            Fireball fireball = spawn(fireballLoc, Fireball.class);
            fireball.setRotation(playerLoc.getYaw(), playerLoc.getPitch());
            fireball.setYield(fireball.getYield() * 2);
            fireball.setShooter(shooter);
            return true;
        });
    }

    private static <E extends Entity> E spawn(Location l, Class<E> clazz) {
        return l.getWorld().spawn(l, clazz);
    }

    @EventHandler
    public void onItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        BWPlayer bwPlayer = BWPlayer.get(player);

        if (bwPlayer.getArena() == null || bwPlayer.getArena().getState() != Arena.ArenaState.STARTED) return;

        Material type = player.getInventory().getItemInMainHand().getType();
        BiFunction<Location, List<Object>, Boolean> effect = ITEM_EFFECTS.get(type);

        if (effect != null) {
            boolean result = effect.apply(event.getInteractionPoint(), List.of(player));
            if (result) {
                event.setCancelled(true);
                player.getInventory().removeItem(new ItemStack(type, 1));
            }
        }
    }

    @EventHandler
    public void onSnowballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (Arena.isNotArena(snowball.getLocation().getWorld())) return;
        Player player = (Player) snowball.getShooter();
        //noinspection DataFlowIssue
        BWPlayer bwPlayer = BWPlayer.get(player);

        Silverfish silverfish = spawn(snowball.getLocation(), Silverfish.class);
        silverfish.setMetadata(MetadataConfig.SUMMONED_BY, new FixedMetadataValue(Bedwars.Plugin, player));
        silverfish.customName(player.name().color(bwPlayer.getTeam().getTextColor()));
    }

    @EventHandler
    public void onTntFuse(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> !b.hasMetadata(MetadataConfig.PLAYER_PLACED));
    }

    @EventHandler
    public void onBlockBurn(BlockIgniteEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onSilverfishAttack(EntityTargetLivingEntityEvent event) {
        if (event.getEntityType() == EntityType.SILVERFISH) {
            if (event.getTarget() == event.getEntity().getMetadata(MetadataConfig.SUMMONED_BY).get(0).value()) {
                event.setCancelled(true);
            }
        }
    }
}
