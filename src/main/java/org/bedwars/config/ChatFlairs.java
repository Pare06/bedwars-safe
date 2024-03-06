package org.bedwars.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bedwars.stats.ChatFlair;

public class ChatFlairs {
    private ChatFlairs() { }

    public static void initializeFlairs() {
        new ChatFlair(Component.text("Nessun flair"));
        new ChatFlair(Component.text("[TEST]").color(NamedTextColor.YELLOW), Component.text("flair test"));
        new ChatFlair(Component.text("[TEST 2]").color(NamedTextColor.RED), Component.text("flair test 2"));
    }
}
