package org.bedwars.utils;

import net.kyori.adventure.text.TextComponent;
import org.bedwars.Bedwars;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;

public class Hologram {
    private Hologram() {}

    private static final List<ArmorStand> allHolograms = new ArrayList<>();

    public static void placeHologram(Location loc, String name, World world, TextComponent text) {
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.setGravity(false);
        stand.setCanPickupItems(false);
        stand.customName(text);
        stand.setCustomNameVisible(true);
        stand.setVisible(false);

        stand.setMetadata(name, new FixedMetadataValue(Bedwars.Plugin, world));
        allHolograms.add(stand);
    }

    public static List<ArmorStand> getHolograms(String name, World world) {
        return allHolograms.stream()
                .filter(s -> s.hasMetadata(name))
                .filter(s -> s.getMetadata(name).get(0).value() == world).toList();
    }
}
