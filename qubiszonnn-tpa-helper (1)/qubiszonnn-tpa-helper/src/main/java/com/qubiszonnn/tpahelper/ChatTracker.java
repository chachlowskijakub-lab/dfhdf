package com.qubiszonnn.tpahelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/** Przechowuje graczy, którzy napisali coś na czacie (najnowsi pierwsi). */
public final class ChatTracker {
    public record Entry(String name, String lastMessage, long time) {}

    private static final int MAX_ENTRIES = 200;
    private static final LinkedHashMap<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static int version = 0;

    private ChatTracker() {}

    public static synchronized void record(String name, String message) {
        String msg = message == null ? "" : message.replace('\n', ' ').trim();
        if (msg.length() > 120) msg = msg.substring(0, 120);
        ENTRIES.remove(name);
        ENTRIES.put(name, new Entry(name, msg, System.currentTimeMillis()));
        while (ENTRIES.size() > MAX_ENTRIES) {
            Iterator<String> it = ENTRIES.keySet().iterator();
            it.next();
            it.remove();
        }
        version++;
    }

    public static synchronized List<Entry> snapshot() {
        List<Entry> list = new ArrayList<>(ENTRIES.values());
        Collections.reverse(list);
        return list;
    }

    public static synchronized void clear() {
        ENTRIES.clear();
        version++;
    }

    public static synchronized int version() {
        return version;
    }
}
