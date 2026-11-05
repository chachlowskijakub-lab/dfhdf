package com.qubiszonnn.tpahelper;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Wykrywa wiadomości graczy (podpisany czat oraz czat serwerowy wysyłany jako wiadomość systemowa). */
public final class ChatListener {
    private static final Pattern WORD = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_.]{1,20}");

    private ChatListener() {}

    public static void register() {
        // Standardowy, podpisany czat gracza – nick pochodzi bezpośrednio od nadawcy.
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            String text = message.getString();
            String name = sender != null ? sender.name() : extractNick(text);
            handle(name, text);
        });

        // Serwery z pluginami czatu często wysyłają wiadomości jako "game message".
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            String text = message.getString();
            handle(extractNick(text), text);
        });
    }

    private static void handle(String name, String text) {
        if (name == null || !VALID_NAME.matcher(name).matches()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && name.equals(client.player.getName().getString())) return;
        ChatTracker.record(name, text);
    }

    /** Szuka nicku online gracza w części wiadomości przed pierwszym separatorem (: » > ›). */
    private static String extractNick(String text) {
        int cut = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ':' || c == '»' || c == '>' || c == '›') {
                cut = i;
                break;
            }
        }
        if (cut <= 0) return null;

        Set<String> online = onlineNames();
        if (online.isEmpty()) return null;

        String result = null;
        Matcher m = WORD.matcher(text.substring(0, cut));
        while (m.find()) {
            if (online.contains(m.group())) result = m.group();
        }
        return result;
    }

    private static Set<String> onlineNames() {
        Set<String> names = new HashSet<>();
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) return names;
        for (PlayerListEntry entry : handler.getPlayerList()) {
            names.add(entry.getProfile().name());
        }
        return names;
    }
}
