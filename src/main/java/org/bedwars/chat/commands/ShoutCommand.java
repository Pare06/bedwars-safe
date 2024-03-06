package org.bedwars.chat.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bedwars.chat.listeners.ChatListener;
import org.bedwars.game.Arena;
import org.bedwars.utils.BWPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShoutCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (commandSender instanceof ConsoleCommandSender) {
            Bukkit.getLogger().info("Non puoi eseguire questo comando da console!");
            return true;
        }

        Player player = (Player) commandSender;
        BWPlayer bwPlayer = BWPlayer.get(player);
        String message = String.join(" ", strings);

        if (bwPlayer.getArena() == null) {
            player.sendMessage(Component.text("Non sei in una partita!").color(NamedTextColor.RED));
        } else if (bwPlayer.getArena().getState() != Arena.ArenaState.STARTED
                && bwPlayer.getArena().getState() != Arena.ArenaState.ENDED) {
            player.sendMessage(Component.text("La partita non è cominciata!").color(NamedTextColor.RED));
        } else if (strings.length == 0) {
            player.sendMessage(Component.text("Inserisci un messaggio!").color(NamedTextColor.RED));
        } else {
            player.getWorld().getPlayers().forEach(p -> p.sendMessage(ChatListener.buildMessage(player, message, true, true, true)));
        }

        return true;
    }
}
