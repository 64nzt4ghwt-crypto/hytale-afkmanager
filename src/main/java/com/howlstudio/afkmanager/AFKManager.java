package com.howlstudio.afkmanager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.*;
import java.util.concurrent.*;

public class AFKManager {
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private final Set<UUID> afkSet = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private static final long THRESHOLD = 5 * 60_000L;
    private ScheduledExecutorService scheduler;

    public void onJoin(UUID uid, String name) {
        lastActivity.put(uid, System.currentTimeMillis());
        names.put(uid, name);
        afkSet.remove(uid);
    }

    public void onLeave(UUID uid) {
        lastActivity.remove(uid);
        afkSet.remove(uid);
        names.remove(uid);
    }

    public void markActive(UUID uid) {
        lastActivity.put(uid, System.currentTimeMillis());
        if (afkSet.remove(uid)) {
            broadcast("[AFK] " + names.getOrDefault(uid, "?") + " is no longer AFK.");
        }
    }

    public void setAFK(UUID uid) {
        if (afkSet.add(uid)) {
            String n = names.getOrDefault(uid, "?");
            broadcast("[AFK] " + n + " is now AFK.");
            PlayerRef ref = Universe.get().getPlayer(uid);
            if (ref != null) ref.sendMessage(Message.raw("[AFK] You are AFK. Type /afk to return."));
        }
    }

    public void toggleAFK(UUID uid) {
        if (afkSet.contains(uid)) markActive(uid); else setAFK(uid);
    }

    public boolean isAFK(UUID uid) { return afkSet.contains(uid); }
    public Set<UUID> getAFKSet() { return Collections.unmodifiableSet(afkSet); }
    public Map<UUID, Long> getLastActivity() { return lastActivity; }
    public Map<UUID, String> getNames() { return names; }

    public void startChecker() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Long> e : lastActivity.entrySet()) {
                if (!afkSet.contains(e.getKey()) && (now - e.getValue()) > THRESHOLD) {
                    setAFK(e.getKey());
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    public void stopChecker() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void broadcast(String msg) {
        try {
            for (PlayerRef p : Universe.get().getPlayers()) p.sendMessage(Message.raw(msg));
        } catch (Exception ex) { /* server may be shutting down */ }
        System.out.println(msg);
    }

    public AbstractPlayerCommand getAFKCommand() {
        return new AbstractPlayerCommand("afk", "Toggle AFK. Usage: /afk") {
            @Override
            protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
                toggleAFK(playerRef.getUuid());
            }
        };
    }

    public AbstractPlayerCommand getAFKListCommand() {
        return new AbstractPlayerCommand("afklist", "List AFK players. Usage: /afklist") {
            @Override
            protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
                if (afkSet.isEmpty()) { playerRef.sendMessage(Message.raw("[AFK] No players are AFK.")); return; }
                playerRef.sendMessage(Message.raw("[AFK] AFK (" + afkSet.size() + "):"));
                for (UUID uid : afkSet) {
                    long idle = (System.currentTimeMillis() - getLastActivity().getOrDefault(uid, 0L)) / 60_000;
                    playerRef.sendMessage(Message.raw("  - " + names.getOrDefault(uid, "?") + " (" + idle + "m)"));
                }
            }
        };
    }
}
