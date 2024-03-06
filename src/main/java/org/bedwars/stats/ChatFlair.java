package org.bedwars.stats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

public class ChatFlair {
    private static final List<ChatFlair> flairs = new ArrayList<>();
    private static int NEXT_ID = 0;
    private final TextComponent chatView;
    private final TextComponent description;
    private final int id;

    public ChatFlair(TextComponent chatView) {
        this(chatView, Component.empty());
    }

    public ChatFlair(TextComponent chatView, TextComponent description) {
        this.chatView = chatView;
        this.description = description;
        this.id = NEXT_ID++;
        flairs.add(this);
    }

    public TextComponent getChatView() {
        return (chatView.content().equals("Nessun flair") ? Component.empty() : chatView.append(Component.text(" "))).decoration(TextDecoration.ITALIC, false);
    }

    public TextComponent getGUIView() {
        return chatView;
    }

    public TextComponent getDescription() {
        return description.decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE);
    }

    public int getId() {
        return id;
    }

    public static ChatFlair getFlair(int id) {
        return flairs.get(id);
    }

    public static int getFlairNumber() {
        return flairs.size();
    }
}
