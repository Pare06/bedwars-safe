package org.bedwars.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bedwars.Bedwars;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BWScoreboard {
    private static final Scoreboard EMPTY_SCOREBOARD = Bukkit.getScoreboardManager().getNewScoreboard();

    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Player player;
    private boolean isEnabled;
    private final Map<Integer, String> scores;

    public BWScoreboard(Player player) {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective("name", Criteria.DUMMY, Component.text("BEDWARS")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        isEnabled = false;
        this.player = player;
        scores = new HashMap<>();
    }

    public static void setEmptyScoreboard() {
        Objective objective = EMPTY_SCOREBOARD.registerNewObjective("emptyObj", Criteria.DUMMY, Component.empty());
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    @SuppressWarnings("DataFlowIssue")
    private void update() {
        for (String score : objective.getScoreboard().getEntries()) {
            scoreboard.resetScores(score);
        }
        for (int i = 0; i < scores.size(); i++) {
            setLine(scores.get(i), i);
        }
    }

    public void loadScores(List<String> scoresList) {
        scores.clear();
        Collections.reverse(scoresList);
        for (int i = 0; i < scoresList.size(); i++) {
            scores.put(i, scoresList.get(i));
        }
    }

    public void enable() {
        isEnabled = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isEnabled) {
                    cancel();
                }
                update();
                player.setScoreboard(isEnabled ? scoreboard : EMPTY_SCOREBOARD);
            }
        }.runTaskTimer(Bedwars.Plugin, 0, 5); // 0.25s a ogni aggiornamento
    }

    public void disable() {
        isEnabled = false;
    }

    public void set(String text, int index) {
        scores.put(index, text);
    }

    private void setLine(String text, int index) {
        objective.getScore(text).setScore(index);
    }
}
